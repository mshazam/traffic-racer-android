package com.trafficracer.game

import android.graphics.RectF

data class PlayerCar(
    var laneIndex: Int = 1,
    var targetLaneIndex: Int = 1,
    var x: Float = 0f,
    var y: Float = 0f,
    var width: Float = 0f,
    var height: Float = 0f,
    var speed: Float = Constants.INITIAL_SPEED,
    var lives: Int = Constants.INITIAL_LIVES,
    var score: Long = 0,
    var distanceScore: Float = 0f,
    var coinScore: Long = 0,
    var isBoosting: Boolean = false,
    var boostEndTime: Long = 0,
    var hasShield: Boolean = false,
    var shieldEndTime: Long = 0,
    var hasMagnet: Boolean = false,
    var magnetEndTime: Long = 0,
    var hasDoubleScore: Boolean = false,
    var doubleScoreEndTime: Long = 0,
    var isInvincible: Boolean = false,
    var invincibleEndTime: Long = 0,
    var comboCount: Int = 0,
    var lastComboTime: Long = 0,
    var laneTransition: Float = 0f
) {
    fun getRect(): RectF = RectF(x - width / 2, y - height / 2, x + width / 2, y + height / 2)

    fun getEffectiveSpeed(): Float {
        val base = speed
        return if (isBoosting) base * Constants.BOOST_SPEED_MULT else base
    }
}

data class TrafficCar(
    var x: Float = 0f,
    var y: Float = 0f,
    var width: Float = 0f,
    var height: Float = 0f,
    var speed: Float = 0f,
    var laneIndex: Int = 0,
    var type: CarType = CarType.SEDAN,
    var color: Int = 0,
    var scored: Boolean = false
) {
    fun getRect(): RectF = RectF(x - width / 2, y - height / 2, x + width / 2, y + height / 2)
}

data class Coin(
    var x: Float = 0f,
    var y: Float = 0f,
    var size: Float = 0f,
    var laneIndex: Int = 0,
    var rotation: Float = 0f,
    var collected: Boolean = false,
    var collectAnimProgress: Float = 0f
)

data class PowerUp(
    var x: Float = 0f,
    var y: Float = 0f,
    var size: Float = 0f,
    var laneIndex: Int = 0,
    var type: PowerUpType = PowerUpType.SHIELD,
    var rotation: Float = 0f,
    var pulsePhase: Float = 0f
) {
    fun getRect(): RectF = RectF(x - size / 2, y - size / 2, x + size / 2, y + size / 2)
}

data class Particle(
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var size: Float = 0f,
    var color: Int = 0,
    var alpha: Float = 1f,
    var lifetime: Float = 0f,
    var maxLifetime: Float = Constants.PARTICLE_LIFETIME_MS.toFloat(),
    var gravity: Float = 0f,
    var shrink: Boolean = true
)

data class RoadMarking(
    var y: Float = 0f
)

data class SceneryObject(
    var x: Float = 0f,
    var y: Float = 0f,
    var type: Int = 0,
    var scale: Float = 1f
)
