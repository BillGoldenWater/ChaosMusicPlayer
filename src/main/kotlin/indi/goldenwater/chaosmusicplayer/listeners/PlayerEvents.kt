/*
 * Copyright 2021-2022 Golden_Water
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package indi.goldenwater.chaosmusicplayer.listeners

import indi.goldenwater.chaosmusicplayer.music.DirectEnabledPlayers
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

object PlayerEvents : Listener {
  @EventHandler
  fun onPlayerQuitEvent(event: PlayerQuitEvent) {
    DirectEnabledPlayers.removePlayer(event.player.uniqueId)
  }
}