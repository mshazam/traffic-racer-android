package com.trafficracer.game

import android.graphics.RectF
import kotlin.math.abs

data class PlayerCar(
    var x: Float = 0f,
    var y: Float = 0f,
    var width: Float = 0f,
    var height: Float = 0f,
    var speed: Float = Constants.INITIAL_SPEED,
    var steerInput: Float = 0f,
    var steerAngle: Float = 0f,
    var driftAngle: Float = 0f,
    var isDrifting: Boolean = false,
    var isAccelerating: Boolean = false,
    var isBraking: Boolean = false,
    var brakeLightIntensity: Float = 0f,
    var nosAmount: Float = Constants.NOS_MAX * 0.5f,
    var nosActive: Boolean = false,
    var lives: Int = Constants.INITIAL_LIVES,
    var score: Long = 0,
    var distanceScore: Float = 0f,
    var coinScore: Long = 0,
    var coinsCollectedThisRun: Int = 0,
    var nearMissesThisRun: Int = 0,
    var overtakesThisRun: Int = 0,
    var powerUpsCollectedThisRun: Int = 0,
    var maxSpeedReachedThisRun: Float = 0f,
    var distanceWithoutCrash: Float = 0f,
    var hasShield: Boolean = false,
    var shieldEndTime: Long = 0,
    var hasMagnet: Boolean = false,
    var magnetEndTime: Long = 0,
    var hasDoubleScore: Boolean = false,
    var doubleScoreEndTime: Long = 0,
    var isInvincible: Boolean = false,
    var invincibleEndTime: Long = 0,
    var isSlipping: Boolean = false,
    var slipEndTime: Long = 0,
    var comboCount: Int = 0,
    var maxComboThisRun: Int = 0,
    var lastComboTime: Long = 0,
    var carDef: PlayerCarDef = PlayerCarDef.ALL_CARS[0],
    var crashFreezeEnd: Long = 0
) {
    fun getRect(): RectF = RectF(x - width / 2, y - height / 2, x + width / 2, y + height / 2)

    fun getEffectiveSpeed(): Float {
        var base = speed * carDef.baseSpeed
        if (nosActive && nosAmount > 0) base *= Constants.NOS_SPEED_MULT * carDef.baseNitro
        if (isSlipping) base *= 0.6f
        return base
    }

    fun getEffectiveHandling(): Float {
        var h = carDef.baseHandling
        if (isSlipping) h *= 0.4f
        val speedFactor = if (speed > Constants.DRIFT_THRESHOLD_SPEED) {
            1f - (speed - Constants.DRIFT_THRESHOLD_SPEED) / (Constants.MAX_SPEED - Constants.DRIFT_THRESHOLD_SPEED) * (1f - Constants.HIGH_SPEED_STEER_REDUCTION)
        } else 1f
        return h * speedFactor
    }

    fun getSpeedKmh(): Int = (getEffectiveSpeed() * 12f).toInt()

    fun isDriftingNow(): Boolean =
        speed > Constants.DRIFT_THRESHOLD_SPEED && abs(steerAngle) > Constants.DRIFT_STEER_THRESHOLD
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
    var nearMissScored: Boolean = false,
    var overtakeScored: Boolean = false,
    var isOncoming: Boolean = false,
    var spriteIndex: Int = 0,
    var targetLane: Int = -1,
    var laneChangeProgress: Float = 0f,
    var nextLaneChangeTime: Long = 0
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

data class RoadMarking(var y: Float = 0f)

data class SceneryObject(var x: Float = 0f, var y: Float = 0f, var type: Int = 0, var scale: Float = 1f)

data class FloatingText(
    var x: Float = 0f, var y: Float = 0f,
    var text: String = "", var color: Int = 0xFFFFFFFF.toInt(),
    var startTime: Long = 0, var duration: Long = Constants.FLOATING_TEXT_DURATION_MS,
    var scale: Float = 1f
) {
    fun getProgress(now: Long): Float = ((now - startTime).toFloat() / duration).coerceIn(0f, 1f)
    fun isExpired(now: Long): Boolean = now - startTime > duration
}

data class RoadHazard(
    var x: Float = 0f, var y: Float = 0f, var size: Float = 0f,
    var laneIndex: Int = 0, var type: HazardType = HazardType.OIL_SLICK, var rotation: Float = 0f
) {
    fun getRect(): RectF = RectF(x - size / 2, y - size / 2, x + size / 2, y + size / 2)
}

data class MysteryBox(
    var x: Float = 0f, var y: Float = 0f, var size: Float = 0f,
    var laneIndex: Int = 0, var rotation: Float = 0f, var bouncePhase: Float = 0f
) {
    fun getRect(): RectF = RectF(x - size / 2, y - size / 2, x + size / 2, y + size / 2)
}

data class WeatherParticle(
    var x: Float = 0f, var y: Float = 0f, var speed: Float = 0f,
    var size: Float = 0f, var alpha: Float = 1f, var windOffset: Float = 0f
)

data class MilestoneEvent(var distance: Long = 0, var startTime: Long = 0, var duration: Long = 2000L) {
    fun getProgress(now: Long): Float = ((now - startTime).toFloat() / duration).coerceIn(0f, 1f)
    fun isExpired(now: Long): Boolean = now - startTime > duration
}

data class TireTrack(var x: Float = 0f, var y: Float = 0f, var alpha: Float = 1f, var width: Float = 0f)

data class DriftSpark(
    var x: Float = 0f, var y: Float = 0f,
    var vx: Float = 0f, var vy: Float = 0f,
    var life: Float = 1f, var color: Int = 0xFFFFAB00.toInt()
)
