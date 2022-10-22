/*
 * Copyright 2021-2022 Golden_Water
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package indi.goldenwater.chaosmusicplayer.common

import org.jtransforms.dst.DoubleDST_1D
import java.nio.ByteBuffer
import javax.sound.sampled.AudioFormat
import kotlin.math.roundToInt

private val dSTs: MutableMap<Int, DoubleDST_1D> = mutableMapOf()

/*
 * Copyright 2021-2022 Golden_Water
 * SPDX-License-Identifier: AGPL-3.0-only
 */

data class MusicData(
  val items: List<DstItem> = listOf(),
  val dstLen: Int = 0,
  val ticksPerSecond: Double = 20.0,
) {
  @Suppress("MemberVisibilityCanBePrivate")
  val lengthInSecond: Double = 1.0 / ticksPerSecond

  fun convToFrequency(value: Int): Double = (((value + 1) / 2.0) * (if (value < 0) -1.0 else 1.0)) * ticksPerSecond

  fun encode(): ByteArray {
    val byteBuffer = ByteBuffer.allocate(size())

    byteBuffer.putDouble(ticksPerSecond)
    byteBuffer.putInt(dstLen)
    byteBuffer.putInt(items.size)

    for (item in items) {
      byteBuffer.putInt(item.index)
      byteBuffer.putFloat(item.valueNormalized)
    }

    return byteBuffer.array()
  }

  private fun size(): Int = HeaderSize + (DstItem.size * items.size)

  @Suppress("unused")
  fun toAudio(audioFormat: AudioFormat, speed: Double, volumeScale: Double): ByteArray {
    val len = (audioFormat.sampleRate * lengthInSecond * (1 / speed)).roundToInt()
    return toAudio(len, volumeScale)
  }

  @Suppress("MemberVisibilityCanBePrivate")
  fun toAudio(len: Int, volumeScale: Double): ByteArray {
    val halfLen = len / 2.0

    val arr = DoubleArray(len) { 0.0 }

    for (item in items) {
      if (item.index < arr.size) {
        arr[item.index] = item.valueNormalized.toDouble() * halfLen
      }
    }

    val dst = dSTs.getOrPut(len) { DoubleDST_1D(len.toLong()) }
    dst.inverse(arr, false)

    val buf = ByteBuffer.allocate(arr.size * Short.SIZE_BYTES)

    for (d in arr) {
      buf.putShort((d * Short.MAX_VALUE * 0.9 * volumeScale).toInt().toShort())
      // 0.9 is for reduce some crack by limit max volume to 90%
    }

    return buf.array()
  }

  companion object {
    private const val MaxSize = 32766
    private const val HeaderSize =
      Double.SIZE_BYTES /* tickPerSecond */ + Int.SIZE_BYTES /* dstLen */ + Int.SIZE_BYTES /* size */
    const val AvailableItemsNumber = (MaxSize - HeaderSize) / (DstItem.size)

    @Suppress("unused")
    fun fromEncoded(encoded: ByteArray): MusicData {
      if (encoded.size < HeaderSize) return MusicData()

      val data = ByteBuffer.wrap(encoded)
      val ticksPerSecond = data.double
      val dstLen = data.int
      val size = data.int

      val items = (1..size).map {
        val index = data.int
        val value = data.float
        DstItem(index = index, valueNormalized = value)
      }

      return MusicData(items = items, ticksPerSecond = ticksPerSecond, dstLen = dstLen)
    }

//    fun toAudioMultiple(audioFormat: AudioFormat, datas: List<MusicData>) {
//      val diff = 0.0
//
//      (audioFormat.sampleRate * lengthInSecond).toInt()
//    }
  }
}
