package indi.goldenwater.chaosmusicplayer.command

import indi.goldenwater.chaosmusicplayer.ChaosMusicPlayer
import indi.goldenwater.chaosmusicplayer.music.MusicManager
import indi.goldenwater.chaosmusicplayer.type.MusicInfo
import indi.goldenwater.chaosmusicplayer.utils.toCB
import indi.goldenwater.chaosmusicplayer.utils.toComponent
import net.kyori.text.TextComponent
import net.kyori.text.adapter.bukkit.TextAdapter
import net.kyori.text.event.ClickEvent
import net.kyori.text.event.HoverEvent
import net.kyori.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import kotlin.reflect.full.createType

object CommandCMP : CommandExecutor {
    const val commandName = "chaosmusicplayer"

    private const val help = "help"
    private val helpUsage = "/$commandName $help [指令] 查看用法".toCB()
    private val helpUsageDetail = """
        |/$commandName $help [指令]
        | 显示指令的详细用法
    """.trimMargin().toCB()

    //region operations
    private const val play = "play"

    private const val pause = "pause"

    private const val resume = "resume"

    private const val stop = "stop"

    private const val join = "join"

    private const val invite = "invite"

    const val accept = "accept"

    const val deny = "deny"

    const val cancel = "cancel"

    private const val quit = "quit"

    private const val modify = "modify"
    private const val modifyPermission = "chaosmusicplayer.modify"
    //endregion

    //region visualize
    private const val list = "list"

    private const val controls = "controls"

    private const val settings = "settings"
    private const val settingsPermission = "chaosmusicplayer.settings"

    private const val attrDetail = "attrDetail"
    //endregion

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

                play -> onPlay(sender, args.removeFirstOrNull())
                pause -> onPause(sender)
                resume -> onResume(sender)
                stop -> onStop(sender)
                join -> onJoin(sender, args.removeFirstOrNull())
                invite -> onInvite(sender, args)
                accept -> onAccept(sender)
                deny -> onDeny(sender)
                cancel -> onCancel(sender)
                quit -> onQuit(sender)
                modify -> onModify(sender, args)

                list -> onList(sender, args.removeFirstOrNull()?.toIntOrNull())
                controls -> onControls(sender)
                settings -> onSettings(sender, args.removeFirstOrNull())
                attrDetail -> onAttrDetail(sender, args.removeFirstOrNull())

