package com.trafficracer.game

import android.content.Context

class GameData(context: Context) {
    private val prefs = context.getSharedPreferences("traffic_racer_data", Context.MODE_PRIVATE)

    var totalCoins: Long
        get() = prefs.getLong("total_coins", 0L)
        set(value) = prefs.edit().putLong("total_coins", value).apply()

    var highScore: Long
        get() = prefs.getLong("high_score", 0L)
        set(value) = prefs.edit().putLong("high_score", value).apply()

    var totalDistance: Long
        get() = prefs.getLong("total_distance", 0L)
        set(value) = prefs.edit().putLong("total_distance", value).apply()

    var totalGames: Int
        get() = prefs.getInt("total_games", 0)
        set(value) = prefs.edit().putInt("total_games", value).apply()

    var selectedCarId: String
        get() = prefs.getString("selected_car", "starter") ?: "starter"
        set(value) = prefs.edit().putString("selected_car", value).apply()

    var missionDifficulty: Int
        get() = prefs.getInt("mission_difficulty", 0)
        set(value) = prefs.edit().putInt("mission_difficulty", value).apply()

    var totalCoinsCollected: Long
        get() = prefs.getLong("total_coins_collected", 0L)
        set(value) = prefs.edit().putLong("total_coins_collected", value).apply()

    var bestCombo: Int
        get() = prefs.getInt("best_combo", 0)
        set(value) = prefs.edit().putInt("best_combo", value).apply()

    var bestDistanceWithoutCrash: Long
        get() = prefs.getLong("best_no_crash_distance", 0L)
        set(value) = prefs.edit().putLong("best_no_crash_distance", value).apply()

    var maxSpeedEver: Float
        get() = prefs.getFloat("max_speed_ever", 0f)
        set(value) = prefs.edit().putFloat("max_speed_ever", value).apply()

    var controlScheme: ControlScheme
        get() {
            val name = prefs.getString("control_scheme", ControlScheme.NFS.name) ?: ControlScheme.NFS.name
            return try { ControlScheme.valueOf(name) } catch (_: Exception) { ControlScheme.NFS }
        }
        set(value) = prefs.edit().putString("control_scheme", value.name).apply()

    var perspectiveEnabled: Boolean
        get() = prefs.getBoolean("perspective_enabled", true)
        set(value) = prefs.edit().putBoolean("perspective_enabled", value).apply()

    var orientationPortrait: Boolean
        get() = prefs.getBoolean("orientation_portrait", true)
        set(value) = prefs.edit().putBoolean("orientation_portrait", value).apply()

    fun isCarUnlocked(carId: String): Boolean {
        if (carId == "starter") return true
        return prefs.getBoolean("car_unlocked_$carId", false)
    }

    fun unlockCar(carId: String) {
        prefs.edit().putBoolean("car_unlocked_$carId", true).apply()
    }

    fun getCarUpgradeLevel(carId: String, stat: String): Int {
        return prefs.getInt("upgrade_${carId}_$stat", 0)
    }

    fun setCarUpgradeLevel(carId: String, stat: String, level: Int) {
        prefs.edit().putInt("upgrade_${carId}_$stat", level).apply()
    }

    fun getUpgradeCost(level: Int): Long {
        return (500L * (level + 1) * (level + 1))
    }

    fun isMissionCompleted(missionId: String): Boolean {
        return prefs.getBoolean("mission_done_$missionId", false)
    }

    fun completeMission(missionId: String) {
        prefs.edit().putBoolean("mission_done_$missionId", true).apply()
    }

    fun isAchievementUnlocked(achievementId: String): Boolean {
        return prefs.getBoolean("achievement_$achievementId", false)
    }

    fun unlockAchievement(achievementId: String) {
        prefs.edit().putBoolean("achievement_$achievementId", true).apply()
    }

    fun getUnlockedCarCount(): Int {
        return PlayerCarDef.ALL_CARS.count { isCarUnlocked(it.id) }
    }

    fun getSelectedCar(): PlayerCarDef {
        val id = selectedCarId
        return PlayerCarDef.ALL_CARS.find { it.id == id } ?: PlayerCarDef.ALL_CARS[0]
    }

    fun addCoins(amount: Long) {
        totalCoins += amount
        totalCoinsCollected += amount
    }

    fun spendCoins(amount: Long): Boolean {
        if (totalCoins >= amount) {
            totalCoins -= amount
            return true
        }
        return false
    }

    fun onGameEnd(score: Long, distance: Float, coinsEarned: Long, maxCombo: Int, maxSpeed: Float, distNoCrash: Float) {
        totalGames++
        totalDistance += distance.toLong()
        if (score > highScore) highScore = score
        if (maxCombo > bestCombo) bestCombo = maxCombo
        if (maxSpeed > maxSpeedEver) maxSpeedEver = maxSpeed
        if (distNoCrash.toLong() > bestDistanceWithoutCrash) bestDistanceWithoutCrash = distNoCrash.toLong()
        addCoins(coinsEarned)
    }

    fun checkAndUnlockAchievements(): List<AchievementDef> {
        val newlyUnlocked = mutableListOf<AchievementDef>()
        for (achievement in Achievements.ALL) {
            if (isAchievementUnlocked(achievement.id)) continue
            val met = when (achievement.id) {
                "speed_demon" -> maxSpeedEver >= achievement.requirement
                "coin_master" -> totalCoinsCollected >= achievement.requirement
                "survivor" -> totalDistance >= achievement.requirement
                "combo_king" -> bestCombo >= achievement.requirement
                "collector" -> getUnlockedCarCount().toLong() >= achievement.requirement
                "high_roller" -> highScore >= achievement.requirement
                "road_warrior" -> totalGames.toLong() >= achievement.requirement
                "untouchable" -> bestDistanceWithoutCrash >= achievement.requirement
                else -> false
            }
            if (met) {
                unlockAchievement(achievement.id)
                newlyUnlocked.add(achievement)
            }
        }
        return newlyUnlocked
    }
}
