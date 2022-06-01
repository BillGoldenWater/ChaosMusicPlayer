package indi.goldenwater.chaosmusicplayer.type

import indi.goldenwater.chaosmusicplayer.music.MusicManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.File
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.declaredMemberProperties

@Serializable
data class MusicInfo(
    val musicFileName: String,
    @Transient
    val musicFile: File = File(MusicManager.musicFolder, musicFileName),
    var displayName: String = musicFile.nameWithoutExtension,

    var preload: Boolean = true,
    var ticksPerSecond: Int = 20,
    var maxSoundNumber: Int = 247,
    var minimumVolume: Double = 0.0,
    var removeLowVolumeValueInPercent: Double = 0.005,
) {

    companion object {
        fun parseMusicFileName(string: String): String {
            return string.replace(":", " ")
        }

        fun removeFileNameSpaces(string: String): String {
            return string.replace(" ", ":")
        }

        fun getAttrs(): MutableList<KMutableProperty<*>> {
            val attrs: MutableList<KMutableProperty<*>> = MusicInfo::class.declaredMemberProperties
                .filterNot {
                    it.annotations.any { annotation ->
                        annotation.annotationClass == Transient::class
                    }
                }
                .mapNotNull { if (it is KMutableProperty<*>) it else null }
                .toMutableList()
            return attrs
        }
    }
}
