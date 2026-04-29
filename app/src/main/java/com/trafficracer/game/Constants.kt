package com.trafficracer.game

object Constants {
    const val TARGET_FPS = 60
    const val FRAME_PERIOD = 1000L / TARGET_FPS

    const val ROAD_WIDTH_RATIO = 0.72f
    const val SHOULDER_WIDTH_RATIO = 0.04f
    const val NUM_LANES = 4

    const val PLAYER_WIDTH_RATIO = 0.11f
    const val PLAYER_HEIGHT_RATIO = 0.065f

    // NFS-style speed physics
    const val INITIAL_SPEED = 5f
    const val MAX_SPEED = 35f
    const val IDLE_SPEED = 3f
    const val ACCEL_FORCE = 0.18f
    const val BRAKE_FORCE = 0.30f
    const val COAST_DECEL = 0.04f
    const val ENGINE_BRAKE = 0.06f

    // NFS-style steering
    const val STEER_SPEED = 8f
    const val STEER_RETURN_SPEED = 12f
    const val MAX_STEER_ANGLE = 1.0f
    const val STEER_TO_MOVEMENT = 6.5f
    const val DRIFT_FACTOR = 0.85f
    const val DRIFT_THRESHOLD_SPEED = 15f
    const val DRIFT_STEER_THRESHOLD = 0.4f
    const val HIGH_SPEED_STEER_REDUCTION = 0.6f

    // NOS system
    const val NOS_MAX = 100f
    const val NOS_FILL_RATE = 3f
    const val NOS_DRAIN_RATE = 25f
    const val NOS_SPEED_MULT = 1.7f
    const val NOS_MIN_TO_ACTIVATE = 20f
    const val NOS_FILL_ON_NEAR_MISS = 8f
    const val NOS_FILL_ON_OVERTAKE = 5f

    const val TRAFFIC_SPAWN_INTERVAL_MS = 700L
    const val TRAFFIC_MIN_SPAWN_MS = 250L
    const val TRAFFIC_SPEED_VARIANCE = 0.4f
    const val ONCOMING_TRAFFIC_CHANCE = 0.12f
    const val ONCOMING_SPEED_MULT = 1.8f

    const val COIN_SPAWN_CHANCE = 0.35f
    const val COIN_VALUE = 100
    const val COIN_SIZE_RATIO = 0.035f
    const val COIN_ROTATION_SPEED = 6f

    const val POWERUP_SPAWN_CHANCE = 0.07f
    const val SHIELD_DURATION_MS = 5000L
    const val MAGNET_DURATION_MS = 7000L
    const val MAGNET_RANGE_RATIO = 0.25f
    const val DOUBLE_SCORE_DURATION_MS = 8000L

    const val INITIAL_LIVES = 3
    const val MAX_LIVES = 5
    const val INVINCIBILITY_AFTER_HIT_MS = 2000L

    const val NEAR_MISS_DISTANCE_RATIO = 0.025f
    const val NEAR_MISS_BONUS = 50
    const val COMBO_MULTIPLIER = 0.5f
    const val MAX_COMBO = 10
    const val SLOW_MO_DURATION_MS = 400L
    const val SLOW_MO_FACTOR = 0.35f

    const val OVERTAKE_BONUS = 75
    const val OVERTAKE_SPEED_THRESHOLD = 1.2f

    const val PARTICLE_COUNT_CRASH = 40
    const val PARTICLE_COUNT_COIN = 15
    const val PARTICLE_COUNT_POWERUP = 20
    const val PARTICLE_LIFETIME_MS = 800L

    const val ROAD_MARKING_LENGTH_RATIO = 0.04f
    const val ROAD_MARKING_GAP_RATIO = 0.04f
    const val ROAD_MARKING_WIDTH_RATIO = 0.005f

    const val TREE_WIDTH_RATIO = 0.06f
    const val TREE_SPACING_RATIO = 0.15f

    const val DISTANCE_SCORE_FACTOR = 0.1f

    const val SCREEN_SHAKE_INTENSITY = 12f
    const val SCREEN_SHAKE_DURATION_MS = 300L

    const val HAZARD_SPAWN_CHANCE = 0.05f
    const val HAZARD_SIZE_RATIO = 0.045f
    const val OIL_SLICK_DURATION_MS = 1500L

    const val MYSTERY_BOX_SPAWN_CHANCE = 0.025f
    const val MYSTERY_BOX_SIZE_RATIO = 0.055f

    const val FLOATING_TEXT_DURATION_MS = 1200L
    const val FLOATING_TEXT_RISE_SPEED = 2.5f

    const val WEATHER_PARTICLE_COUNT = 80
    const val WEATHER_PARTICLE_SPEED = 15f

