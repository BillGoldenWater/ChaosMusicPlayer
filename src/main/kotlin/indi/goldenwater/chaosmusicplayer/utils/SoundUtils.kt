/*
 * Copyright 2021-2022 Golden_Water
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package indi.goldenwater.chaosmusicplayer.utils

import indi.goldenwater.chaosmusicplayer.ChaosMusicPlayer
import org.bukkit.SoundCategory
import org.bukkit.entity.Player

val allSoundEventName = getFrequenciesNeedGen().map { getSineWaveSoundEventName(it) }

fun stopAllSounds(player: Player) {
    if (ChaosMusicPlayer.legacyStop) {
        allSoundEventName.forEach { player.stopSound(it, SoundCategory.RECORDS) }
    } else {
        player.stopAllSounds()
    }
}