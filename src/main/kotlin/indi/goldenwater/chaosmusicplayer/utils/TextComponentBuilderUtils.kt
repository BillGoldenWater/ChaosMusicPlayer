package indi.goldenwater.chaosmusicplayer.utils

import net.kyori.adventure.text.TextComponent

fun TextComponent.Builder.append(string: String) {
    this.append(string.toComponent())
}