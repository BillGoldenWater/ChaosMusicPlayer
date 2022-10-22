/*
 * Copyright 2021-2022 Golden_Water
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package indi.goldenwater.chaosmusicplayer.music

import java.util.*

object DirectEnabledPlayers {
  private val players: MutableSet<UUID> = mutableSetOf()

  fun isEnabled(uuid: UUID) = players.contains(uuid)

  fun addPlayer(uuid: UUID) = players.add(uuid)

  fun removePlayer(uuid: UUID) = players.remove(uuid)
}