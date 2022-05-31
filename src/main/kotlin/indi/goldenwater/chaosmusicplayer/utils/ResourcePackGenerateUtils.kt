package indi.goldenwater.chaosmusicplayer.utils

import indi.goldenwater.chaosmusicplayer.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.ByteBuffer
import java.util.zip.ZipOutputStream
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.math.sin

const val packetName = "ChaosMusicPlayer"
const val packetNamespace = "minecraft"

const val maxFrequencyNeedProvide = 20000.0

//region sine wave audio files generate
fun sineWave(frequency: Double, seconds: Double, sampleRate: Int): ByteArray {
    val samples = (seconds * sampleRate).toInt()
    val result = ByteBuffer.allocate(samples * Long.SIZE_BYTES)

    val interval = sampleRate.toDouble() / frequency

    for (i in 1..samples) {
        val radians = Math.toRadians((i / interval) * 360)
        val value = sin(radians) * Long.MAX_VALUE
        result.putLong(
            value
                .roundToLong()
        )
    }
    return result.array()
}

fun generateSineWaveFile(file: File, frequency: Double, seconds: Double = 1.0, sampleRate: Int = 44000) {
    val buffer = sineWave(frequency, seconds, sampleRate)
    val format = AudioFormat(sampleRate.toFloat(), Long.SIZE_BITS, 1, true, true)

    AudioSystem.write(
        AudioInputStream(ByteArrayInputStream(buffer), format, buffer.size.toLong() / Long.SIZE_BYTES),
        AudioFileFormat.Type.WAVE,
        file
    )
}

fun getFrequenciesNeedGen(maxFrequency: Double = maxFrequencyNeedProvide): MutableList<Double> {
    val frequencies = mutableListOf<Double>()

    val startFrequency = 0.5

    var generateFrequency = startFrequency * 2
    var nextStartFrequency = generateFrequency * 2

    while (nextStartFrequency < maxFrequency) {
        frequencies.add(generateFrequency)
        frequencies.add(generateFrequency * -1)

        generateFrequency = nextStartFrequency * 2
        nextStartFrequency = generateFrequency * 2
    }
    frequencies.add(generateFrequency)
    frequencies.add(generateFrequency * -1)

    return frequencies
}

fun getSineWaveFileName(frequency: Double, ext: String = ".wav"): String {
    val frequencyName = frequency
        .toString()
        .replace(".", "_")
        .replace("-", "n")
    return "$frequencyName$ext"
}

fun generateSineWaveFiles(outputDir: File, maxFrequency: Double = maxFrequencyNeedProvide) {
    val frequencies = getFrequenciesNeedGen(maxFrequency)

    val generate = { frequency: Double ->
        println("generating ${getSineWaveFileName(frequency)}")
        generateSineWaveFile(File(outputDir, getSineWaveFileName(frequency)), frequency, 2.0, 96_000)
    }

    frequencies.forEach(generate)
}

/**
 * need ffmpeg install
 */
fun transCodeToOgg(dir: File) {
    dir
        .listFiles()
        ?.forEach { sourceFile ->
            val targetFile = File(sourceFile.parentFile, "${sourceFile.nameWithoutExtension}.ogg")
            if (sourceFile.extension == "ogg") return@forEach

            val ffmpegProcessBuilder = ProcessBuilder(
                "ffmpeg", "-y", "-i", sourceFile.canonicalPath, "-qscale:a", "5", targetFile.canonicalPath
            )
            println("trans code ${sourceFile.name} to ${targetFile.name}")
            ffmpegProcessBuilder
                .start()
                .waitFor()
            sourceFile.delete()
        }
}
//endregion

//region generate resource pack
@Serializable
data class PackMcmetaPack(
    val pack_format: Int = 8,
    val description: String = ""
)

@Serializable
data class PackMcmeta(
    val pack: PackMcmetaPack
)

