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
import kotlin.reflect.KFunction
import kotlin.reflect.full.createType
import kotlin.reflect.full.memberFunctions

@Suppress("unused")
object CommandCMP : CommandExecutor {
    const val commandName = "chaosmusicplayer"

    val commandHandlers: List<KFunction<*>>

    init {
        commandHandlers = CommandCMP::class.memberFunctions
            .filter { p -> p.annotations.any { it is CommandHandler } }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val listArgs = mutableListOf<String>()
        listArgs.addAll(args)
        return this.onCommand(sender, listArgs)
    }

    private fun onCommand(sender: CommandSender, args: MutableList<String>): Boolean {
        if (args.isEmpty()) {
            onHelp(sender, mutableListOf())
        } else {
            val cmd = args.removeFirst()
            val handler = getCommandHandler(cmd)

            if (handler == null) {
                onUnknownUsage(sender)
                return true
            }

            val commandInfo = getCommandInfo(handler)
            if (commandInfo.minimumArgNum > args.size) {
                onUnknownUsage(sender)
                return true
            }
            if (commandInfo.onlyPlayer && sender !is Player) {
                onOnlyPlayer(sender)
                return true
            }
            if (checkPermission(sender, handler)) return true

            if (handler.parameters.size == 3) {
                handler.call(CommandCMP, sender, args)
            } else {
                handler.call(CommandCMP, sender)
            }
        }
        return true
    }

    private fun onUnknownUsage(sender: CommandSender) {
        val helpCmd = getCmd(this::onHelp)
        val message = "未知的用法, 使用 $helpCmd 查看".toCB()
            .hoverEvent(HoverEvent.showText("点击获取用法".toComponent()))
            .clickEvent(ClickEvent.runCommand(helpCmd))
        TextAdapter.sendMessage(sender, message.build())
    }

    private fun onOnlyPlayer(sender: CommandSender) {
        sender.sendMessage("这个指令只能由玩家使用")
    }

    /**
     * true if no permission
     */
    private fun checkPermission(sender: CommandSender, commandHandler: KFunction<*>): Boolean {
        if (!hasPermission(sender, commandHandler)) {
            sender.sendMessage("你没有权限执行此指令")
            return true
        }
        return false
    }

    private fun getHelpUsage(commandInfo: CommandHandler, detail: Boolean = false): TextComponent.Builder {
        val usage = TextComponent.builder()

        usage.append("/$commandName ${commandInfo.command}")
        if (commandInfo.argumentsInfo.isNotBlank()) {
            usage
                .append(TextComponent.space())
                .append(commandInfo.argumentsInfo)
        }
        if (!detail) {
            usage
                .append(TextComponent.space())
                .append(commandInfo.description)
        } else {
            usage.append(TextComponent.newline())
            commandInfo.descriptionDetail.lines().forEach { usage.append("  $it") }
        }

        val handler = getCommandHandler(commandInfo)
        if (commandInfo.minimumArgNum == 0) {
            usage.clickEvent(ClickEvent.runCommand(getCmd(handler)))
                .hoverEvent(HoverEvent.showText("点击执行".toComponent()))
        } else {
            usage.clickEvent(ClickEvent.suggestCommand("${getCmd(handler)} "))
                .hoverEvent(HoverEvent.showText("点击补全指令".toComponent()))
        }

        return usage
    }

    @CommandHandler(
        "help",
        argumentsInfo = "[指令]", description = "显示用法", descriptionDetail = "显示指令的详细用法"
    )
    fun onHelp(sender: CommandSender, args: MutableList<String>) {
        val message = TextComponent.builder()
        message.append("${ChaosMusicPlayer.instance.name} By.Golden_Water")

        val targetCommand = args.removeFirstOrNull()

        if (targetCommand == null || targetCommand.isBlank()) {
            message
                .append(TextComponent.newline())
                .append("指令别名: cmp, music")

            commandHandlers.forEach {
                val info = getCommandInfo(it)
                if (!hasPermission(sender, it)) return@forEach

                message
                    .append(TextComponent.newline())
                    .append(getHelpUsage(info))
            }
        } else {
            val handler = getCommandHandler(targetCommand)

            if (handler == null) {
                sender.sendMessage("未知的命令 $targetCommand")
                return
            }

            val info = getCommandInfo(handler)

            message
                .append(TextComponent.newline())
                .append(getHelpUsage(info, detail = true))
        }

        TextAdapter.sendMessage(sender, message.build())
    }

