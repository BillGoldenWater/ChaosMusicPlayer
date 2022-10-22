/*
 * Copyright 2021-2022 Golden_Water
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package indi.goldenwater.chaosmusicplayer.common

data class DstItem(val index: Int, val valueNormalized: Float) {
  companion object {
    const val size = Int.SIZE_BYTES + Float.SIZE_BYTES
  }
}
