/*
 * Copyright 2021-2022 Golden_Water
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package indi.goldenwater.chaosmusicplayer.common.utils

fun Double.split(): Pair<Int, Double> {
  val int = this.toInt()

  return int to this - int
}