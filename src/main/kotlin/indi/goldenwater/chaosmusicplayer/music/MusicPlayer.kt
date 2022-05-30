package indi.goldenwater.chaosmusicplayer.music

import indi.goldenwater.chaosmusicplayer.type.MCSoundItem
import indi.goldenwater.chaosmusicplayer.utils.getFrequencySoundInfo
import org.bukkit.Bukkit
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.jtransforms.dst.DoubleDST_1D
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.DoubleBuffer
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis

class MusicPlayer(
    private val musicFile: File,
    private val ticksPerSecond: Int = 20,
    private val maxSoundNumber: Int = 247,
    private val minimumVolume: Double = 0.005
) : BukkitRunnable() {
    private val targetPlayers: MutableList<Player> = Bukkit.getServer().onlinePlayers.toMutableList()

    private var audioInputStream: AudioInputStream = AudioSystem.getAudioInputStream(musicFile)
    private val format: AudioFormat = audioInputStream.format
    private val channelSize = format.channels
    private val sampleRate = format.sampleRate
    private val sampleSize = format.sampleSizeInBits
    private val frameSize = format.frameSize
    private val isBigEndian = format.isBigEndian

    private val framePerTick: Int = (sampleRate * (1.0 / ticksPerSecond)).roundToInt()
    private val frameBuffer: DoubleBuffer = DoubleBuffer.allocate(channelSize)
    private val readAFrame: (ByteBuffer) -> Unit = when (sampleSize) {
        8 -> { buffer: ByteBuffer ->
            frameBuffer.clear()
            for (i in 0 until channelSize) {
                frameBuffer.put(
                    buffer
                        .get() / (Byte.MAX_VALUE * 1.0)
                )
            }
        }
        16 -> { buffer: ByteBuffer ->
            frameBuffer.clear()
            for (i in 0 until channelSize) {
                frameBuffer.put(
                    buffer.short / (Short.MAX_VALUE * 1.0)
                )
            }
        }
        32 -> { buffer: ByteBuffer ->
            frameBuffer.clear()
            for (i in 0 until channelSize) {
                frameBuffer.put(
                    buffer.int / (Int.MAX_VALUE * 1.0)
                )
            }
        }
        64 -> { buffer: ByteBuffer ->
            frameBuffer.clear()
            for (i in 0 until channelSize) {
                frameBuffer.put(
                    buffer.long / (Long.MAX_VALUE * 1.0)
                )
            }
        }
        else -> throw IllegalArgumentException("Unsupported sample size $sampleSize")
    }
    private val dSTs: MutableMap<Int, DoubleDST_1D> = mutableMapOf()
    private val tickBufferArrays: MutableMap<Int, ByteArray> = mutableMapOf()

    private var playing = true
    private var running = true

    private fun readATick(maxBytes: Int): ByteBuffer {
        val result = tickBufferArrays.getOrPut(maxBytes) { ByteArray(maxBytes) }
        audioInputStream.read(result)
        return ByteBuffer
            .wrap(result)
            .order(if (isBigEndian) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN)
    }

    override fun run() {
        while (running) {
            val cost = measureTimeMillis { tick() }
            val delay = ((1000 / ticksPerSecond) - cost).coerceAtLeast(0)
            Thread.sleep(delay)
        }
    }

    private fun tick() {
        if (!playing) return

        //region readData
        val packetSize = audioInputStream
            .available()
            .coerceAtMost(framePerTick * frameSize)
        if (packetSize == 0) {
            this.cancel()
            return
        }
        val tickFrame = readATick(packetSize)
        //endregion

        //region mergeChannel
        val frameNumInDouble = tickFrame.capacity() * 1.0 / frameSize
        if (frameNumInDouble % 1.0 != 0.0) {
            throw RuntimeException("Unexpected $frameNumInDouble frame per tick, it should be a integer.")
        }
        val frameNum = frameNumInDouble.roundToInt()
        val packetMono = DoubleBuffer.allocate(frameNum)

        while (tickFrame.hasRemaining()) {
            readAFrame(tickFrame)
            packetMono.put(
                frameBuffer
                    .array()
                    .sum()
            )
        }

        val packetMonoArray = packetMono.array()
        //endregion

        //region generateSoundsNeedPlay
        dSTs
            .getOrPut(frameNum) {
                DoubleDST_1D(frameNum.toLong())
            }
            .forward(packetMonoArray, false)

        // Pair<Double, Int> dstValue index
        val dSTOutputSounds: MutableList<Pair<Double, Int>> = mutableListOf()
        for (i in packetMonoArray.indices) {
            val dstValue = packetMonoArray[i]
            if (abs(dstValue / (packetMonoArray.size / 2)) < minimumVolume) continue

            dSTOutputSounds.add(dstValue to i)
        }

        dSTOutputSounds.removeIf { abs(it.first / (packetMonoArray.size / 2)) == 0.0 }

        dSTOutputSounds.sortByDescending { abs(it.first) }

        val soundsNeedPlayNum = dSTOutputSounds.size.coerceAtMost(maxSoundNumber)
        val soundsNeedPlay: MutableList<MCSoundItem> = MutableList(soundsNeedPlayNum) { MCSoundItem("", 0.0f, 0.0f) }
        for (i in 0 until soundsNeedPlayNum) {
            val item = dSTOutputSounds[i]
            val dstValue = item.first
            val volume = abs(dstValue / (packetMonoArray.size / 2))
            val frequency = (item.second + 1) / 2.0

            getFrequencySoundInfo((frequency * if (dstValue < 0) -1.0 else 1.0) * ticksPerSecond)?.let { info ->
                soundsNeedPlay.add(
                    MCSoundItem(
                        info.eventName,
                        volume
                            .toFloat(),
                        info.pitch
                            .toFloat()
                    )
                )
            }
        }
        //endregion

        playToPlayers(soundsNeedPlay)
    }

    private fun playToPlayers(soundsNeedPlay: MutableList<MCSoundItem>) {
        targetPlayers.forEach { player ->
            player.stopAllSounds()

            soundsNeedPlay.forEach {
                player.playSound(player.location, it.eventName, SoundCategory.RECORDS, it.volume, it.pitch)
            }
        }
    }

    fun play() {
        playing = true
    }

    fun pause() {
        playing = false
    }

    fun stop() {
        running = false
    }

    fun reset() {
        audioInputStream = AudioSystem.getAudioInputStream(musicFile)
    }
}