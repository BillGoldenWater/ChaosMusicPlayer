package indi.goldenwater.chaosmusicplayer

import org.bukkit.plugin.java.JavaPlugin
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.ByteBuffer
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.math.roundToInt
import kotlin.math.sin

class ChaosMusicPlayer : JavaPlugin() {
    override fun onEnable() {

    }

    override fun onDisable() {

    }
}

fun sineWave(frequency: Double, seconds: Int, sampleRate: Int): ByteArray {
    val samples = seconds * sampleRate
    val result = ByteBuffer.allocate(samples * Short.SIZE_BYTES)

    val interval = sampleRate.toDouble() / frequency

    for (i in 0 until samples) {
        val radians = Math.toRadians((i / interval) * 360)
        val value = sin(radians) * Short.MAX_VALUE
        result.putShort(value.roundToInt().toShort())
    }
    return result.array()
}

fun generateSineWaveFile(file: File, frequency: Double, seconds: Int = 1, sampleRate: Int = 44000) {
    val buffer = sineWave(frequency, seconds, sampleRate)
    val format = AudioFormat(sampleRate.toFloat(), Short.SIZE_BITS, 1, true, true)

    AudioSystem.write(
        AudioInputStream(ByteArrayInputStream(buffer), format, buffer.size.toLong() / Short.SIZE_BYTES),
        AudioFileFormat.Type.WAVE,
        file
    )
}

fun generateSineWaveFiles(outputDir: File = File("sineWaveFiles"), maxFrequency: Double = 20000.0) {
    val startFrequency = 0.5

    var generateFrequency = startFrequency * 2
    var nextStartFrequency = generateFrequency * 2

    if (!outputDir.exists()) {
        outputDir.mkdir()
    } else if (!outputDir.isDirectory) {
        throw IllegalArgumentException("Output directory is not a directory")
    }

    while (nextStartFrequency < maxFrequency) {
        generateSineWaveFile(File(outputDir, "$generateFrequency.wav"), generateFrequency, 1)
        generateFrequency = nextStartFrequency * 2
        nextStartFrequency = generateFrequency * 2
    }
    generateSineWaveFile(File(outputDir, "$generateFrequency.wav"), generateFrequency, 1)
}

fun main() {
    generateSineWaveFiles()
}