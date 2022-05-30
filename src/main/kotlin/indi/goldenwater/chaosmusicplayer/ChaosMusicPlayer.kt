package indi.goldenwater.chaosmusicplayer

import indi.goldenwater.chaosmusicplayer.utils.generateResourcePack
import org.bukkit.plugin.java.JavaPlugin

@Suppress("unused")
class ChaosMusicPlayer : JavaPlugin() {
    override fun onEnable() {
        saveDefaultConfig()
        reloadConfig()

        logger.info("Enabled")
    }

    override fun onDisable() {
        saveConfig()

        logger.info("Disabled")
    }
}

fun main() {
    generateResourcePack()
}