/*
 * Copyright 2021-2022 Golden_Water
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package indi.goldenwater.chaosmusicplayer.music

import indi.goldenwater.chaosmusicplayer.ChaosMusicPlayer
import indi.goldenwater.chaosmusicplayer.common.DstItem
import indi.goldenwater.chaosmusicplayer.common.MusicData
import indi.goldenwater.chaosmusicplayer.common.utils.split
import indi.goldenwater.chaosmusicplayer.type.MCSoundEventItem
import indi.goldenwater.chaosmusicplayer.type.MusicInfo
import indi.goldenwater.chaosmusicplayer.utils.getFrequencySoundInfo
import indi.goldenwater.chaosmusicplayer.utils.stopAllSounds
import net.kyori.text.TextComponent
import net.kyori.text.adapter.bukkit.TextAdapter
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.jtransforms.dst.DoubleDST_1D
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.DoubleBuffer
import java.util.logging.Logger
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis

@Suppress("unused", "MemberVisibilityCanBePrivate")
class MusicPlayer(
  var musicInfo: MusicInfo,
  val hostPlayer: Player,
  private val showProgressBar: Boolean = true,
  private val progressBarLength: Int = 50,
) : BukkitRunnable() {
  private val logger: Logger = ChaosMusicPlayer.instance.logger

  val listenTogether: MutableSet<Player> = mutableSetOf()
  private val targetPlayers: MutableSet<Player> = mutableSetOf()

  //region musicInfo
  private val musicFile: File
    get() = musicInfo.musicFile
  private val preload: Boolean
    get() = musicInfo.preload
  private val ticksPerSecond: Int
    get() = musicInfo.ticksPerSecond
  private val maxSoundNumber: Int
    get() = musicInfo.maxSoundNumber
  private val minimumVolume: Double
    get() = musicInfo.minimumVolume
  private val removeLowVolumeValueInPercent: Double
    get() = musicInfo.removeLowVolumeValueInPercent
  //endregion

  private var audioInputStream: AudioInputStream =
    AudioSystem.getAudioInputStream(musicFile)

  //region format
  private val format: AudioFormat = audioInputStream.format
  private val channelSize = format.channels
  private val sampleRate = format.sampleRate
  private val sampleSize = format.sampleSizeInBits
  private val frameSize = format.frameSize
  private val isBigEndian = format.isBigEndian
  private val encoding = format.encoding
  //endregion

  /**
   * in second(s)
   */
  private val totalLength: Int = (audioInputStream.frameLength / sampleRate).roundToInt()

  //region utilities
  private var samplePerTickDiff = 0.0
  private val samplePerTick: Int
    get() {
      val perTickDouble = sampleRate * (1.0 / ticksPerSecond)
      val perTickSplit = perTickDouble.split()
      samplePerTickDiff += perTickSplit.second

      val diffSplit = samplePerTickDiff.split()
      val perTick = if (diffSplit.first >= 1) {
        samplePerTickDiff -= diffSplit.first
        perTickSplit.first + diffSplit.first
      } else {
        perTickSplit.first
      }

      return perTick
    }
  private val readAFrame: (ByteBuffer) -> Double = { buffer: ByteBuffer ->
    var sum = 0.0
    for (i in 0 until channelSize) {
      sum += when (encoding) {
        AudioFormat.Encoding.PCM_SIGNED -> when (sampleSize) {
          8 -> buffer.get() / Byte.MAX_VALUE.toDouble()
          16 -> buffer.short / Short.MAX_VALUE.toDouble()
          32 -> buffer.int / Int.MAX_VALUE.toDouble()
          64 -> buffer.long / Long.MAX_VALUE.toDouble()
          else -> throw IllegalArgumentException("Unsupported sample size $sampleSize")
        }

        AudioFormat.Encoding.PCM_UNSIGNED -> when (sampleSize) {
          8 -> buffer.get().toUByte().toDouble() / Byte.MAX_VALUE - 1.0
          16 -> buffer.short.toUShort().toDouble() / Short.MAX_VALUE - 1.0
          32 -> buffer.int.toUInt().toDouble() / Int.MAX_VALUE - 1.0
          64 -> buffer.long.toULong().toDouble() / Long.MAX_VALUE - 1.0
          else -> throw IllegalArgumentException("Unsupported sample size $sampleSize")
        }

        else -> throw IllegalArgumentException("Unsupported encoding $encoding")
      }
    }
    sum / channelSize
  }
  //endregion

  //region cache
  private val audioBuffer: ByteBuffer = ByteBuffer.allocate(
    if (preload) {
      audioInputStream.frameLength.toInt() * frameSize
    } else 0
  )

  private val dSTs: MutableMap<Int, DoubleDST_1D> = mutableMapOf()
  private val tickBufferArrays: MutableMap<Int, ByteArray> = mutableMapOf()

  private var musicDataPerTick: MusicData? = null
  //endregion

  private var playing = true
  private var running = true

  private fun readATick(maxBytes: Int): ByteBuffer {
    val result = tickBufferArrays.getOrPut(maxBytes) { ByteArray(maxBytes) }

    if (preload) audioBuffer.get(result)
    else audioInputStream.read(result)

    return ByteBuffer.wrap(result).order(if (isBigEndian) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN)
  }

  override fun run() {
    if (preload) audioInputStream.read(audioBuffer.array())
    tickWhenWaiting()
    var lastTime = 1000L / ticksPerSecond
    var offset = 0.0
    while (running) {
      val loopTime = 1000L / ticksPerSecond
      offset += (loopTime - lastTime) / 4.0

      lastTime = measureTimeMillis {
        val costMillis = measureTimeMillis {
          tick()
          if (!running) return@run
          tickWhenWaiting()
        }

        val delay = (loopTime - costMillis + offset.roundToInt()).coerceAtLeast(0)
        Thread.sleep(delay)
      }
    }
  }

  private fun tick() {
    updateTargetPlayers()
    if (!running) return
    updateProgressBar()

    if (!playing) return
    playToPlayers()
  }

  private fun tickWhenWaiting() {
    if (!playing) return

    //region readData
    val packetSize = (if (preload) audioBuffer.remaining() else audioInputStream.available())
      .coerceAtMost(samplePerTick * frameSize)
    if (packetSize == 0) {
      this.stop()
      return
    }
    val data = readATick(packetSize)
    //endregion

    //region mergeChannel
    val frameNumInDouble = data.capacity() * 1.0 / frameSize
    if (frameNumInDouble % 1.0 != 0.0) {
      throw RuntimeException("Unexpected $frameNumInDouble frame per tick, it should be a integer.")
    }
    val frameNum = frameNumInDouble.roundToInt()
    val packet = DoubleBuffer.allocate(frameNum)

    while (data.hasRemaining()) {
      packet.put(readAFrame(data))
    }

    val packetArr = packet.array()
    //endregion

    //region generateSoundsNeedPlay
    val dstValueNormalized = { value: Double -> (value / (packetArr.size / 2)).toFloat() }

    val dst = dSTs.getOrPut(frameNum) { DoubleDST_1D(frameNum.toLong()) }
    dst.forward(packetArr, false)

    var minimumVolume = Float.MAX_VALUE
    var maximumVolume = 0.0f
    val dSTOutputSounds: MutableList<DstItem> = mutableListOf()
    for (i in packetArr.indices) {
      val normalized = dstValueNormalized(packetArr[i])

      val volume = abs(normalized)

      if (volume < this.minimumVolume || volume == 0.0f) continue

      if (volume < minimumVolume) minimumVolume = volume
      else if (volume > maximumVolume) maximumVolume = volume

      dSTOutputSounds.add(DstItem(index = i, valueNormalized = normalized))
    }
    val uVolumeRange = maximumVolume - minimumVolume

    dSTOutputSounds.removeIf { abs(it.valueNormalized) < minimumVolume + (uVolumeRange * removeLowVolumeValueInPercent) }

    dSTOutputSounds.sortByDescending { abs(it.valueNormalized) }
    //endregion

    //region convert to MusicData
    val items = dSTOutputSounds
      .take(MusicData.AvailableItemsNumber)
    musicDataPerTick =
      MusicData(
        items = items,
        dstLen = packetArr.size,
        ticksPerSecond = sampleRate.toDouble() / samplePerTick,
        sampleRate = sampleRate,
        sampleNum = frameNum
      )
    //endregion
  }

  private fun updateTargetPlayers() {
    targetPlayers.clear()

    if (!hostPlayer.isOnline) {
      stop()
      logger.warning("Auto stopped, because the host player ${hostPlayer.name} is offline.")
      return
    }
    targetPlayers.add(hostPlayer)

    listenTogether.removeIf { !it.isOnline }
    targetPlayers.addAll(listenTogether)
  }

  private fun updateProgressBar() {
    val finished = (getPlayedPercent() * progressBarLength).toInt()
    val unfinished = progressBarLength - finished

    val playedLength = (getPlayedPercent() * totalLength).roundToInt()

    val progressBar = TextComponent.builder()

    progressBar.append("[${"=".repeat(finished)}${"-".repeat(unfinished)}] ")
    progressBar.append("${playedLength / 60}:${playedLength % 60}/${totalLength / 60}:${totalLength % 60}")

    targetPlayers.forEach { TextAdapter.sendActionBar(it, progressBar.build()) }
  }

  private fun playToPlayers() {
    val grouped = targetPlayers.groupBy { DirectEnabledPlayers.isEnabled(it.uniqueId) }
    val vanilla = grouped.getOrDefault(false, listOf())
    val direct = grouped.getOrDefault(true, listOf())

    if (vanilla.isNotEmpty() && musicDataPerTick != null) {
      val musicDataPerTick = musicDataPerTick!!
      val sounds = musicDataPerTick.items.take(maxSoundNumber).mapNotNull { item ->
        getFrequencySoundInfo(frequency = musicDataPerTick.convToFrequency(item.index))?.let {
          MCSoundEventItem(eventName = it.eventName, volume = abs(item.valueNormalized), pitch = it.pitch.toFloat())
        }
      }

      for (player in vanilla) {
        stopAllSounds(player)

        sounds.forEach {
          player.playSound(player.location, it.eventName, SoundCategory.RECORDS, it.volume, it.pitch)
        }
      }
    }

    if (direct.isNotEmpty() && musicDataPerTick != null) {
      val musicDataPerTick = musicDataPerTick!!
      val encoded = musicDataPerTick.encode()

      for (player in direct) {
        player.sendPluginMessage(ChaosMusicPlayer.instance, "chaosmusicplayer:music_data", encoded)
      }
    }
  }

  private fun stopAllPlaying() {
    targetPlayers.forEach { stopAllSounds(it) }
  }

  fun getPlayedPercent(): Double =
    if (preload)
      audioBuffer.position() / (audioBuffer.capacity() + 0.0)
    else 0.0

  fun resume() {
    playing = true
  }

  fun pause() {
    playing = false
    stopAllPlaying()
  }

  fun stop() {
    running = false
    stopAllPlaying()
  }

  fun reset() {
    if (preload) audioBuffer.clear()
    else audioInputStream = AudioSystem.getAudioInputStream(musicFile)
  }
}