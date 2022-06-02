/*
 * Copyright 2021-2022 Golden_Water
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package indi.goldenwater.chaosmusicplayer.utils

import net.kyori.text.TextComponent

fun String.toComponent(): TextComponent = TextComponent.builder(this).build()

fun String.toCB(): TextComponent.Builder = TextComponent.builder(this)