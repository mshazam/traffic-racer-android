package com.trafficracer.game

object Constants {
    // Road
    const val NUM_LANES = 4
    const val ROAD_WIDTH = 14f
    const val LANE_WIDTH = ROAD_WIDTH / NUM_LANES
    const val ROAD_LENGTH = 500f
    const val ROAD_SEGMENT_LENGTH = 50f

    // Player physics
    const val INITIAL_SPEED = 15f
    const val MAX_SPEED = 120f
    const val IDLE_SPEED = 10f
    const val ACCEL_FORCE = 0.3f
    const val BRAKE_FORCE = 1.5f
    const val COAST_DECEL = 0.1f
    const val STEER_SPEED = 8f
    const val MAX_STEER = 0.5f
    const val NOS_SPEED_BOOST = 40f
    const val NOS_DRAIN_RATE = 15f
    const val NOS_CHARGE_RATE = 3f

    // Traffic
    const val TRAFFIC_SPAWN_DISTANCE = 200f
    const val TRAFFIC_DESPAWN_DISTANCE = 30f
    const val TRAFFIC_MIN_SPEED = 8f
    const val TRAFFIC_MAX_SPEED = 18f
    const val TRAFFIC_SPAWN_INTERVAL = 0.8f
    const val LANE_CHANGE_CHANCE = 0.005f
    const val LANE_CHANGE_SPEED = 3f
    const val PLAYER_LANE_BIAS = 0.6f

    // Camera
    const val CAM_HEIGHT = 5f
    const val CAM_DISTANCE = 12f
    const val CAM_LOOK_AHEAD = 8f

    // Scoring
    const val SCORE_PER_SECOND = 10
    const val NEAR_MISS_DISTANCE = 2.5f
    const val NEAR_MISS_BONUS = 50
    const val OVERTAKE_BONUS = 25
    const val COIN_VALUE = 10

    // Crash
    const val CRASH_SPEED_PENALTY = 0.15f
    const val CRASH_FREEZE_MS = 600
    const val INVINCIBILITY_MS = 2500
    const val PLAYER_LIVES = 3

    // Environment
    const val ENV_CHANGE_DISTANCE = 2000f

    // Car dimensions (approximate for collision)
    const val CAR_HALF_WIDTH = 0.9f
    const val CAR_HALF_LENGTH = 2.2f
    const val TRUCK_HALF_LENGTH = 3.5f
}
