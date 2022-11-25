/*
 * Copyright 2021-2022 Golden_Water
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package indi.goldenwater.chaosmusicplayer.common.utils

const val debug = false
fun printDebug(str: String) {
  if (debug) {
    println(str)
  }
}