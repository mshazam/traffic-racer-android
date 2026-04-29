package com.trafficracer.game

import com.badlogic.gdx.Gdx

class GamePrefs {
    private val prefs = Gdx.app.getPreferences("traffic_racer_prefs")

    var highScore: Int
        get() = prefs.getInteger("high_score", 0)
        set(value) { prefs.putInteger("high_score", value); prefs.flush() }

    var totalCoins: Int
        get() = prefs.getInteger("total_coins", 0)
        set(value) { prefs.putInteger("total_coins", value); prefs.flush() }

    var controlScheme: Int
        get() = prefs.getInteger("control_scheme", 0)
        set(value) { prefs.putInteger("control_scheme", value); prefs.flush() }

    var orientationPortrait: Boolean
        get() = prefs.getBoolean("orientation_portrait", true)
        set(value) { prefs.putBoolean("orientation_portrait", value); prefs.flush() }

    var selectedCar: String
        get() = prefs.getString("selected_car", "sedan-sports") ?: "sedan-sports"
        set(value) { prefs.putString("selected_car", value); prefs.flush() }

    fun isCarUnlocked(carId: String): Boolean = prefs.getBoolean("car_$carId", carId == "sedan-sports")

    fun unlockCar(carId: String) { prefs.putBoolean("car_$carId", true); prefs.flush() }
}
