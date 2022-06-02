package indi.goldenwater.chaosmusicplayer.command

import indi.goldenwater.chaosmusicplayer.music.MusicManager
import indi.goldenwater.chaosmusicplayer.type.MusicInfo
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

object TabCMP : TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): MutableList<String> {
        val listArgs = mutableListOf<String>()
        listArgs.addAll(args)
        return this.onTabComplete(sender, listArgs)
    }

    private fun onTabComplete(sender: CommandSender, args: MutableList<String>): MutableList<String> {
//        sender.sendMessage(args.joinToString())
//        sender.sendMessage(args.size.toString())

        val completions = mutableListOf<String>()
        val commands = CommandCMP.commandHandlers
            .filter { CommandCMP.hasPermission(sender, it) }
            .map { CommandCMP.getCommandInfo(it).command }

        if (args.size == 1) {
            completions.addAll(commands)
        } else {
            val handler = CommandCMP.getCommandHandler(args.removeFirst())
            if (handler != null) {
                val fileNames = MusicManager.getMusics()
                    .map { MusicInfo.removeFileNameSpaces(it.musicFileName) }
                val needAdd = when (CommandCMP.getCommandInfo(handler).command) {
                    CommandCMP.getCmdName(CommandCMP::onHelp) -> commands
                    CommandCMP.getCmdName(CommandCMP::onPlay) -> fileNames
                    CommandCMP.getCmdName(CommandCMP::onJoin) -> Bukkit
                        .getOnlinePlayers()
                        .filterNot { it.name == sender.name }
                        .map { it.name }
                    CommandCMP.getCmdName(CommandCMP::onInvite) -> Bukkit
                        .getOnlinePlayers()
                        .filterNot { it.name == sender.name }
                        .map { it.name }
                    CommandCMP.getCmdName(CommandCMP::onModify) -> {
                        val attrNames = MusicInfo.getAttrs().map { it.name }
                        when (args.size) {
                            1 -> fileNames + attrNames
                            2 -> if (fileNames.contains(args.first())) attrNames else mutableListOf()
                            else -> listOf()
                        }
                    }
                    CommandCMP.getCmdName(CommandCMP::onList) -> {
                        var i = 0
                        val groups = MusicManager.getMusics()
                            .groupBy { (i++) / CommandCMP.itemPerPage }

                        groups.keys.map { (it + 1).toString() }
                    }
                    CommandCMP.getCmdName(CommandCMP::onSettings) -> fileNames
                    else -> listOf()
                }
                completions.addAll(needAdd)
            }
        }

        return completions.filter { it.startsWith(args.last()) }
            .toMutableList()
    }
}