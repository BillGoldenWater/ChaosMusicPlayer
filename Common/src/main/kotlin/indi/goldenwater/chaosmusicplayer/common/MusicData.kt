/*
 * Copyright 2021-2022 Golden_Water
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package indi.goldenwater.chaosmusicplayer.common

import indi.goldenwater.chaosmusicplayer.common.utils.printDebug
import org.jtransforms.dst.DoubleDST_1D
import java.nio.ByteBuffer
import javax.sound.sampled.AudioFormat

private val dSTs: MutableMap<Int, DoubleDST_1D> = mutableMapOf()

/*
 * Copyright 2021-2022 Golden_Water
 * SPDX-License-Identifier: AGPL-3.0-only
 */

data class MusicData(
  val dstLen: Int,
  val ticksPerSecond: Double,
  val sampleRate: Float,
  val sampleNum: Int,
  val items: List<DstItem>,
) {
  @Suppress("MemberVisibilityCanBePrivate")
  val lengthInSecond: Double = 1.0 / ticksPerSecond

  fun convToFrequency(value: Int): Double = (((value + 1) / 2.0) * (if (value < 0) -1.0 else 1.0)) * ticksPerSecond

  fun encode(): ByteArray {
    val byteBuffer = ByteBuffer.allocate(size())

    byteBuffer.putInt(dstLen)
    byteBuffer.putDouble(ticksPerSecond)
    byteBuffer.putFloat(sampleRate)
    byteBuffer.putInt(sampleNum)
    byteBuffer.putInt(items.size)

    for (item in items) {
      byteBuffer.putInt(item.index)
      byteBuffer.putFloat(item.valueNormalized)
    }

    return byteBuffer.array()
  }

  private fun size(): Int = HeaderSize + (DstItem.size * items.size)

  fun toSamples(): DoubleArray {
    val arr = DoubleArray(sampleNum) { 0.0 }

    for (item in items) {
      if (item.index < arr.size) {
        arr[item.index] = item.valueNormalized.toDouble() * sampleNum / 2
      }
    }

    val dst = dSTs.getOrPut(sampleNum) { DoubleDST_1D(sampleNum.toLong()) }
    dst.inverse(arr, false)

    return arr
  }

  companion object {
    private const val MaxSize = 32766
    private const val HeaderSize =
      Int.SIZE_BYTES /* dstLen */ + Double.SIZE_BYTES /* tickPerSecond */ + Int.SIZE_BYTES /* sampleRate */ + Int.SIZE_BYTES /* sampleNum */ + Int.SIZE_BYTES /* size */
    const val AvailableItemsNumber = (MaxSize - HeaderSize) / (DstItem.size)

    @Suppress("unused")
    fun fromEncoded(encoded: ByteArray): MusicData {
      if (encoded.size < HeaderSize) throw IllegalArgumentException("empty header")

      val data = ByteBuffer.wrap(encoded)

      val dstLen = data.int
      val ticksPerSecond = data.double
      val sampleRate = data.float
      val sampleNum = data.int

      val size = data.int
      val items = (1..size).map {
        val index = data.int
        val value = data.float
        DstItem(index = index, valueNormalized = value)
      }

      return MusicData(
        dstLen = dstLen,
        ticksPerSecond = ticksPerSecond,
        sampleRate = sampleRate,
        sampleNum = sampleNum,
        items = items,
      )
    }

    fun toAudioNSecond(audioFormat: AudioFormat, datas: List<MusicData>, volumeScale: Double, seconds: Int): ByteArray {
      val tLen = audioFormat.sampleRate.toInt() * seconds

      @Suppress("NAME_SHADOWING")
      val datas = datas.map { it.toSamples() }

      datas.forEachIndexed { i, it ->
        if (i < datas.size - 1) {
          val cur = it[it.size - 1]
          val next = datas[i + 1][0]

          val move = cur - next / 4
          it[it.size - 1] = cur - move
          datas[i + 1][0] = next + move
        }
      }

      val data = datas.flatMap { it.asIterable() }.toDoubleArray()

      val scale = data.size.toDouble() / tLen
      printDebug("${data.size} to $tLen with $scale")

      val buf = ByteBuffer.allocate(tLen * Short.SIZE_BYTES)

      for (i in 0 until tLen) {
        buf.putShort((data[(i * scale).toInt()] * volumeScale * Short.MAX_VALUE).toInt().toShort())
      }

      return buf.array()
    }
  }
}
