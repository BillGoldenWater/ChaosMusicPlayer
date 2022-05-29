package indi.goldenwater.chaosmusicplayer

import org.bukkit.plugin.java.JavaPlugin
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.ByteBuffer
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

class ChaosMusicPlayer : JavaPlugin() {
    override fun onEnable() {

    }

    override fun onDisable() {

    }
}

fun sineWave(frequency: Int, seconds: Int, sampleRate: Int): ByteArray {
    val samples = seconds * sampleRate
    val result = ByteBuffer.allocate(samples * Short.SIZE_BYTES)
    val interval = sampleRate.toDouble() / frequency
    for (i in 0 until samples) {
        val angle = 2.0 * PI * i / interval
        result.putShort((sin(angle) * Short.MAX_VALUE).roundToInt().toShort())
    }
    return result.array()
}

fun generateSineWaveFile(fileName: String, frequency: Int, seconds: Int = 1, sampleRate: Int = 44000) {
    val buffer = sineWave(frequency, seconds, sampleRate)
    val format = AudioFormat(sampleRate.toFloat(), Short.SIZE_BITS, 1, true, true)

    AudioSystem.write(
        AudioInputStream(ByteArrayInputStream(buffer), format, buffer.size.toLong() / Short.SIZE_BYTES),
        AudioFileFormat.Type.WAVE,
        File(fileName)
    )
}

fun main() {
    generateSineWaveFile(fileName = "test.wav", frequency = 420, 1)
}