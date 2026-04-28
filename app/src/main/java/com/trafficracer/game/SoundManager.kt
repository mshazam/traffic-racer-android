package com.trafficracer.game

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class SoundManager(private val context: Context) {
    private var soundPool: SoundPool? = null
    private var toneGenerator: ToneGenerator? = null
    private val vibrator: Vibrator?

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(audioAttributes)
            .build()

        toneGenerator = try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 50)
        } catch (e: Exception) {
            null
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun playCoinPickup() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 50)
        vibrateLight()
    }

    fun playCrash() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 200)
        vibrateHeavy()
    }

    fun playPowerUp() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 100)
        vibrateLight()
    }

    fun playLaneSwitch() {
        toneGenerator?.startTone(ToneGenerator.TONE_DTMF_1, 30)
    }

    fun playBoost() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 150)
        vibrateMedium()
    }

    fun playGameOver() {
        toneGenerator?.startTone(ToneGenerator.TONE_SUP_ERROR, 500)
        vibrateHeavy()
    }

    private fun vibrateLight() {
        vibrate(30L, VibrationEffect.EFFECT_TICK)
    }

    private fun vibrateMedium() {
        vibrate(50L, VibrationEffect.EFFECT_CLICK)
    }

    private fun vibrateHeavy() {
        vibrate(150L, VibrationEffect.EFFECT_HEAVY_CLICK)
    }

    private fun vibrate(durationMs: Long, effectId: Int) {
        vibrator?.let { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    v.vibrate(VibrationEffect.createPredefined(effectId))
                } catch (e: Exception) {
                    v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            } else {
                v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        toneGenerator?.release()
        toneGenerator = null
    }
}
