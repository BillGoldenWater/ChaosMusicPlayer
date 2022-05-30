package indi.goldenwater.chaosmusicplayer

import indi.goldenwater.chaosmusicplayer.music.MusicPlayer
import indi.goldenwater.chaosmusicplayer.utils.generateResourcePack
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

@Suppress("unused")
class ChaosMusicPlayer : JavaPlugin() {
    private val musicFolder: File = File(dataFolder, "musics")
    lateinit var musicPlayer: MusicPlayer

    override fun onEnable() {
        saveDefaultConfig()
        reloadConfig()

        if (!musicFolder.exists())
            musicFolder.mkdirs()

        musicPlayer = MusicPlayer(File(musicFolder, "Miracle Forest (VIP Mix) - Rinth.wav"), ticksPerSecond = 19, minimumVolume = 0.001)
        musicPlayer.runTaskAsynchronously(this)

        logger.info("Enabled")
    }

    override fun onDisable() {
        saveConfig()

        musicPlayer.stop()

        logger.info("Disabled")
    }
}

fun main() {
    generateResourcePack()
}