                else -> onUnknownUsage(sender)
            }
        }
        return true
    }

    private fun onUnknownUsage(sender: CommandSender) {
        sender.sendMessage("未知的用法, 使用 /$commandName $help 查看")
    }

    private fun onOnlyPlayer(sender: CommandSender) {
        sender.sendMessage("这个指令只能由玩家使用")
    }

    /**
     * true if no permission
     */
    private fun checkPermission(sender: CommandSender, permission: String): Boolean {
        if (!sender.hasPermission(permission)) {
            sender.sendMessage("你没有权限执行此指令")
            return true
        }
        return false
    }

    private fun onHelp(sender: CommandSender, command: Command, targetCommand: String? = null) {
        val message = TextComponent.builder()
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

        TextAdapter.sendMessage(sender, message.build())
    }

    //region operations
    private fun onPlay(sender: CommandSender, musicFileName: String?) {
        if (musicFileName == null) {
            onUnknownUsage(sender)
            return
        }
        if (sender !is Player) {
            onOnlyPlayer(sender)
            return
        }
        val parsedMusicFileName = MusicInfo.parseMusicFileName(musicFileName)

        val musicInfo = MusicManager.getMusics().find { it.musicFileName == parsedMusicFileName }

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

    private fun onJoin(sender: CommandSender, targetPlayerName: String?) {
        if (targetPlayerName == null) {
            onUnknownUsage(sender)
            return
        }
        if (sender !is Player) {
            onOnlyPlayer(sender)
            return
        }
        val targetPlayer = Bukkit.getPlayer(targetPlayerName)
        if (targetPlayer == null) {
            sender.sendMessage("未知的玩家 $targetPlayerName")
            return
        }
        MusicManager.join(sender, targetPlayer)
    }

    private fun onInvite(sender: CommandSender, targets: MutableList<String>) {
        if (sender !is Player) {
            onOnlyPlayer(sender)
            return
        }

        targets.forEach {
            val targetPlayer = Bukkit.getPlayer(it)
            if (targetPlayer == null) {
                sender.sendMessage("未知的玩家 $it")
                return
            }
            MusicManager.invite(sender, targetPlayer)
        }
    }

    private fun onAccept(sender: CommandSender) {
        if (sender !is Player) {
            onOnlyPlayer(sender)
            return
        }
        MusicManager.accept(sender)
    }

    private fun onDeny(sender: CommandSender) {
        if (sender !is Player) {
            onOnlyPlayer(sender)
            return
        }
        MusicManager.deny(sender)
    }

    private fun onCancel(sender: CommandSender) {
        if (sender !is Player) {
            onOnlyPlayer(sender)
            return
        }
        MusicManager.cancel(sender)
    }

    private fun onQuit(sender: CommandSender) {
        if (sender !is Player) {
            onOnlyPlayer(sender)
            return
        }
        MusicManager.quit(sender)
    }

    private fun onModify(sender: CommandSender, args: MutableList<String>) {
        if (sender !is Player) {
            onOnlyPlayer(sender)
            return
        }
        if (checkPermission(sender, modifyPermission)) return
        if (args.size < 2) {
            onUnknownUsage(sender)
            return
        }

        val attrs = MusicInfo.getAttrs()

        val musicFileName = MusicInfo.parseMusicFileName(args.first())
        var musicInfo = MusicManager.getMusics().find { it.musicFileName == musicFileName }

        if (musicInfo == null) { // 不指定音乐文件名
            musicInfo = MusicManager.getPlayingMusicInfo(player = sender)
            if (musicInfo == null) { // 没有正在播放
                sender.sendMessage("未播放时必须指定音乐文件名")
                return
            }
        } else if (args.size < 3) { // 指定但参数长度不足
            onUnknownUsage(sender)
            return
        } else { // 指定音乐文件名
            args.removeFirst()
        }

        val attrName = args.removeFirst()
        val attr = attrs.find { it.name == attrName }
        val valueStr: String = args.removeFirst()

        if (attr == null) {
            sender.sendMessage("未知的属性")
            return
        }

        when (attr.returnType) {
            Boolean::class.createType() -> {
                val value = valueStr.toBooleanStrictOrNull()
                if (value == null) {
                    sender.sendMessage("未知的值, 需要 true 或 false")
                    return
                }
                attr.setter.call(musicInfo, value)
            }
            Int::class.createType() -> {
                val value = valueStr.toIntOrNull()
                if (value == null) {
                    sender.sendMessage("未知的值, 需要一个数字")
                    return
                }
                attr.setter.call(musicInfo, value)
            }
            Double::class.createType() -> {
                val value = valueStr.toDoubleOrNull()
                if (value == null) {
                    sender.sendMessage("未知的值, 需要一个数字")
                    return
                }
                attr.setter.call(musicInfo, value)
            }
            String::class.createType() -> {
                attr.setter.call(musicInfo, valueStr)
            }
        }

        MusicManager.modify(musicInfo)
        val attrInfo = MusicInfo.getAttrInfo(attrName)
        sender.sendMessage("成功修改 ${musicInfo.musicFileName} 的 ${attrInfo.name} 为 $valueStr")
    }
    //endregion

    //region visualize
    private fun onList(sender: CommandSender, pageNum: Int?) {

    }

    private fun onControls(sender: CommandSender) {
        if (sender !is Player) {
            onOnlyPlayer(sender)
            return
        }

        val pauseText = "暂停".toCB()
        pauseText.clickEvent(ClickEvent.runCommand("/${commandName} $pause"))
        pauseText.hoverEvent(HoverEvent.showText("暂停播放".toComponent()))

        val resumeText = "继续".toCB()
        resumeText.clickEvent(ClickEvent.runCommand("/${commandName} $resume"))
        resumeText.hoverEvent(HoverEvent.showText("继续播放".toComponent()))

        val stopText = "停止".toCB()
        stopText.clickEvent(ClickEvent.runCommand("/${commandName} $stop"))
        stopText.hoverEvent(HoverEvent.showText("停止播放".toComponent()))

        val listText = "列表".toCB()
        listText.clickEvent(ClickEvent.runCommand("/${commandName} $list"))
        listText.hoverEvent(HoverEvent.showText("列出音乐".toComponent()))


        val message = TextComponent.builder()

        message.append(pauseText).append(" ")
        message.append(resumeText).append(" ")
        message.append(stopText).append(" ")
        message.append(listText).append(" ")

        TextAdapter.sendMessage(sender, message.build())
    }

    private fun onSettings(sender: CommandSender, musicFileName: String?) {
        if (checkPermission(sender, settingsPermission)) return

        val musicInfo: MusicInfo

        if (musicFileName == null) {
            if (sender !is Player) {
                sender.sendMessage("非玩家执行时必须提供文件名")
                return
            } else {
                val info = MusicManager.getPlayingMusicInfo(sender)
                if (info == null) {
                    sender.sendMessage("未播放时必须提供文件名")
                    return
                }
                musicInfo = info
            }
        } else {
            val fileName = MusicInfo.parseMusicFileName(musicFileName)
            val info = MusicManager.getMusics().find { it.musicFileName == fileName }
            if (info == null) {
                sender.sendMessage("未知的音乐文件")
                return
            }
            musicInfo = info
        }

        val message = "${musicInfo.musicFileName} 的设置项".toCB()

        val attrs = MusicInfo.getAttrs()
        attrs.forEach {
            val attrInfo = MusicInfo.getAttrInfo(it.name)
            val valueStr = it.getter.call(musicInfo).toString()

            val detailText = "详情".toCB()
                .color(TextColor.GRAY)
                .hoverEvent(HoverEvent.showText("点击查看详情".toComponent()))
                .clickEvent(ClickEvent.runCommand("/$commandName $attrDetail ${it.name}"))
            val fileNameWithoutSpace = MusicInfo.removeFileNameSpaces(musicInfo.musicFileName)
            val modifyText = "修改".toCB()
                .color(TextColor.GRAY)
                .hoverEvent(HoverEvent.showText("点击补全修改命令".toComponent()))
                .clickEvent(
                    ClickEvent.suggestCommand(
                        "/$commandName $modify $fileNameWithoutSpace ${it.name} $valueStr"
                    )
                )

            val attrMessage = "属性: ".toCB()
                .append(attrInfo.name.toCB().color(TextColor.LIGHT_PURPLE))
                .append(", 当前: ")
                .append(valueStr.toCB().color(TextColor.AQUA))
                .append("; ")
            attrMessage
                .append("[")
                .append(detailText)
                .append("]")
                .append(TextComponent.space())
            if (sender.hasPermission(modifyPermission))
                attrMessage.append("[").append(modifyText).append("]").append(TextComponent.space())

            message
                .append(TextComponent.newline())
                .append(attrMessage)
        }

        TextAdapter.sendMessage(sender, message.build())
    }

    private fun onAttrDetail(sender: CommandSender, attrName: String?) {
        if (attrName == null) {
            onUnknownUsage(sender)
            return
        }

        val attrInfo = MusicInfo.getAttrInfo(attrName)
        sender.sendMessage(
            "属性: ${attrInfo.name}" +
                    if (attrInfo.description.isNotBlank()) "\n  ${attrInfo.description}" else ""
        )
    }
    //endregion
}