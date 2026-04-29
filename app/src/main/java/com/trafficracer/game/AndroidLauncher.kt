package com.trafficracer.game

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("traffic_racer_prefs", Context.MODE_PRIVATE)
        val isPortrait = prefs.getBoolean("orientation_portrait", true)
        requestedOrientation = if (isPortrait)
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        else
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = true
            useCompass = false
            useGyroscope = false
            numSamples = 2
        }

        val game = TrafficRacerGame { portrait ->
            runOnUiThread {
                requestedOrientation = if (portrait)
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                else
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                prefs.edit().putBoolean("orientation_portrait", portrait).apply()
            }
        }
        initialize(game, config)
    }
}
