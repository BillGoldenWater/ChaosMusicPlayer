package indi.goldenwater.chaosmusicplayer

import indi.goldenwater.chaosmusicplayer.command.CommandCMP
import indi.goldenwater.chaosmusicplayer.command.TabCMP
import indi.goldenwater.chaosmusicplayer.music.MusicManager
import indi.goldenwater.chaosmusicplayer.type.MusicInfo
import indi.goldenwater.chaosmusicplayer.utils.generateResourcePack
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

val json = Json {
    encodeDefaults = true
    prettyPrint = true
}

@Suppress("unused")
class ChaosMusicPlayer : JavaPlugin() {
    private val musicDataFile: File = File(dataFolder, "musicInfos.json")
    private var musicDatas: List<MusicInfo> = mutableListOf()

    init {
        instance = this
    }

    override fun onEnable() {
        //region init datas
        saveDefaultConfig()
        reloadConfig()

        val musicFolder = File(dataFolder, "musics")
        if (!musicFolder.exists())
            musicFolder.mkdirs()

        MusicManager.updateMusicFolder(musicFolder)
        loadMusicInfos()
        //endregion

        getCommand(CommandCMP.commandName)?.let {
            it.setExecutor(CommandCMP)
            it.tabCompleter = TabCMP
        }

        logger.info("Enabled")
    }

    override fun onDisable() {
        saveConfig()
        saveMusicInfos()

        MusicManager.stopAll()

        logger.info("Disabled")
    }

    fun getMusicInfos(): MutableList<MusicInfo> {
        return musicDatas.toMutableList()
    }

    fun setMusicInfos(musicInfos: MutableList<MusicInfo>) {
        musicDatas = musicInfos
            .distinctBy { it.musicFileName }
            .filter { it.musicFile.exists() }
            .sortedBy { it.musicFileName }
    }

    private fun loadMusicInfos() {
        if (!musicDataFile.exists()) {
            musicDatas = mutableListOf()
            return
        }
        if (!musicDataFile.isFile) {
            logger.warning("The musicInfos.json is not a file, unable to read it.")
            musicDatas = mutableListOf()
            return
        }
        val inputStream = musicDataFile.inputStream()
        try {
            inputStream.use { fis ->
                val musicInfos = json
                    .decodeFromString<List<MusicInfo>>(fis.reader().readText())
                musicDatas = musicInfos
                    .distinctBy { it.musicFileName }
                    .filter { it.musicFile.exists() }
                return
            }
        } catch (e: Exception) {
            logger.warning("Unable to parse musicInfos.json")
            e.printStackTrace()
            musicDatas = mutableListOf()
            return
        }
    }

    private fun saveMusicInfos() {
        if (musicDataFile.exists() && !musicDataFile.isFile) {
            logger.warning("The musicInfos.json is not a file, all info will not be saved.")
            return
        }

        try {
            val jsonStr = json.encodeToString(musicDatas)
            musicDataFile.outputStream().use { it.write(jsonStr.encodeToByteArray()) }
        } catch (e: Exception) {
            logger.warning("Unable to write musicInfos.json")
            e.printStackTrace()
        }
    }

    companion object {
        lateinit var instance: ChaosMusicPlayer
    }
}

fun main() {
    generateResourcePack()
}
