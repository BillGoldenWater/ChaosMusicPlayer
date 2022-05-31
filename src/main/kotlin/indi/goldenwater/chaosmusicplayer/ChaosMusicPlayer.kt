package indi.goldenwater.chaosmusicplayer

import indi.goldenwater.chaosmusicplayer.music.MusicPlayer
import indi.goldenwater.chaosmusicplayer.utils.generateResourcePack
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

@Suppress("unused")
class ChaosMusicPlayer : JavaPlugin() {
    private val musicFolder: File = File(dataFolder, "musics")
    lateinit var musicPlayer: MusicPlayer

    init {
        instance = this
    }

    override fun onEnable() {
        saveDefaultConfig()
        reloadConfig()

        if (!musicFolder.exists())
            musicFolder.mkdirs()

        Bukkit.getPlayer("Golden_Water")?.let {
            musicPlayer = MusicPlayer(
                hostPlayer = it,
                isBroadcast = true,
                broadcastRange = 10.0,

                musicFile = File(musicFolder, "朧月 - 初音ミク,湊貴大.wav"),
                ticksPerSecond = 20,
                minimumVolume = 0.0,
                removeLowVolumeValueInPercent = 0.0
            )
            musicPlayer.runTaskAsynchronously(this)
            Bukkit.getPlayer("WanAna_kai")?.let { p ->
                musicPlayer.listenTogether.add(p)
            }
        }

        logger.info("Enabled")
    }

    override fun onDisable() {
        saveConfig()

        musicPlayer.stop()

        logger.info("Disabled")
    }

    companion object {
        lateinit var instance: ChaosMusicPlayer
    }
}

fun main() {
    generateResourcePack()
}
