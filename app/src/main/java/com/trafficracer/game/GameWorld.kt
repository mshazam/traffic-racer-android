package com.trafficracer.game

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class GameWorld(private val prefs: GamePrefs) {

    // Player state
    var playerX = 0f
    var playerZ = 0f // always 0 in world coords; camera follows
    var playerSpeed = Constants.INITIAL_SPEED
    var steerInput = 0f
    var isAccelerating = false
    var isBraking = false
    var nosActive = false
    var nosAmount = 100f
    var lives = Constants.PLAYER_LIVES
    var score = 0
    var coins = 0
    var distance = 0f
    var comboCount = 0
    var comboTimer = 0f
    var isInvincible = false
    var invincibleTimer = 0f
    var crashFreezeTimer = 0f
    var gameOver = false
    var selectedCarDef = PlayerCarDef.ALL.find { it.id == prefs.selectedCar } ?: PlayerCarDef.ALL[0]

    // Control scheme: 0=NFS, 1=Simple(tilt), 2=Arcade(buttons)
    var controlScheme = prefs.controlScheme

    // Traffic
    data class TrafficCar(
        var x: Float,
        var z: Float,
        var speed: Float,
        var laneIndex: Int,
        var defIndex: Int,
        var targetLane: Int = -1,
        var laneChangeProgress: Float = 0f,
        var nextLaneChangeTime: Float = 0f,
        var passed: Boolean = false
    )

    val trafficCars = mutableListOf<TrafficCar>()
    private var trafficSpawnTimer = 0f
    private var distanceSinceEnvChange = 0f

    // Environment
    var envIndex = 0
    val environments = listOf(
        "City", "Desert", "Snow", "Night", "Forest"
    )

    // Coins on road
    data class Coin(var x: Float, var z: Float, var collected: Boolean = false)
    val roadCoins = mutableListOf<Coin>()
    private var coinSpawnTimer = 0f

    // Floating texts for HUD
    data class FloatingText(var text: String, var timer: Float, var x: Float, var y: Float)
    val floatingTexts = mutableListOf<FloatingText>()

    // Screen shake
    var shakeTimer = 0f
    var shakeIntensity = 0f

    fun reset() {
        playerX = 0f
        playerSpeed = Constants.INITIAL_SPEED
        steerInput = 0f
        isAccelerating = false
        isBraking = false
        nosActive = false
        nosAmount = 100f
        lives = Constants.PLAYER_LIVES
        score = 0
        coins = 0
        distance = 0f
        comboCount = 0
        comboTimer = 0f
        isInvincible = false
        invincibleTimer = 0f
        crashFreezeTimer = 0f
        gameOver = false
        trafficCars.clear()
        roadCoins.clear()
        floatingTexts.clear()
        trafficSpawnTimer = 0f
        distanceSinceEnvChange = 0f
        envIndex = 0
        selectedCarDef = PlayerCarDef.ALL.find { it.id == prefs.selectedCar } ?: PlayerCarDef.ALL[0]
        controlScheme = prefs.controlScheme
    }

    fun update(dt: Float) {
        if (gameOver) return

        // Crash freeze
        if (crashFreezeTimer > 0) {
            crashFreezeTimer -= dt
            updateTraffic(dt)
            updateFloatingTexts(dt)
            updateShake(dt)
            return
        }

        // Invincibility
        if (isInvincible) {
            invincibleTimer -= dt
            if (invincibleTimer <= 0) isInvincible = false
        }

        // Speed physics
        updateSpeed(dt)

        // Steering
        val roadHalfWidth = Constants.ROAD_WIDTH / 2f - Constants.CAR_HALF_WIDTH
        playerX += steerInput * Constants.STEER_SPEED * selectedCarDef.handling * dt
        playerX = playerX.coerceIn(-roadHalfWidth, roadHalfWidth)

        // Distance & scoring
        val moved = playerSpeed * dt
        distance += moved
        distanceSinceEnvChange += moved
        score += (Constants.SCORE_PER_SECOND * dt * (1 + comboCount * 0.1f)).toInt()

        // Combo timer
        if (comboCount > 0) {
            comboTimer -= dt
            if (comboTimer <= 0) comboCount = 0
        }

        // NOS
        if (nosActive && nosAmount > 0) {
            nosAmount -= Constants.NOS_DRAIN_RATE * dt
            if (nosAmount <= 0) { nosActive = false; nosAmount = 0f }
        } else if (!nosActive && nosAmount < 100f) {
            nosAmount = min(100f, nosAmount + Constants.NOS_CHARGE_RATE * dt)
        }

        // Environment cycling
        if (distanceSinceEnvChange > Constants.ENV_CHANGE_DISTANCE) {
            distanceSinceEnvChange = 0f
            envIndex = (envIndex + 1) % environments.size
        }

        // Traffic
        updateTraffic(dt)
        checkCollisions()

        // Coins
        updateCoins(dt, moved)

        // Floating texts & shake
        updateFloatingTexts(dt)
        updateShake(dt)
    }

    private fun updateSpeed(dt: Float) {
        val speedMul = selectedCarDef.baseSpeed
        val nosBoost = if (nosActive) Constants.NOS_SPEED_BOOST else 0f
        val maxSpeed = Constants.MAX_SPEED * speedMul + nosBoost

        if (isBraking) {
            playerSpeed -= Constants.BRAKE_FORCE * dt * 60f
        } else if (isAccelerating || controlScheme != 0) {
            // Non-NFS schemes auto-accelerate
            val accel = Constants.ACCEL_FORCE * speedMul
            playerSpeed += accel * dt * 60f
        } else {
            playerSpeed -= Constants.COAST_DECEL * dt * 60f
        }
        playerSpeed = playerSpeed.coerceIn(Constants.IDLE_SPEED, maxSpeed)
    }

    private fun updateTraffic(dt: Float) {
        // Spawn
        trafficSpawnTimer -= dt
        if (trafficSpawnTimer <= 0) {
            trafficSpawnTimer = Constants.TRAFFIC_SPAWN_INTERVAL * (0.7f + Random.nextFloat() * 0.6f)
            spawnTraffic()
        }

        // Update positions & lane changes
        val roadHalfWidth = Constants.ROAD_WIDTH / 2f
        val playerLane = getLaneIndex(playerX)

        for (car in trafficCars) {
            val relSpeed = playerSpeed - car.speed
            car.z -= relSpeed * dt

            // Lane changing AI
            if (car.targetLane < 0 && car.nextLaneChangeTime <= 0f) {
                if (Random.nextFloat() < Constants.LANE_CHANGE_CHANCE) {
                    val possibleLanes = mutableListOf<Int>()
                    if (car.laneIndex > 0) possibleLanes.add(car.laneIndex - 1)
                    if (car.laneIndex < Constants.NUM_LANES - 1) possibleLanes.add(car.laneIndex + 1)
                    val target = if (possibleLanes.contains(playerLane) && Random.nextFloat() < Constants.PLAYER_LANE_BIAS) {
                        playerLane
                    } else {
                        possibleLanes.randomOrNull() ?: car.laneIndex
                    }
                    if (target != car.laneIndex) {
                        car.targetLane = target
                        car.laneChangeProgress = 0f
                    }
                }
            } else if (car.nextLaneChangeTime > 0f) {
                car.nextLaneChangeTime -= dt
            }

            if (car.targetLane >= 0) {
                car.laneChangeProgress += dt * Constants.LANE_CHANGE_SPEED
                val targetX = getLaneCenter(car.targetLane)
                car.x += (targetX - car.x) * min(car.laneChangeProgress, 1f) * dt * 5f
                if (car.laneChangeProgress >= 1f || abs(car.x - targetX) < 0.1f) {
                    car.x = targetX
                    car.laneIndex = car.targetLane
                    car.targetLane = -1
                    car.laneChangeProgress = 0f
                    car.nextLaneChangeTime = 3f + Random.nextFloat() * 5f
                }
            }

            // Near miss & overtake detection
            if (!car.passed && car.z < -Constants.CAR_HALF_LENGTH * 2) {
                car.passed = true
                val lateralDist = abs(car.x - playerX)
                if (lateralDist < Constants.NEAR_MISS_DISTANCE) {
                    score += Constants.NEAR_MISS_BONUS * (1 + comboCount)
                    comboCount++
                    comboTimer = 2f
                    floatingTexts.add(FloatingText("NEAR MISS x$comboCount", 1.5f, 0f, 0f))
                } else {
                    score += Constants.OVERTAKE_BONUS
                }
            }
        }

        // Remove far-away cars
        trafficCars.removeAll { it.z < -Constants.TRAFFIC_DESPAWN_DISTANCE }
    }

    private fun spawnTraffic() {
        val lane = Random.nextInt(Constants.NUM_LANES)
        val x = getLaneCenter(lane)
        val defIdx = Random.nextInt(TrafficCarDef.ALL.size)
        val def = TrafficCarDef.ALL[defIdx]
        val speed = (Constants.TRAFFIC_MIN_SPEED + Random.nextFloat() *
                (Constants.TRAFFIC_MAX_SPEED - Constants.TRAFFIC_MIN_SPEED)) * def.speedFactor
        val z = Constants.TRAFFIC_SPAWN_DISTANCE + Random.nextFloat() * 50f

        // Don't spawn on top of another car
        val tooClose = trafficCars.any { abs(it.x - x) < Constants.LANE_WIDTH * 0.5f && abs(it.z - z) < 10f }
        if (!tooClose) {
            trafficCars.add(TrafficCar(x, z, speed, lane, defIdx))
        }
    }

    private fun checkCollisions() {
        if (isInvincible) return
        for (car in trafficCars) {
            val def = TrafficCarDef.ALL[car.defIndex]
            val halfLen = if (def.isLarge) Constants.TRUCK_HALF_LENGTH else Constants.CAR_HALF_LENGTH
            if (abs(car.x - playerX) < Constants.CAR_HALF_WIDTH * 2 &&
                abs(car.z) < halfLen + Constants.CAR_HALF_LENGTH) {
                handleCrash()
                break
            }
        }
    }

    private fun handleCrash() {
        lives--
        playerSpeed = max(Constants.IDLE_SPEED, playerSpeed * Constants.CRASH_SPEED_PENALTY)
        crashFreezeTimer = Constants.CRASH_FREEZE_MS / 1000f
        isInvincible = true
        invincibleTimer = Constants.INVINCIBILITY_MS / 1000f
        comboCount = 0
        nosActive = false
        shakeTimer = 0.5f
        shakeIntensity = 15f
        floatingTexts.add(FloatingText("CRASH!", 2f, 0f, 0f))
        floatingTexts.add(FloatingText("-1 LIFE", 1.5f, 0f, 0f))

        if (lives <= 0) {
            gameOver = true
            if (score > prefs.highScore) prefs.highScore = score
            prefs.totalCoins += coins
        }
    }

    private fun updateCoins(dt: Float, moved: Float) {
        coinSpawnTimer -= dt
        if (coinSpawnTimer <= 0) {
            coinSpawnTimer = 1.5f + Random.nextFloat() * 2f
            val lane = Random.nextInt(Constants.NUM_LANES)
            roadCoins.add(Coin(getLaneCenter(lane), Constants.TRAFFIC_SPAWN_DISTANCE))
        }

        for (coin in roadCoins) {
            coin.z -= moved
            if (!coin.collected && abs(coin.x - playerX) < 1.5f && abs(coin.z) < 2f) {
                coin.collected = true
                coins += Constants.COIN_VALUE
                score += Constants.COIN_VALUE
            }
        }
        roadCoins.removeAll { it.z < -20f || it.collected }
    }

    private fun updateFloatingTexts(dt: Float) {
        floatingTexts.forEach { it.timer -= dt }
        floatingTexts.removeAll { it.timer <= 0 }
    }

    private fun updateShake(dt: Float) {
        if (shakeTimer > 0) shakeTimer -= dt
    }

    fun getLaneCenter(lane: Int): Float {
        val roadLeft = -Constants.ROAD_WIDTH / 2f
        return roadLeft + Constants.LANE_WIDTH * (lane + 0.5f)
    }

    private fun getLaneIndex(x: Float): Int {
        val roadLeft = -Constants.ROAD_WIDTH / 2f
        return ((x - roadLeft) / Constants.LANE_WIDTH).toInt().coerceIn(0, Constants.NUM_LANES - 1)
    }

    fun activateNOS() {
        if (nosAmount > 10f) nosActive = true
    }

    fun deactivateNOS() {
        nosActive = false
    }
}