    const val ENV_CHANGE_DISTANCE = 2500f
    const val MILESTONE_INTERVAL = 1000L

    const val CAMERA_ZOOM_BOOST = 1.04f
    const val CAMERA_ZOOM_SPEED = 0.06f
    const val SPEED_BLUR_THRESHOLD = 20f

    // HUD layout
    const val HUD_CONTROLS_HEIGHT_RATIO = 0.28f
    const val PEDAL_WIDTH_RATIO = 0.18f
    const val STEERING_ZONE_WIDTH_RATIO = 0.40f
    const val NOS_BUTTON_SIZE_RATIO = 0.10f
    const val SPEEDO_SIZE_RATIO = 0.13f
}

enum class GameState {
    START_SCREEN,
    GARAGE,
    MISSIONS_SCREEN,
    PLAYING,
    PAUSED,
    GAME_OVER
}

enum class CarType(val widthMult: Float, val heightMult: Float, val speedMult: Float) {
    SEDAN(1.0f, 1.0f, 1.0f),
    SUV(1.1f, 1.15f, 0.9f),
    TRUCK(1.15f, 1.6f, 0.7f),
    SPORTS(0.95f, 0.95f, 1.3f),
    BUS(1.2f, 2.0f, 0.6f)
}

enum class PowerUpType(val durationMs: Long, val color: Int) {
    SHIELD(Constants.SHIELD_DURATION_MS, 0xFF00E5FF.toInt()),
    MAGNET(Constants.MAGNET_DURATION_MS, 0xFFFF6F00.toInt()),
    DOUBLE_SCORE(Constants.DOUBLE_SCORE_DURATION_MS, 0xFFFFD700.toInt()),
    EXTRA_LIFE(0L, 0xFFE91E63.toInt()),
    NITRO(Constants.NOS_MAX.toLong(), 0xFF2979FF.toInt())
}

enum class Environment(
    val displayName: String,
    val skyTopDay: Int, val skyBottomDay: Int,
    val skyTopNight: Int, val skyBottomNight: Int,
    val grassColor: Int, val grassLight: Int,
    val roadColor: Int, val shoulderColor: Int,
    val sceneryType: Int,
    val hasWeather: Boolean,
    val weatherType: WeatherType
) {
    CITY("City",
        0xFF87CEEB.toInt(), 0xFFB0E2FF.toInt(), 0xFF0A0A28.toInt(), 0xFF191950.toInt(),
        0xFF2E7D32.toInt(), 0xFF388E3C.toInt(), 0xFF424242.toInt(), 0xFF616161.toInt(),
        0, false, WeatherType.NONE),
    HIGHWAY("Highway",
        0xFF64B5F6.toInt(), 0xFFBBDEFB.toInt(), 0xFF0D1B2A.toInt(), 0xFF1B2838.toInt(),
        0xFF33691E.toInt(), 0xFF558B2F.toInt(), 0xFF37474F.toInt(), 0xFF546E7A.toInt(),
        1, false, WeatherType.NONE),
    DESERT("Desert",
        0xFFFFF176.toInt(), 0xFFFFCC80.toInt(), 0xFF1A0A2E.toInt(), 0xFF2D1B4E.toInt(),
        0xFFD4A44A.toInt(), 0xFFC4943A.toInt(), 0xFF5D4037.toInt(), 0xFF795548.toInt(),
        2, false, WeatherType.NONE),
    SNOW("Snow",
        0xFFB0BEC5.toInt(), 0xFFCFD8DC.toInt(), 0xFF1A1A2E.toInt(), 0xFF16213E.toInt(),
        0xFFECEFF1.toInt(), 0xFFFFFFFF.toInt(), 0xFF78909C.toInt(), 0xFF90A4AE.toInt(),
        3, true, WeatherType.SNOW),
    RAINY("Rainy",
        0xFF546E7A.toInt(), 0xFF78909C.toInt(), 0xFF0A0F14.toInt(), 0xFF1A2530.toInt(),
        0xFF1B5E20.toInt(), 0xFF2E7D32.toInt(), 0xFF263238.toInt(), 0xFF37474F.toInt(),
        0, true, WeatherType.RAIN)
}

enum class WeatherType { NONE, RAIN, SNOW }

enum class HazardType(val slowFactor: Float, val durationMs: Long) {
    OIL_SLICK(0.5f, Constants.OIL_SLICK_DURATION_MS),
    CONE(0f, 0L),
    POTHOLE(0.7f, 800L)
}

