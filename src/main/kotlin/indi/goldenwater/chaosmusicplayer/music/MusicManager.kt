package indi.goldenwater.chaosmusicplayer.music

import indi.goldenwater.chaosmusicplayer.ChaosMusicPlayer
import indi.goldenwater.chaosmusicplayer.type.MusicInfo
import org.bukkit.entity.Player
import java.io.File
import java.io.FileFilter

object MusicManager {
    var musicFolder: File = File("musics")

    private val musicPlaying: MutableMap<Player, MusicPlayer> = mutableMapOf()
    private val pendingRequests: MutableMap<Player, RequestInfo> = mutableMapOf()

    fun getMusics(): MutableList<MusicInfo> {
        val musicFiles = musicFolder.listFiles(FileFilter { it.extension == "wav" })?.toMutableList()
        val musicInfos = ChaosMusicPlayer.instance.getMusicInfos()

        musicFiles?.removeIf { file ->
            musicInfos.find { it.musicFile.canonicalPath == file.canonicalPath } != null
        }

        musicFiles?.forEach {
            musicInfos.add(MusicInfo(it.relativeTo(musicFolder).path))
        }

        return musicInfos
    }

    fun play(player: Player, musicInfo: MusicInfo) {
        val previousPlaying = musicPlaying[player]
        val playing = MusicPlayer(
            musicInfo,
            player,
        )

        if (previousPlaying != null) {
            previousPlaying.stop()
            playing.listenTogether.addAll(previousPlaying.listenTogether)
            musicPlaying.remove(player)
        }

        musicPlaying[player] = playing
        playing.runTaskAsynchronously(ChaosMusicPlayer.instance)
    }

    fun pause(player: Player) {
        val playing = musicPlaying[player]
        if (playing == null) {
            player.sendMessage("暂停播放失败, 未开始播放或你不是主持")
        } else {
            playing.pause()
        }
    }

    fun resume(player: Player) {
        val playing = musicPlaying[player]
        if (playing == null) {
            player.sendMessage("无法继续播放, 未开始播放或你不是主持")
        } else {
            playing.resume()
        }
    }

    fun stop(player: Player) {
        val playing = musicPlaying[player]
        if (playing == null) {
            player.sendMessage("无法停止播放, 未开始播放或你不是主持")
        } else {
            playing.stop()
            musicPlaying.remove(player)
        }
    }

    fun join(player: Player, targetPlayer: Player) {

    }

    fun invite(player: Player, targetPlayer: Player) {

    }

    fun accept(player: Player) {

    }

    fun quit(player: Player) {

    }

    fun stopAll() {
        musicPlaying.forEach { it.value.stop() }
        musicPlaying.clear()
    }

    fun updateMusicFolder(folder: File) {
        musicFolder = folder
    }

    data class RequestInfo(
        val target: Player,
        val type: RequestType,
        val timestamp: Long = System.currentTimeMillis()
    )

    enum class RequestType {
        Invite,
        Join,
    }
}