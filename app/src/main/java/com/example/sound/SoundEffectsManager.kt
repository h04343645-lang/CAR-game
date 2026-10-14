package com.example.sound

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class SoundEffectsManager(context: Context) {

    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (_: Exception) {
        null
    }

    private var toneGen: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 60)
    } catch (_: Exception) {
        null
    }

    fun playEnginePulse() {
        // Subtle haptic for acceleration
        vibrate(15, 40)
    }

    fun playCoinPickup() {
        toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 70)
        vibrate(30, 100)
    }

    fun playNitroEngage() {
        toneGen?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 120)
        vibrate(70, 180)
    }

    fun playCollisionImpact() {
        toneGen?.startTone(ToneGenerator.TONE_PROP_NACK, 160)
        vibrate(180, 255)
    }

    fun playHazardWarning() {
        toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP2, 90)
        vibrate(40, 80)
    }

    private fun vibrate(durationMs: Long, amplitude: Int) {
        try {
            vibrator?.let { v ->
                if (v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(durationMs)
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore if vibration fails or permission not granted
        }
    }

    fun release() {
        try {
            toneGen?.release()
            toneGen = null
        } catch (_: Exception) {}
    }
}
