package indi.goldenwater.chaosmusicplayer.command

import indi.goldenwater.chaosmusicplayer.ChaosMusicPlayer
import indi.goldenwater.chaosmusicplayer.music.MusicManager
import indi.goldenwater.chaosmusicplayer.utils.append
import indi.goldenwater.chaosmusicplayer.utils.toComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object CommandCMP : CommandExecutor {
    const val commandName = "chaosmusicplayer"

    private const val help = "help"
    private val helpUsage = "/$commandName $help [指令] 查看用法".toComponent()
    private val helpUsageDetail = """
        |/$commandName $help [指令]
        |   显示指令的详细用法
    """.trimMargin().toComponent()

    private const val play = "play"

    private const val pause = "pause"

    private const val resume = "resume"

    private const val stop = "stop"

    init {
        helpUsage.clickEvent(ClickEvent.runCommand("/$commandName $help"))
        helpUsageDetail.clickEvent(ClickEvent.suggestCommand("/$commandName $help "))
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val listArgs = mutableListOf<String>()
        listArgs.addAll(args)
        return this.onCommand(sender, command, listArgs)
    }

    private fun onCommand(sender: CommandSender, command: Command, args: MutableList<String>): Boolean {
        if (args.isEmpty()) {
            onHelp(sender, command)
        } else {
            when (args.removeFirst()) {
                help -> onHelp(sender, command, args.removeFirstOrNull())
                play -> onPlay(sender, args)
                pause -> onPause(sender)
                resume -> onResume(sender)
                stop -> onStop(sender)
                else -> onUnknownUsage(sender)
            }
        }
        return true
    }

    private fun onUnknownUsage(sender: CommandSender) {
        val message = Component.text("未知的用法, 使用 /$commandName $help 查看")
        sender.sendMessage(message)
    }

    private fun onOnlyPlayer(sender: CommandSender) {
        sender.sendMessage("这个指令只能由玩家使用")
    }

    private fun onHelp(sender: CommandSender, command: Command, targetCommand: String? = null) {
        val message = Component.text()
        message.append("${ChaosMusicPlayer.instance.name} By.Golden_Water\n")

        if (targetCommand == null) {
            message.append("指令别名: ${command.aliases.joinToString(separator = " ")}\n")
            message.append(helpUsage)
        } else {
            val msg = when (targetCommand) {
                help -> helpUsageDetail

                else -> helpUsageDetail
            }
            message.append(msg)
        }

        sender.sendMessage(message)
    }

    private fun onPlay(sender: CommandSender, args: MutableList<String>) {
        if (args.isEmpty()) {
            onUnknownUsage(sender)
            return
        }
        if (sender !is Player) {
            onOnlyPlayer(sender)
            return
        }
        val musicFileName = args.joinToString(separator = " ")

        val musicInfo = MusicManager.getMusics().find { it.musicFileName == musicFileName }

        if (musicInfo == null) {
            sender.sendMessage("未知的音乐文件")
        } else {
            MusicManager.play(sender, musicInfo)
        }
    }

    private fun onPause(sender: CommandSender) {
        if (sender !is Player) {
            onOnlyPlayer(sender)
            return
        }
        MusicManager.pause(sender)
    }

    private fun onResume(sender: CommandSender) {
        if (sender !is Player) {
            onOnlyPlayer(sender)
            return
        }
        MusicManager.resume(sender)
    }

    private fun onStop(sender: CommandSender) {
        if (sender !is Player) {
            onOnlyPlayer(sender)
            return
        }
        MusicManager.stop(sender)
    }
}