data class PlayerCarDef(
    val id: String, val name: String,
    val color: Int, val accentColor: Int,
    val baseSpeed: Float, val baseHandling: Float, val baseNitro: Float,
    val price: Long, val description: String
) {
    companion object {
        val ALL_CARS = listOf(
            PlayerCarDef("starter", "Street Runner", 0xFFE53935.toInt(), 0xFFB71C1C.toInt(),
                1.0f, 1.0f, 1.0f, 0, "Your trusty starter car"),
            PlayerCarDef("blue_bolt", "Blue Bolt", 0xFF1E88E5.toInt(), 0xFF0D47A1.toInt(),
                1.1f, 1.05f, 1.0f, 2000, "Quick and nimble city cruiser"),
            PlayerCarDef("viper", "Green Viper", 0xFF43A047.toInt(), 0xFF1B5E20.toInt(),
                1.15f, 1.1f, 1.1f, 5000, "Venomous speed on the highway"),
            PlayerCarDef("phantom", "Purple Phantom", 0xFF7B1FA2.toInt(), 0xFF4A148C.toInt(),
                1.2f, 1.15f, 1.15f, 10000, "Ghostly fast, barely visible"),
            PlayerCarDef("golden_fury", "Golden Fury", 0xFFFFA000.toInt(), 0xFFFF6F00.toInt(),
                1.25f, 1.2f, 1.2f, 20000, "Pure gold, pure power"),
            PlayerCarDef("shadow", "Shadow X", 0xFF212121.toInt(), 0xFF000000.toInt(),
                1.3f, 1.25f, 1.3f, 35000, "The ultimate racing machine"),
            PlayerCarDef("inferno", "Inferno", 0xFFFF3D00.toInt(), 0xFFDD2C00.toInt(),
                1.35f, 1.1f, 1.5f, 50000, "Burns everything in its wake"),
            PlayerCarDef("arctic", "Arctic Storm", 0xFF00BCD4.toInt(), 0xFF006064.toInt(),
                1.2f, 1.4f, 1.2f, 50000, "Ice cold handling perfection")
        )
    }
}

data class MissionDef(val id: String, val description: String, val target: Int, val reward: Long, val type: MissionType)
enum class MissionType { COLLECT_COINS, NEAR_MISSES, REACH_SPEED, TRAVEL_DISTANCE, COLLECT_POWERUPS, DESTROY_HAZARDS, REACH_COMBO }

data class AchievementDef(val id: String, val name: String, val description: String, val icon: String, val requirement: Long)

object Achievements {
    val ALL = listOf(
        AchievementDef("speed_demon", "Speed Demon", "Reach 350 km/h", "⚡", 350),
        AchievementDef("coin_master", "Coin Master", "Collect 10,000 total coins", "$", 10000),
        AchievementDef("survivor", "Survivor", "Travel 50,000m in total", "🛡", 50000),
        AchievementDef("combo_king", "Combo King", "Get a 10x combo", "🔥", 10),
        AchievementDef("collector", "Collector", "Unlock all cars", "🏆", PlayerCarDef.ALL_CARS.size.toLong()),
        AchievementDef("high_roller", "High Roller", "Score 100,000 in one run", "⭐", 100000),
        AchievementDef("road_warrior", "Road Warrior", "Play 100 games", "🎮", 100),
        AchievementDef("untouchable", "Untouchable", "Travel 5,000m without a crash", "💎", 5000)
    )
}

object MissionBank {
    fun generateMissions(difficulty: Int): List<MissionDef> {
        val mult = 1 + difficulty * 0.5f
        return listOf(
            MissionDef("coins_$difficulty", "Collect ${(15 * mult).toInt()} coins", (15 * mult).toInt(), (300 * mult).toLong(), MissionType.COLLECT_COINS),
            MissionDef("nearmiss_$difficulty", "Get ${(5 * mult).toInt()} near misses", (5 * mult).toInt(), (400 * mult).toLong(), MissionType.NEAR_MISSES),
            MissionDef("speed_$difficulty", "Reach ${(200 + difficulty * 25)} km/h", 200 + difficulty * 25, (500 * mult).toLong(), MissionType.REACH_SPEED),
            MissionDef("dist_$difficulty", "Travel ${(1000 * mult).toInt()}m", (1000 * mult).toInt(), (350 * mult).toLong(), MissionType.TRAVEL_DISTANCE),
            MissionDef("combo_$difficulty", "Get a ${3 + difficulty}x combo", 3 + difficulty, (600 * mult).toLong(), MissionType.REACH_COMBO),
            MissionDef("powerup_$difficulty", "Collect ${(3 * mult).toInt()} power-ups", (3 * mult).toInt(), (250 * mult).toLong(), MissionType.COLLECT_POWERUPS)
        )
    }
}
