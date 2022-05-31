package indi.goldenwater.chaosmusicplayer.type

import indi.goldenwater.chaosmusicplayer.music.MusicManager
import kotlinx.serialization.Transient
import java.io.File

@kotlinx.serialization.Serializable
data class MusicInfo(
    val musicFileName: String,
    @Transient
    val musicFile: File = File(MusicManager.musicFolder, musicFileName),
    val displayName: String = musicFile.nameWithoutExtension,

    val preload: Boolean = true,
    val ticksPerSecond: Int = 20,
    val maxSoundNumber: Int = 247,
    val minimumVolume: Double = 0.001,
    val removeLowVolumeValueInPercent: Double = 0.005,
)
