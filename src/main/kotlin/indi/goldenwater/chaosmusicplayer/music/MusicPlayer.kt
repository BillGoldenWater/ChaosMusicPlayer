package indi.goldenwater.chaosmusicplayer.music

import indi.goldenwater.chaosmusicplayer.ChaosMusicPlayer
import indi.goldenwater.chaosmusicplayer.type.MCSoundEventItem
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
import java.util.logging.Logger
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.system.measureNanoTime

@Suppress("unused", "MemberVisibilityCanBePrivate")
class MusicPlayer(
    private val musicFile: File,
    private val hostPlayer: Player,
    private val isBroadcast: Boolean = false,
    private val broadcastRange: Double = 1.0,
    private val preload: Boolean = true,
    private val ticksPerSecond: Int = 20,
    private val maxSoundNumber: Int = 247,
    private val minimumVolume: Double = 0.001,
    private val removeLowVolumeValueInPercent: Double = 0.005,
) : BukkitRunnable() {
    private val logger: Logger = ChaosMusicPlayer.instance.logger

    val listenTogether: MutableSet<Player> = mutableSetOf()
    private val targetPlayers: MutableSet<Player> = mutableSetOf()

    private var audioInputStream: AudioInputStream = AudioSystem.getAudioInputStream(musicFile)

    //region format
    private val format: AudioFormat = audioInputStream.format
    private val channelSize = format.channels
    private val sampleRate = format.sampleRate
    private val sampleSize = format.sampleSizeInBits
    private val frameSize = format.frameSize
    private val isBigEndian = format.isBigEndian
    private val encoding = format.encoding
    //endregion

    //region utilities
    private val framePerTick: Int = (sampleRate * (1.0 / ticksPerSecond)).roundToInt()
    private val frameBuffer: DoubleBuffer = DoubleBuffer.allocate(channelSize)
    private val readAFrame: (ByteBuffer) -> Unit = { buffer: ByteBuffer ->
        frameBuffer.clear()
        for (i in 0 until channelSize) {
            val value = when (encoding) {
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
            frameBuffer.put(value)
        }
    }
    //endregion

    //region cache
    private val audioBuffer: ByteBuffer = ByteBuffer.allocate(audioInputStream.frameLength.toInt() * frameSize)

    private val dSTs: MutableMap<Int, DoubleDST_1D> = mutableMapOf()
    private val tickBufferArrays: MutableMap<Int, ByteArray> = mutableMapOf()

    private val soundsNeedPlay: MutableList<MCSoundEventItem> = mutableListOf()
    //endregion

    private var playing = true
    private var running = true

    init {
        if (preload) audioInputStream.read(audioBuffer.array())
    }

    private fun readATick(maxBytes: Int): ByteBuffer {
        val result = tickBufferArrays.getOrPut(maxBytes) { ByteArray(maxBytes) }

        if (preload) audioBuffer.get(result)
        else audioInputStream.read(result)

        return ByteBuffer.wrap(result).order(if (isBigEndian) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN)
    }

    override fun run() {
        tickWhenWaiting()
        while (running) {
            val costNano = measureNanoTime {
                tick()
                if (!running) return@run
                tickWhenWaiting()
            }
            val costMillis = costNano.toDouble() / 1000 / 1000

            val delay = ((1000.0 / ticksPerSecond) - costMillis).coerceAtLeast(0.0)
            Thread.sleep(delay.roundToLong())
        }
    }

    private fun tick() {
        if (!playing) return

        updateTargetPlayers()
        if (!running) return
        playToPlayers()
    }

    private fun tickWhenWaiting() {
        if (!playing) return

        //region readData
        val packetSize = (if (preload) audioBuffer.remaining() else audioInputStream.available())
            .coerceAtMost(framePerTick * frameSize)
        if (packetSize == 0) {
            this.stop()
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
            packetMono.put(frameBuffer.array().sum())
        }

        val packetMonoArray = packetMono.array()
        //endregion

        //region generateSoundsNeedPlay
        val dstValueToVolume = { value: Double -> abs(value / (packetMonoArray.size / 2)) }

        val dst = dSTs.getOrPut(frameNum) { DoubleDST_1D(frameNum.toLong()) }
        dst.forward(packetMonoArray, false)

        var minimumUDstValue = Double.MAX_VALUE
        var maximumUDstValue = 0.0
        // Pair<Double, Int> dstValue index
        val dSTOutputSounds: MutableList<Pair<Double, Int>> = mutableListOf()
        for (i in packetMonoArray.indices) {
            val dstValue = packetMonoArray[i]

            val volume = dstValueToVolume(dstValue)
            if (volume < minimumVolume || volume == 0.0) continue

            val uDstValue = abs(dstValue)
            if (uDstValue < minimumUDstValue) minimumUDstValue = uDstValue
            else if (uDstValue > maximumUDstValue) maximumUDstValue = uDstValue

            dSTOutputSounds.add(dstValue to i)
        }
        val uDstValueRange = maximumUDstValue - minimumUDstValue

        dSTOutputSounds.removeIf { abs(it.first) < minimumUDstValue + (uDstValueRange * removeLowVolumeValueInPercent) }

        dSTOutputSounds.sortByDescending { abs(it.first) }
        //endregion

        //region convert to minecraft sound event
        soundsNeedPlay.clear()

        val soundsNeedPlayNum = dSTOutputSounds.size.coerceAtMost(maxSoundNumber)
        for (i in 0 until soundsNeedPlayNum) {
            val item = dSTOutputSounds[i]
            val dstValue = item.first

            val volume = dstValueToVolume(dstValue).toFloat()
            val frequency = ((item.second + 1) / 2.0) * (if (dstValue < 0) -1.0 else 1.0)

            getFrequencySoundInfo(frequency * ticksPerSecond)?.let { info ->
                soundsNeedPlay.add(MCSoundEventItem(info.eventName, volume, info.pitch.toFloat()))
            }
        }
        //endregion
    }

    private fun updateTargetPlayers() {
        targetPlayers.clear()

        if (!hostPlayer.isOnline) {
            stop()
            logger.warning("Auto stopped, because the host player ${hostPlayer.name} is offline.")
            return
        }
        if (!hostPlayer.isDead) targetPlayers.add(hostPlayer)

        listenTogether.removeIf { !it.isOnline }
        targetPlayers.addAll(listenTogether)

        if (isBroadcast) {
            val playingPlayers = targetPlayers.map { it }

            val onlinePlayers = Bukkit.getServer().onlinePlayers.toMutableSet()
            onlinePlayers.removeIf { targetPlayers.contains(it) }
            onlinePlayers.filterTo(targetPlayers) { op ->
                val opLoc = op.location
                playingPlayers.find { pp ->
                    val ppLoc = pp.location
                    if (opLoc.world.uid.compareTo(ppLoc.world.uid) != 0) return@find false

                    opLoc.distanceSquared(ppLoc) <= broadcastRange.pow(2.0)
                } != null
            }
        }
    }

    private fun playToPlayers() {
        targetPlayers.forEach { player ->
            player.stopAllSounds()

            soundsNeedPlay.forEach {
                player.playSound(player.location, it.eventName, SoundCategory.RECORDS, it.volume, it.pitch)
            }
        }
    }

    fun getPlayedPercent(): Double =
        if (preload)
            audioBuffer.position() / (audioBuffer.capacity() + 0.0)
        else 0.0

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
        if (preload) audioBuffer.clear()
        else audioInputStream = AudioSystem.getAudioInputStream(musicFile)
    }
}