    //region operations
    @CommandHandler(
        "play", minimumArgNum = 1, onlyPlayer = true,
        argumentsInfo = "<文件名>", description = "播放音乐", descriptionDetail = "播放指定的音乐"
    )
    fun onPlay(sender: Player, args: MutableList<String>) {
        val musicFileName = args.first()

        val parsedMusicFileName = MusicInfo.parseMusicFileName(musicFileName)

        val musicInfo = MusicManager.getMusics().find { it.musicFileName == parsedMusicFileName }

        if (musicInfo == null) {
            sender.sendMessage("未知的音乐文件")
        } else {
            MusicManager.play(sender, musicInfo)
        }
    }

    @CommandHandler("pause", onlyPlayer = true, description = "暂停", descriptionDetail = "暂停正在播放的音乐")
    fun onPause(sender: Player) {
        MusicManager.pause(sender)
    }

    @CommandHandler("resume", onlyPlayer = true, description = "恢复", descriptionDetail = "恢复之前暂停的音乐(如果没停止的话)")
    fun onResume(sender: Player) {
        MusicManager.resume(sender)
    }

    @CommandHandler("stop", onlyPlayer = true, description = "停止", descriptionDetail = "停止正在播放的音乐")
    fun onStop(sender: Player) {
        MusicManager.stop(sender)
    }

    @CommandHandler(
        "join", minimumArgNum = 1, onlyPlayer = true,
        argumentsInfo = "<玩家名>", description = "加入一起听", descriptionDetail = "申请和对方一起听"
    )
    fun onJoin(sender: Player, args: MutableList<String>) {
        val targetPlayerName = args.removeFirst()

        val targetPlayer = Bukkit.getPlayer(targetPlayerName)
        if (targetPlayer == null) {
            sender.sendMessage("未知的玩家 $targetPlayerName")
            return
        }
        MusicManager.join(sender, targetPlayer)
    }

    @CommandHandler(
        "invite", minimumArgNum = 1, onlyPlayer = true,
        argumentsInfo = "<玩家名>", description = "邀请一起听", descriptionDetail = "邀请对方一起听"
    )
    fun onInvite(sender: Player, args: MutableList<String>) {
        args.forEach {
            val targetPlayer = Bukkit.getPlayer(it)
            if (targetPlayer == null) {
                sender.sendMessage("未知的玩家 $it")
                return
            }
            MusicManager.invite(sender, targetPlayer)
        }
    }

    @CommandHandler(
        "accept",
        description = "同意/接受请求/邀请", descriptionDetail = "同意/接受最近一个其他人发给你的请求/邀请"
    )
    fun onAccept(sender: CommandSender) {
        if (sender !is Player) {
            onOnlyPlayer(sender)
            return
        }
        MusicManager.accept(sender)
    }

    @CommandHandler(
        "deny",
        description = "拒绝请求/邀请", descriptionDetail = "拒绝最近一个其他人发给你的请求/邀请"
    )
    fun onDeny(sender: CommandSender) {
        if (sender !is Player) {
            onOnlyPlayer(sender)
            return
        }
        MusicManager.deny(sender)
    }

    @CommandHandler(
        "cancel",
        description = "取消请求/邀请", descriptionDetail = "取消所有你发给其他人的请求/邀请"
    )
    fun onCancel(sender: CommandSender) {
        if (sender !is Player) {
            onOnlyPlayer(sender)
            return
        }
        MusicManager.cancel(sender)
    }

    @CommandHandler(
        "quit",
        description = "退出一起听", descriptionDetail = "退出你加入的一起听"
    )
    fun onQuit(sender: CommandSender) {
        if (sender !is Player) {
            onOnlyPlayer(sender)
            return
        }
        MusicManager.quit(sender)
    }

