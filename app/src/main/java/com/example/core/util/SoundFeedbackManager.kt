package com.example.core.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.exp
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SoundFeedbackManager {
  private val scope = CoroutineScope(Dispatchers.Default)

  fun playSoftTick(enabled: Boolean) {
    if (!enabled) return
    scope.launch {
      try {
        val sampleRate = 22050
        val durationMs = 28
        val numSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
          val t = i.toDouble() / sampleRate
          val envelope = exp(-t * 140.0)
          val wave = sin(2.0 * Math.PI * 640.0 * t)
          samples[i] = (wave * envelope * 9000).toInt().toShort()
        }
        val audioTrack = AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build()
          )
          .setAudioFormat(
            AudioFormat.Builder()
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setSampleRate(sampleRate)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build()
          )
          .setBufferSizeInBytes(samples.size * 2)
          .setTransferMode(AudioTrack.MODE_STATIC)
          .build()

        audioTrack.write(samples, 0, samples.size)
        audioTrack.play()
        kotlinx.coroutines.delay(50)
        audioTrack.release()
      } catch (_: Exception) {
        // Ignore audio hardware issues on headless/test environments
      }
    }
  }
}
