package com.trafficracer.game

object Constants {
    const val TARGET_FPS = 60
    const val FRAME_PERIOD = 1000L / TARGET_FPS

    const val NUM_LANES = 4
    const val ROAD_WIDTH_RATIO = 0.70f
    const val SHOULDER_WIDTH_RATIO = 0.04f

    const val PLAYER_WIDTH_RATIO = 0.13f
    const val PLAYER_HEIGHT_RATIO = 0.08f

    const val INITIAL_SPEED = 8f
    const val MAX_SPEED = 30f
    const val SPEED_INCREMENT = 0.003f
    const val BRAKE_FACTOR = 0.5f
    const val BOOST_SPEED_MULT = 1.8f
    const val BOOST_DURATION_MS = 3000L

    const val LANE_SWITCH_SPEED = 0.15f

    const val TRAFFIC_SPAWN_INTERVAL_MS = 800L
    const val TRAFFIC_MIN_SPAWN_MS = 300L
    const val TRAFFIC_SPEED_VARIANCE = 0.4f

    const val COIN_SPAWN_CHANCE = 0.35f
    const val COIN_VALUE = 100
    const val COIN_SIZE_RATIO = 0.04f
    const val COIN_ROTATION_SPEED = 6f

    const val POWERUP_SPAWN_CHANCE = 0.08f
    const val SHIELD_DURATION_MS = 5000L
    const val MAGNET_DURATION_MS = 7000L
    const val MAGNET_RANGE_RATIO = 0.25f
    const val DOUBLE_SCORE_DURATION_MS = 8000L

    const val INITIAL_LIVES = 3
    const val MAX_LIVES = 5
    const val INVINCIBILITY_AFTER_HIT_MS = 2000L

    const val NEAR_MISS_DISTANCE_RATIO = 0.02f
    const val NEAR_MISS_BONUS = 50
    const val COMBO_MULTIPLIER = 0.5f
    const val MAX_COMBO = 10

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

    const val NIGHT_MODE_SCORE_THRESHOLD = 5000
    const val NIGHT_TRANSITION_SPEED = 0.002f

    const val SCREEN_SHAKE_INTENSITY = 12f
    const val SCREEN_SHAKE_DURATION_MS = 300L
}

enum class GameState {
    START_SCREEN,
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
    NITRO(Constants.BOOST_DURATION_MS, 0xFF2979FF.toInt())
}
