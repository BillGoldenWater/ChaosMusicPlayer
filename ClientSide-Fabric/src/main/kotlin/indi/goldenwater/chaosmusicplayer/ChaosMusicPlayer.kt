/*
 * Copyright 2021-2022 Golden_Water
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package indi.goldenwater.chaosmusicplayer

import indi.goldenwater.chaosmusicplayer.common.MusicData
import indi.goldenwater.chaosmusicplayer.common.utils.printDebug
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
import java.util.*
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.concurrent.thread
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis

val af = AudioFormat(88200f, Short.SIZE_BITS, 1, true, true)
val line: SourceDataLine = AudioSystem.getSourceDataLine(af)

// data, length in second
val buffer = mutableListOf<MusicData>()
val bufferLength = if (System.getProperty("os.name") == "Mac OS X") {
  1
} else {
  4
}
var minecraftClient: MinecraftClient? = null

@Suppress("UNUSED")
object ChaosMusicPlayer : ClientModInitializer {
  override fun onInitializeClient() {
    line.open(af, (af.sampleRate * (af.sampleSizeInBits / 8.0) * 2).roundToInt())
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
      try {
        while (true) {
          if (buffer.isEmpty()) {
            break
          }

          val size = (buffer[0].sampleRate.toDouble() / buffer[0].sampleNum).toInt() * bufferLength

          if (buffer.size > size * 3) {
            println("[ChaosMusicPlayer] buffer larger than it should be 3 times (should ${size}, but ${buffer.size}), clear buffer.")
            buffer.clear()
            break
          }

          printDebug("size: $size rate: ${buffer[0].sampleRate} num: ${buffer[0].sampleNum}")
          val arrList = (1..size)
            .mapNotNull { buffer.removeFirstOrNull() }

          val masterScaler = minecraftClient?.options?.getSoundVolume(SoundCategory.MASTER)?.toDouble() ?: 0.5
          val volumeScaler =
            (minecraftClient?.options?.getSoundVolume(SoundCategory.RECORDS)?.toDouble() ?: 0.5) * masterScaler

          val arr = MusicData.toAudioNSecond(af, arrList, volumeScaler, bufferLength)

          val duration = measureTimeMillis {
            line.write(arr, 0, arr.size)
            line.drain()
          }
          printDebug("duration: $duration")
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
      Thread.sleep(10)
    }
  }
}

// region buffer auto add
//        if (buffer.size < size * (1 + bufferLength)) {
//          if (!playing) {
//            break
//          } else if (buffer.size < size * 0.98) {
//            if (bufferLength < 10) {
//              bufferLength *= 1.5
//              println("[ChaosMusicPlayer] was out of buffer, adding buffer length to $bufferLength")
//            }
//            break
//          }
//        }
//
//        playing = true
// endregion