    @CommandHandler(
        "modify", permission = "chaosmusicplayer.modify", minimumArgNum = 2, onlyPlayer = true,
        argumentsInfo = "[文件名] <属性名> <值>",
        description = "修改音乐参数", descriptionDetail = "修改指定音乐的参数, 没有正在播放时必须提供文件名"
    )
    fun onModify(sender: Player, args: MutableList<String>) {
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
        val valueStr: String = args.joinToString(separator = " ")

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
    private const val itemPerPage = 10

    @CommandHandler(
        "list",
        argumentsInfo = "[页数]", description = "列出音乐", descriptionDetail = "列出第n页的音乐"
    )
    fun onList(sender: CommandSender, args: MutableList<String>) {
        val pageNum = args.removeFirstOrNull()?.toIntOrNull() ?: 1
        val pageIndex = pageNum - 1
        val musicInfos = MusicManager.getMusics()
        var i = 0
        val pages = musicInfos.groupBy { (i++) / itemPerPage }

        if (musicInfos.size == 0) {
            sender.sendMessage("没有音乐可供列出")
            return
        } else if (!pages.containsKey(pageIndex)) {
            sender.sendMessage("未知的页数")
            return
        }

        //region header
        val pageNumText = pageNum.toString().toCB().color(TextColor.AQUA)
        val message = TextComponent
            .builder()
            .append("第")
            .append(TextComponent.space())
            .append(pageNumText)
            .append(TextComponent.space())
            .append("页")
        //endregion

        //region body
        pages[pageIndex]?.forEach {
            val fileName = MusicInfo.removeFileNameSpaces(it.musicFileName)
            val playText = "播放"
                .toCB()
                .color(TextColor.GRAY)
                .hoverEvent(HoverEvent.showText("点击播放".toComponent()))
                .clickEvent(ClickEvent.runCommand(getCmd(this::onPlay, fileName)))
            val settingsText = "设置"
                .toCB()
                .color(TextColor.GRAY)
                .hoverEvent(HoverEvent.showText("点击查看设置".toComponent()))
                .clickEvent(ClickEvent.runCommand(getCmd(this::onSettings, fileName)))

            val musicMsg = TextComponent
                .builder()
                .append(it.displayName)
                .append(TextComponent.space())
                .append("[")
                .append(playText)
                .append("]")
                .append(TextComponent.space())
                .append("[")
                .append(settingsText)
                .append("]")

            message.append(TextComponent.newline()).append(musicMsg)
        }
        //endregion

        //region footer
        val previousIndex = pageIndex - 1
        val nextIndex = pageIndex + 1
        val hasPrevious = pages.containsKey(previousIndex)
        val hasNext = pages.containsKey(nextIndex)

        val previousPage = "上一页".toCB().color(if (hasPrevious) TextColor.GRAY else TextColor.DARK_GRAY)
        val nextPage = "下一页".toCB().color(if (hasNext) TextColor.GRAY else TextColor.DARK_GRAY)

        if (hasPrevious) {
            previousPage
                .hoverEvent(HoverEvent.showText("前往上一页".toComponent()))
                .clickEvent(ClickEvent.runCommand(getCmd(this::onList, (previousIndex + 1).toString())))
        } else {
            previousPage.hoverEvent(HoverEvent.showText("没有上一页".toComponent()))
        }
        if (hasNext) {
            nextPage
                .hoverEvent(HoverEvent.showText("前往下一页".toComponent()))
                .clickEvent(ClickEvent.runCommand(getCmd(this::onList, (nextIndex + 1).toString())))
        } else {
            nextPage.hoverEvent(HoverEvent.showText("没有下一页".toComponent()))
        }

        val footer = TextComponent
            .builder()
            .append("[")
            .append(previousPage)
            .append("]")
            .append(TextComponent.space())
            .append(pageNumText)
            .append(TextComponent.space())
            .append("[")
            .append(nextPage)
            .append("]")

        message.append(TextComponent.newline()).append(footer)
        //endregion

        TextAdapter.sendMessage(sender, message.build())
    }

    @CommandHandler(
        "controls", onlyPlayer = true,
        description = "播放控制", descriptionDetail = "显示播放控制按钮"
    )
    fun onControls(sender: CommandSender) {
        val pauseText = "暂停".toCB()
        pauseText.clickEvent(ClickEvent.runCommand("/${commandName} ${getCommandInfo(this::onPause).command}"))
        pauseText.hoverEvent(HoverEvent.showText("暂停播放".toComponent()))

        val resumeText = "继续".toCB()
        resumeText.clickEvent(ClickEvent.runCommand(getCmd(this::onResume)))
        resumeText.hoverEvent(HoverEvent.showText("继续播放".toComponent()))

        val stopText = "停止".toCB()
        stopText.clickEvent(ClickEvent.runCommand(getCmd(this::onStop)))
        stopText.hoverEvent(HoverEvent.showText("停止播放".toComponent()))

        val listText = "列表".toCB()
        listText.clickEvent(ClickEvent.runCommand(getCmd(this::onList)))
        listText.hoverEvent(HoverEvent.showText("列出音乐".toComponent()))


        val message = TextComponent.builder()

        message.append(pauseText).append(" ")
        message.append(resumeText).append(" ")
        message.append(stopText).append(" ")
        message.append(listText).append(" ")

        TextAdapter.sendMessage(sender, message.build())
    }

    @CommandHandler(
        "settings", permission = "chaosmusicplayer.settings",
        argumentsInfo = "[文件名]",
        description = "显示参数", descriptionDetail = "显示指定音乐的参数, 没有正在播放时必须提供文件名"
    )
    fun onSettings(sender: CommandSender, args: MutableList<String>) {
        val musicFileName = args.removeFirstOrNull()

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
                .hoverEvent(HoverEvent.showText(attrInfo.description.toComponent()))
            val fileNameWithoutSpace = MusicInfo.removeFileNameSpaces(musicInfo.musicFileName)
            val modifyText = "修改".toCB()
                .color(TextColor.GRAY)
                .hoverEvent(HoverEvent.showText("点击补全修改命令".toComponent()))
                .clickEvent(
                    ClickEvent.suggestCommand(getCmd(this::onModify, fileNameWithoutSpace, it.name, valueStr))
                )

            val attrMessage = "属性: "
                .toCB()
                .append(attrInfo.name.toCB().color(TextColor.LIGHT_PURPLE))
                .append(", 当前: ")
                .append(valueStr.toCB().color(TextColor.AQUA))
                .append("; ")
                .append("[")
                .append(detailText)
                .append("]")
            if (hasPermission(sender, this::onModify)) attrMessage
                .append(TextComponent.space())
                .append("[")
                .append(modifyText)
                .append("]")

            message.append(TextComponent.newline()).append(attrMessage)
        }

        TextAdapter.sendMessage(sender, message.build())
    }
    //endregion

