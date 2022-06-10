/*
 * Copyright 2021-2022 Golden_Water
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package indi.goldenwater.chaosmusicplayer.type

import indi.goldenwater.chaosmusicplayer.ChaosMusicPlayer
import indi.goldenwater.chaosmusicplayer.music.MusicManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.File
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.declaredMemberProperties

@Serializable
data class MusicInfo(
    @Name("文件名")
    val musicFileName: String,
    @Transient
    val musicFile: File = File(MusicManager.musicFolder, musicFileName),
    @Name("显示名称")
    @Description("玩家会看到的名字")
    var displayName: String = musicFile.nameWithoutExtension,

    @Name("预加载")
    @Description("在播放时预先读取所有数据, 关闭后将流式读取. (true/false)")
    var preload: Boolean = true,
    @Name("TPS")
    @Description(
        "每秒计算的次数, 效果表现为:\n" +
                "  越小CPU性能需求越大, 细节越少;\n" +
                "  越大网络性能需求越大, 细节越多;\n" +
                "  推荐范围 1~30"
    )
    var ticksPerSecond: Int = if (ChaosMusicPlayer.legacyStop) 16 else 20,
    @Name("最大声音数量")
    @Description(
        "每次播放的最大声音数量, 效果表现为: \n" +
                "  越小网络性能需求越少, 细节越少;\n" +
                "  越大网络性能需求越大, 细节越多;\n" +
                "  推荐范围 20~247 (版本低于1.17时 不推荐大于150)"
    )
    var maxSoundNumber: Int = if (ChaosMusicPlayer.legacyStop) 100 else 247,
    @Name("最小音量(绝对)")
    @Description(
        "剔除音量低于指定值的音频, 效果表现为: \n" +
                "  越小网络性能需求越大, 细节越多;\n" +
                "  越大网络性能需求越少, 细节越少;\n" +
                "  推荐范围 0 ~ 0.1" +
                "  1是能播放的最大音量, 0是没有声音"
    )
    var minimumVolume: Double = 0.0,
    @Name("最小音量(相对)")
    @Description(
        "剔除音量低于指定值的音频, 效果表现为: \n" +
                "  越小网络性能需求越大, 细节越多;\n" +
                "  越大网络性能需求越少, 细节越少;\n" +
                "  推荐范围 0 ~ 0.1" +
                "  1是当前播放的最大音量, 0是当前播放的最小音量"
    )
    var removeLowVolumeValueInPercent: Double = 0.0,
) {

    companion object {
        private val mutableAttrs: List<KMutableProperty<*>> = MusicInfo::class.declaredMemberProperties
            .filterNot { it.annotations.any { annotation -> annotation is Transient } }
            .mapNotNull { if (it is KMutableProperty<*>) it else null }

        fun parseMusicFileName(string: String): String {
            return string.replace(":", " ")
        }

        fun removeFileNameSpaces(string: String): String {
            return string.replace(" ", ":")
        }

        fun getAttrs(): List<KMutableProperty<*>> {
            return mutableAttrs
        }

        fun getAttrInfo(attr: String): AttrInfo {
            val annotations = (getAttrs().find { it.name == attr } ?: return AttrInfo()).annotations
            val name = (annotations.find { it is Name } as? Name)?.name ?: ""
            val description = (annotations.find { it is Description } as? Description)?.description ?: ""
            return AttrInfo(name = name, description = description)
        }

        data class AttrInfo(val name: String = "", val description: String = "")

        @Target(AnnotationTarget.PROPERTY)
        annotation class Name(val name: String)

        @Target(AnnotationTarget.PROPERTY)
        annotation class Description(val description: String)
    }
}
