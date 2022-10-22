/*
 * Copyright 2021-2022 Golden_Water
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package indi.goldenwater.chaosmusicplayer

import indi.goldenwater.chaosmusicplayer.common.MusicData
import indi.goldenwater.chaosmusicplayer.common.utils.split
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.network.PacketByteBuf
import net.minecraft.sound.SoundCategory
import net.minecraft.util.Identifier
import java.io.ByteArrayOutputStream
import java.util.*
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.concurrent.thread
import kotlin.math.roundToInt

val af = AudioFormat(44100f, Short.SIZE_BITS, 1, true, true)
val line: SourceDataLine = AudioSystem.getSourceDataLine(af)

// data, length in second
val buffer = mutableListOf<MusicData>()
const val defaultBufferLengthInSecond: Double = 0.5
var bufferLength = defaultBufferLengthInSecond
var minecraftClient: MinecraftClient? = null

@Suppress("UNUSED")
object ChaosMusicPlayer : ClientModInitializer {
  override fun onInitializeClient() {
    line.open(af, 128000)
    line.start()

    startPlayLoop()

    ClientPlayConnectionEvents.JOIN.register { _: ClientPlayNetworkHandler,
                                               _: PacketSender, client: MinecraftClient ->
      minecraftClient = client
      // region register data channel
      ClientPlayNetworking.registerReceiver(
        Identifier(
          "chaosmusicplayer",
          "music_data"
        )
      ) { _: MinecraftClient, _: ClientPlayNetworkHandler,
          packetByteBuf: PacketByteBuf, _: PacketSender ->

        val arr = ByteArray(packetByteBuf.readableBytes())
        packetByteBuf.readBytes(arr)

        val decoded = MusicData.fromEncoded(arr)
        buffer.add(decoded)
      }
      // endregion

      // region send enable signal
      ClientPlayNetworking.send(
        Identifier("chaosmusicplayer", "enable_direct_send"),
        PacketByteBufs.empty()
      )
      // endregion
    }

    ClientPlayConnectionEvents.DISCONNECT.register { _: ClientPlayNetworkHandler, _: MinecraftClient ->
      minecraftClient = null
    }

    ClientLifecycleEvents.CLIENT_STOPPING.register {
      line.stop()
      line.close()
    }
  }
}

fun startPlayLoop() {
  thread {
    while (true) {
      // region calc bufferSize
      val bufferSize =
        (bufferLength / (buffer.getOrNull(0)?.lengthInSecond ?: 0.2))
          .roundToInt()
          .coerceAtLeast(1)
      // endregion

      if (buffer.size > bufferSize) {
        var lenDiff = 0.0

        var arr = getAudio(lenDiff).let { lenDiff = it.second; it.first }

        while (true) {
          line.write(arr, 0, arr.size)
          val drain = thread { line.drain() }

          if (buffer.isEmpty()) break
          arr = getAudio(lenDiff).let { lenDiff = it.second; it.first }

          drain.join()

          if (buffer.size > bufferSize * 3) {
            println("[ChaosMusicPlayer] buffer larger than it should be (should ${bufferSize}, but ${buffer.size}), clear buffer.")
            buffer.clear()
          }
        }

        // region buffer auto adjust
        thread {
          Thread.sleep(100)
          if (buffer.size > 0) {
            println("[ChaosMusicPlayer] was out of buffer, adding buffer length")
            bufferLength += 0.1
          } else if (bufferLength != defaultBufferLengthInSecond) {
            println("[ChaosMusicPlayer] music end, reset buffer length")
            bufferLength = defaultBufferLengthInSecond
          }
        }
        // endregion
      } else {
        Thread.sleep(10)
      }
    }
  }
}

// data, newDiff
fun getAudio(lenDiff: Double): Pair<ByteArray, Double> {
  var diff = lenDiff

  val size = buffer.size
  val arrList = (1..size)
    .mapNotNull { buffer.removeFirstOrNull() }
  val bos = ByteArrayOutputStream()

  val volumeScaler = minecraftClient?.options?.getSoundVolume(SoundCategory.RECORDS) ?: 0.5f

  arrList.forEach {
    val lenDouble = af.sampleRate * it.lengthInSecond
    val lenSpilt = lenDouble.split()
    diff += lenSpilt.second

    val diffSplit = diff.split()
    val len = if (diffSplit.first >= 1) {
      diff -= diffSplit.first
      lenSpilt.first + diffSplit.first
    } else {
      lenSpilt.first
    }

    bos.write(it.toAudio(len, volumeScaler.toDouble()))
  }

  return bos.toByteArray() to diff
}