fun writePackMcmetaFile(file: File) {
    val packMcmetaData = PackMcmeta(pack = PackMcmetaPack(description = "播放音乐所需的正弦波音频文件"))
    file.writeText(json.encodeToString(packMcmetaData))
}

fun getSineWaveSoundEventNameWithoutNameSpace(frequency: Double): String =
    "sine_wave.${getSineWaveFileName(frequency, ext = "")}"

fun getSineWaveSoundEventName(frequency: Double): String =
    "${packetNamespace}:${getSineWaveSoundEventNameWithoutNameSpace(frequency)}"

data class SoundInfo(
    val eventName: String,
    val pitch: Double,
)

fun getFrequencySoundInfo(frequency: Double): SoundInfo? {
    val negativeMultiplier = if (frequency < 0) -1.0 else 1.0
    val targetFrequency = abs(frequency)

    val frequencies = getFrequenciesNeedGen()
    frequencies.removeIf { it < 0 }
    if (targetFrequency < frequencies.first() * 0.5 || targetFrequency > frequencies.last() * 2.0)
        return null

    val originFrequency = frequencies.find {
        val pitch = targetFrequency / it
        pitch in 0.5..2.0
    } ?: return null

    return SoundInfo(
        getSineWaveSoundEventName(originFrequency * negativeMultiplier),
        targetFrequency / originFrequency
    )
}

@Serializable
data class SoundsJsonSoundItem(
    val name: String,
    val preload: Boolean = true
)

@Serializable
data class SoundsJsonSoundObject(
    val sounds: MutableList<SoundsJsonSoundItem> = mutableListOf()
)

fun writeSoundsJson(file: File) {
    val soundsJson = mutableMapOf<String, SoundsJsonSoundObject>()

    val frequencies = getFrequenciesNeedGen()

    frequencies.forEach { frequency ->
        val soundObject = SoundsJsonSoundObject()
        soundObject.sounds.add(SoundsJsonSoundItem(name = getSineWaveFileName(frequency, "")))
        soundsJson[getSineWaveSoundEventNameWithoutNameSpace(frequency)] = soundObject
    }

    file.writeText(json.encodeToString(soundsJson))
}

fun copySineWaveFiles(sourceDir: File, targetDir: File) {
    sourceDir
        .listFiles()
        ?.forEach {
            it.copyTo(File(targetDir, it.name))
        }
}

fun createPack(dir: File, sineWaveSourceDir: File) {
    val packMcmeta = File(dir, "pack.mcmeta")
    writePackMcmetaFile(packMcmeta)

    val assets = File(dir, "assets")
    assets.mkdir()
    val namespace = File(assets, packetNamespace)
    namespace.mkdir()

    val soundsJson = File(namespace, "sounds.json")
    writeSoundsJson(soundsJson)
    val sounds = File(namespace, "sounds")
    sounds.mkdir()

    copySineWaveFiles(sineWaveSourceDir, sounds)
}

fun packToOutput(sourceDir: File, outputFile: File) {
    val fos = outputFile.outputStream()
    val zos = ZipOutputStream(fos)

    zos.writeDirectory(sourceDir, sourceDir)

    zos.close()
    fos.close()
}
//endregion

fun generateResourcePack() {
    val workspace = File("generateResourcePack")
    workspace.deleteRecursively()
    workspace.mkdir()

    //region generateSource
    val source = File(workspace, "source")
    source.mkdir()

    val sineWaves = File(source, "sineWaveFiles")
    sineWaves.mkdir()
    val packSource = File(source, "packSource")
    packSource.mkdir()

    generateSineWaveFiles(sineWaves)
    transCodeToOgg(sineWaves)

    createPack(packSource, sineWaves)
    //endregion

    //region packToOutput
    val output = File(workspace, "output")
    output.mkdir()
    val resourcePackOutput = File(output, "${packetName}.zip")
    packToOutput(packSource, resourcePackOutput)
    //endregion
}