    //region commandInfo
    @Target(AnnotationTarget.FUNCTION)
    annotation class CommandHandler(
        val command: String,
        val permission: String = "",
        val minimumArgNum: Int = 0,
        val onlyPlayer: Boolean = false,
        val argumentsInfo: String = "",
        val description: String = "",
        val descriptionDetail: String = "",
    )

    private fun getCommandInfo(commandHandler: KFunction<*>): CommandHandler {
        return (
                commandHandler.annotations.find { it is CommandHandler }
                    ?: throw IllegalArgumentException("${commandHandler.name} is not a command handler.")
                ) as CommandHandler
    }

    private fun getCmdName(commandHandler: KFunction<*>): String {
        return getCommandInfo(commandHandler).command
    }

    fun getCmd(commandHandler: KFunction<*>, vararg args: String): String {
        val cmdName = getCmdName(commandHandler)
        val argsStr = if (args.isNotEmpty()) " ${args.joinToString(separator = " ")}" else ""
        return "/$commandName $cmdName$argsStr"
    }

    private fun hasPermission(sender: CommandSender, commandHandler: KFunction<*>): Boolean {
        val perm = getPermission(commandHandler)
        return perm.isBlank() || sender.hasPermission(perm)
    }

    private fun getCommandHandler(command: String): KFunction<*>? {
        return commandHandlers.find { handler ->
            getCommandInfo(handler).command == command
        }
    }

    private fun getCommandHandler(commandInfo: CommandHandler): KFunction<*> {
        return commandHandlers.find { getCommandInfo(it) == commandInfo }
            ?: throw IllegalArgumentException("${commandInfo.command} is not exists.")
    }

    private fun getPermission(commandHandler: KFunction<*>): String {
        return getCommandInfo(commandHandler).permission
    }
    //endregion
}