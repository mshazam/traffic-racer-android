package com.trafficracer.game

import android.content.Context
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class GameWorld(private val context: Context) {
    var screenWidth: Float = 0f
    var screenHeight: Float = 0f
    var roadLeft: Float = 0f
    var roadRight: Float = 0f
    var roadWidth: Float = 0f
    var laneWidth: Float = 0f
    var shoulderWidth: Float = 0f

    var state: GameState = GameState.START_SCREEN
    var player = PlayerCar()
    val trafficCars = mutableListOf<TrafficCar>()
    val coins = mutableListOf<Coin>()
    val powerUps = mutableListOf<PowerUp>()
    val particles = mutableListOf<Particle>()
    val roadMarkings = mutableListOf<RoadMarking>()
    val sceneryObjects = mutableListOf<SceneryObject>()

    var highScore: Long = 0L
    var nightFactor: Float = 0f
    var screenShakeX: Float = 0f
    var screenShakeY: Float = 0f
    private var screenShakeEndTime: Long = 0
    private var lastTrafficSpawn: Long = 0
    private var markingOffset: Float = 0f
    private var sceneryOffset: Float = 0f

    private val highScoreManager by lazy { HighScoreManager(context) }
    private var soundManager: SoundManager? = null

    private val trafficColors = intArrayOf(
        0xFF1565C0.toInt(), 0xFFC62828.toInt(), 0xFF2E7D32.toInt(),
        0xFFF9A825.toInt(), 0xFF6A1B9A.toInt(), 0xFFEF6C00.toInt(),
        0xFF00838F.toInt(), 0xFF4E342E.toInt(), 0xFF37474F.toInt(),
        0xFFAD1457.toInt(), 0xFF283593.toInt(), 0xFF558B2F.toInt()
    )

    fun init(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height
        roadWidth = width * Constants.ROAD_WIDTH_RATIO
        shoulderWidth = width * Constants.SHOULDER_WIDTH_RATIO
        roadLeft = (width - roadWidth) / 2f
        roadRight = roadLeft + roadWidth
        laneWidth = roadWidth / Constants.NUM_LANES

        highScore = highScoreManager.getHighScore()

        soundManager = SoundManager(context)

        initRoadMarkings()
        initScenery()
    }

    private fun initRoadMarkings() {
        roadMarkings.clear()
        val markingHeight = screenHeight * Constants.ROAD_MARKING_LENGTH_RATIO
        val gap = screenHeight * Constants.ROAD_MARKING_GAP_RATIO
        val totalSpacing = markingHeight + gap
        var y = -totalSpacing
        while (y < screenHeight + totalSpacing) {
            roadMarkings.add(RoadMarking(y))
            y += totalSpacing
        }
    }

    private fun initScenery() {
        sceneryObjects.clear()
        val spacing = screenHeight * Constants.TREE_SPACING_RATIO
        var y = 0f
        while (y < screenHeight + spacing) {
            sceneryObjects.add(SceneryObject(
                x = roadLeft - screenWidth * 0.08f,
                y = y,
                type = Random.nextInt(3),
                scale = 0.8f + Random.nextFloat() * 0.4f
            ))
            sceneryObjects.add(SceneryObject(
                x = roadRight + screenWidth * 0.08f,
                y = y + spacing * 0.5f,
                type = Random.nextInt(3),
                scale = 0.8f + Random.nextFloat() * 0.4f
            ))
            y += spacing
        }
    }

    fun startGame() {
        state = GameState.PLAYING
        player = PlayerCar(
            laneIndex = Constants.NUM_LANES / 2,
            targetLaneIndex = Constants.NUM_LANES / 2,
            width = screenWidth * Constants.PLAYER_WIDTH_RATIO,
            height = screenHeight * Constants.PLAYER_HEIGHT_RATIO
        )
        player.x = getLaneCenter(player.laneIndex)
        player.y = screenHeight * 0.78f

        trafficCars.clear()
        coins.clear()
        powerUps.clear()
        particles.clear()
        nightFactor = 0f
        lastTrafficSpawn = 0
        markingOffset = 0f

        initRoadMarkings()
        initScenery()
    }

    fun getLaneCenter(lane: Int): Float {
        return roadLeft + laneWidth * lane + laneWidth / 2f
    }

    fun update(deltaTime: Float) {
        if (state != GameState.PLAYING) return

        val now = System.currentTimeMillis()
        val effectiveSpeed = player.getEffectiveSpeed()

        player.speed = min(player.speed + Constants.SPEED_INCREMENT * deltaTime * 60f, Constants.MAX_SPEED)
        player.distanceScore += effectiveSpeed * Constants.DISTANCE_SCORE_FACTOR * deltaTime * 60f

        val scoreMultiplier = if (player.hasDoubleScore) 2 else 1
        player.score = (player.distanceScore.toLong() + player.coinScore) * scoreMultiplier

        updatePlayerPosition(deltaTime)
        updatePowerUpTimers(now)
        updateTraffic(deltaTime, now, effectiveSpeed)
        updateCoins(deltaTime, effectiveSpeed)
        updatePowerUps(deltaTime, effectiveSpeed)
        updateParticles(deltaTime)
        updateRoadMarkings(deltaTime, effectiveSpeed)
        updateScenery(deltaTime, effectiveSpeed)
        updateNightCycle()
        updateScreenShake(now)
        checkCollisions(now)
        checkNearMisses(now)
    }

    private fun updatePlayerPosition(deltaTime: Float) {
        val targetX = getLaneCenter(player.targetLaneIndex)
        val dx = targetX - player.x
        if (abs(dx) > 1f) {
            player.x += dx * Constants.LANE_SWITCH_SPEED * deltaTime * 60f
            player.laneTransition = abs(dx) / laneWidth
        } else {
            player.x = targetX
            player.laneIndex = player.targetLaneIndex
            player.laneTransition = 0f
        }
    }

    private fun updatePowerUpTimers(now: Long) {
        if (player.isBoosting && now > player.boostEndTime) {
            player.isBoosting = false
        }
        if (player.hasShield && now > player.shieldEndTime) {
            player.hasShield = false
        }
        if (player.hasMagnet && now > player.magnetEndTime) {
            player.hasMagnet = false
        }
        if (player.hasDoubleScore && now > player.doubleScoreEndTime) {
            player.hasDoubleScore = false
        }
        if (player.isInvincible && now > player.invincibleEndTime) {
            player.isInvincible = false
        }
    }

    private fun updateTraffic(deltaTime: Float, now: Long, effectiveSpeed: Float) {
        val spawnInterval = max(
            Constants.TRAFFIC_MIN_SPAWN_MS,
            Constants.TRAFFIC_SPAWN_INTERVAL_MS - (player.speed * 15).toLong()
        )

        if (now - lastTrafficSpawn > spawnInterval) {
            spawnTraffic()
            lastTrafficSpawn = now
        }

        val iterator = trafficCars.iterator()
        while (iterator.hasNext()) {
            val car = iterator.next()
            val relativeSpeed = effectiveSpeed - car.speed
            car.y += relativeSpeed * deltaTime * 60f
            if (car.y > screenHeight + car.height || car.y < -car.height * 2) {
                iterator.remove()
            }
        }
    }

    private fun spawnTraffic() {
        val lane = Random.nextInt(Constants.NUM_LANES)
        val type = CarType.entries[Random.nextInt(CarType.entries.size)]
        val baseWidth = screenWidth * Constants.PLAYER_WIDTH_RATIO
        val baseHeight = screenHeight * Constants.PLAYER_HEIGHT_RATIO

        val canSpawn = trafficCars.none { car ->
            car.laneIndex == lane && car.y < baseHeight * type.heightMult * 2.5f
        }

        if (canSpawn) {
            val speedVariance = 1f + (Random.nextFloat() - 0.5f) * Constants.TRAFFIC_SPEED_VARIANCE * 2f
            val trafficSpeed = player.speed * 0.6f * type.speedMult * speedVariance

            trafficCars.add(TrafficCar(
                x = getLaneCenter(lane),
                y = -baseHeight * type.heightMult,
                width = baseWidth * type.widthMult,
                height = baseHeight * type.heightMult,
                speed = trafficSpeed,
                laneIndex = lane,
                type = type,
                color = trafficColors[Random.nextInt(trafficColors.size)]
            ))
        }

        if (Random.nextFloat() < Constants.COIN_SPAWN_CHANCE) {
            spawnCoin()
        }
        if (Random.nextFloat() < Constants.POWERUP_SPAWN_CHANCE) {
            spawnPowerUp()
        }
    }

    private fun spawnCoin() {
        val lane = Random.nextInt(Constants.NUM_LANES)
        val coinSize = screenWidth * Constants.COIN_SIZE_RATIO
        coins.add(Coin(
            x = getLaneCenter(lane),
            y = -coinSize,
            size = coinSize,
            laneIndex = lane
        ))
    }

    private fun spawnPowerUp() {
        val lane = Random.nextInt(Constants.NUM_LANES)
        val size = screenWidth * Constants.COIN_SIZE_RATIO * 1.5f
        val type = PowerUpType.entries[Random.nextInt(PowerUpType.entries.size)]
        powerUps.add(PowerUp(
            x = getLaneCenter(lane),
            y = -size,
            size = size,
            laneIndex = lane,
            type = type
        ))
    }

    private fun updateCoins(deltaTime: Float, effectiveSpeed: Float) {
        val magnetRange = if (player.hasMagnet) screenWidth * Constants.MAGNET_RANGE_RATIO else 0f

        val iterator = coins.iterator()
        while (iterator.hasNext()) {
            val coin = iterator.next()
            coin.y += effectiveSpeed * deltaTime * 60f
            coin.rotation += Constants.COIN_ROTATION_SPEED * deltaTime * 60f

            if (coin.collected) {
                coin.collectAnimProgress += deltaTime * 5f
                if (coin.collectAnimProgress >= 1f) {
                    iterator.remove()
                    continue
                }
            } else if (player.hasMagnet) {
                val dx = player.x - coin.x
                val dy = player.y - coin.y
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                if (dist < magnetRange) {
                    val pull = (1f - dist / magnetRange) * 0.15f * deltaTime * 60f
                    coin.x += dx * pull
                    coin.y += dy * pull
                }
            }

            if (coin.y > screenHeight + coin.size) {
                iterator.remove()
            }
        }
    }

    private fun updatePowerUps(deltaTime: Float, effectiveSpeed: Float) {
        val iterator = powerUps.iterator()
        while (iterator.hasNext()) {
            val pu = iterator.next()
            pu.y += effectiveSpeed * deltaTime * 60f
            pu.rotation += 3f * deltaTime * 60f
            pu.pulsePhase += deltaTime * 4f

            if (pu.y > screenHeight + pu.size) {
                iterator.remove()
            }
        }
    }

    private fun updateParticles(deltaTime: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.lifetime += deltaTime * 1000f
            if (p.lifetime >= p.maxLifetime) {
                iterator.remove()
                continue
            }
            val progress = p.lifetime / p.maxLifetime
            p.x += p.vx * deltaTime * 60f
            p.y += p.vy * deltaTime * 60f
            p.vy += p.gravity * deltaTime * 60f
            p.alpha = 1f - progress
            if (p.shrink) {
                p.size *= (1f - deltaTime * 2f)
            }
        }
    }

    private fun updateRoadMarkings(deltaTime: Float, effectiveSpeed: Float) {
        markingOffset += effectiveSpeed * deltaTime * 60f
        val markingHeight = screenHeight * Constants.ROAD_MARKING_LENGTH_RATIO
        val gap = screenHeight * Constants.ROAD_MARKING_GAP_RATIO
        val totalSpacing = markingHeight + gap

        for (marking in roadMarkings) {
            marking.y += effectiveSpeed * deltaTime * 60f
        }

        roadMarkings.removeAll { it.y > screenHeight + totalSpacing }

        while (roadMarkings.isEmpty() || roadMarkings.minOf { it.y } > -totalSpacing) {
            val minY = if (roadMarkings.isEmpty()) 0f else roadMarkings.minOf { it.y }
            roadMarkings.add(RoadMarking(minY - totalSpacing))
        }
    }

    private fun updateScenery(deltaTime: Float, effectiveSpeed: Float) {
        val spacing = screenHeight * Constants.TREE_SPACING_RATIO

        for (obj in sceneryObjects) {
            obj.y += effectiveSpeed * deltaTime * 60f
        }

        sceneryObjects.removeAll { it.y > screenHeight + spacing }

        val leftObjs = sceneryObjects.filter { it.x < screenWidth / 2 }
        val rightObjs = sceneryObjects.filter { it.x >= screenWidth / 2 }

        if (leftObjs.isEmpty() || leftObjs.minOf { it.y } > -spacing * 0.5f) {
            val minY = if (leftObjs.isEmpty()) -spacing else leftObjs.minOf { it.y }
            sceneryObjects.add(SceneryObject(
                x = roadLeft - screenWidth * (0.05f + Random.nextFloat() * 0.06f),
                y = minY - spacing * (0.8f + Random.nextFloat() * 0.4f),
                type = Random.nextInt(3),
                scale = 0.8f + Random.nextFloat() * 0.4f
            ))
        }

        if (rightObjs.isEmpty() || rightObjs.minOf { it.y } > -spacing * 0.5f) {
            val minY = if (rightObjs.isEmpty()) -spacing else rightObjs.minOf { it.y }
            sceneryObjects.add(SceneryObject(
                x = roadRight + screenWidth * (0.05f + Random.nextFloat() * 0.06f),
                y = minY - spacing * (0.8f + Random.nextFloat() * 0.4f),
                type = Random.nextInt(3),
                scale = 0.8f + Random.nextFloat() * 0.4f
            ))
        }
    }

    private fun updateNightCycle() {
        val targetNight = if (player.score > Constants.NIGHT_MODE_SCORE_THRESHOLD) {
            min(1f, (player.score - Constants.NIGHT_MODE_SCORE_THRESHOLD) / 5000f)
        } else 0f
        nightFactor += (targetNight - nightFactor) * Constants.NIGHT_TRANSITION_SPEED
    }

    private fun updateScreenShake(now: Long) {
        if (now < screenShakeEndTime) {
            val progress = 1f - (screenShakeEndTime - now).toFloat() / Constants.SCREEN_SHAKE_DURATION_MS
            val intensity = Constants.SCREEN_SHAKE_INTENSITY * (1f - progress)
            screenShakeX = (Random.nextFloat() - 0.5f) * intensity * 2f
            screenShakeY = (Random.nextFloat() - 0.5f) * intensity * 2f
        } else {
            screenShakeX = 0f
            screenShakeY = 0f
        }
    }

    private fun checkCollisions(now: Long) {
        if (player.isInvincible) return

        val playerRect = player.getRect()

        val shrinkX = playerRect.width() * 0.12f
        val shrinkY = playerRect.height() * 0.08f
        val playerHitbox = android.graphics.RectF(
            playerRect.left + shrinkX,
            playerRect.top + shrinkY,
            playerRect.right - shrinkX,
            playerRect.bottom - shrinkY
        )

        for (car in trafficCars) {
            val carRect = car.getRect()
            val carShrinkX = carRect.width() * 0.1f
            val carShrinkY = carRect.height() * 0.05f
            val carHitbox = android.graphics.RectF(
                carRect.left + carShrinkX,
                carRect.top + carShrinkY,
                carRect.right - carShrinkX,
                carRect.bottom - carShrinkY
            )

            if (android.graphics.RectF.intersects(playerHitbox, carHitbox)) {
                if (player.hasShield) {
                    player.hasShield = false
                    spawnParticles(car.x, car.y, Constants.PARTICLE_COUNT_CRASH, 0xFF00E5FF.toInt())
                    soundManager?.playPowerUp()
                    trafficCars.remove(car)
                    return
                }

                handleCrash(now, car)
                return
            }
        }

        val coinIterator = coins.iterator()
        while (coinIterator.hasNext()) {
            val coin = coinIterator.next()
            if (coin.collected) continue
            val dx = player.x - coin.x
            val dy = player.y - coin.y
            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
            if (dist < player.width * 0.6f + coin.size * 0.5f) {
                coin.collected = true
                coin.collectAnimProgress = 0f
                player.coinScore += Constants.COIN_VALUE
                spawnParticles(coin.x, coin.y, Constants.PARTICLE_COUNT_COIN, 0xFFFFD700.toInt())
                soundManager?.playCoinPickup()
            }
        }

        val puIterator = powerUps.iterator()
        while (puIterator.hasNext()) {
            val pu = puIterator.next()
            val dx = player.x - pu.x
            val dy = player.y - pu.y
            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
            if (dist < player.width * 0.6f + pu.size * 0.5f) {
                activatePowerUp(pu, now)
                spawnParticles(pu.x, pu.y, Constants.PARTICLE_COUNT_POWERUP, pu.type.color)
                soundManager?.playPowerUp()
                puIterator.remove()
            }
        }
    }

    private fun handleCrash(now: Long, car: TrafficCar) {
        player.lives--
        player.isInvincible = true
        player.invincibleEndTime = now + Constants.INVINCIBILITY_AFTER_HIT_MS
        player.comboCount = 0
        player.isBoosting = false

        spawnParticles(
            (player.x + car.x) / 2f,
            (player.y + car.y) / 2f,
            Constants.PARTICLE_COUNT_CRASH,
            0xFFFF5722.toInt()
        )

        triggerScreenShake(now)
        soundManager?.playCrash()

        if (player.lives <= 0) {
            gameOver()
        }
    }

    private fun activatePowerUp(pu: PowerUp, now: Long) {
        when (pu.type) {
            PowerUpType.SHIELD -> {
                player.hasShield = true
                player.shieldEndTime = now + Constants.SHIELD_DURATION_MS
            }
            PowerUpType.MAGNET -> {
                player.hasMagnet = true
                player.magnetEndTime = now + Constants.MAGNET_DURATION_MS
            }
            PowerUpType.DOUBLE_SCORE -> {
                player.hasDoubleScore = true
                player.doubleScoreEndTime = now + Constants.DOUBLE_SCORE_DURATION_MS
            }
            PowerUpType.EXTRA_LIFE -> {
                player.lives = min(player.lives + 1, Constants.MAX_LIVES)
            }
            PowerUpType.NITRO -> {
                player.isBoosting = true
                player.boostEndTime = now + Constants.BOOST_DURATION_MS
            }
        }
    }

    private fun checkNearMisses(now: Long) {
        if (player.isInvincible) return

        val nearMissThreshold = screenWidth * Constants.NEAR_MISS_DISTANCE_RATIO + player.width * 0.6f

        for (car in trafficCars) {
            if (car.scored) continue
            val dy = abs(player.y - car.y)
            val dx = abs(player.x - car.x)

            if (dy < (player.height + car.height) * 0.6f &&
                dx < nearMissThreshold + car.width * 0.5f &&
                dx > (player.width + car.width) * 0.45f
            ) {
                car.scored = true

                if (now - player.lastComboTime < 2000) {
                    player.comboCount = min(player.comboCount + 1, Constants.MAX_COMBO)
                } else {
                    player.comboCount = 1
                }
                player.lastComboTime = now

                val bonus = (Constants.NEAR_MISS_BONUS *
                    (1f + player.comboCount * Constants.COMBO_MULTIPLIER)).toLong()
                player.coinScore += bonus

                spawnParticles(player.x, player.y - player.height / 2, 5, 0xFF00E676.toInt())
            }
        }
    }

    private fun spawnParticles(x: Float, y: Float, count: Int, baseColor: Int) {
        for (i in 0 until count) {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            val speed = 2f + Random.nextFloat() * 6f
            val r = ((baseColor shr 16) and 0xFF) + Random.nextInt(-30, 30)
            val g = ((baseColor shr 8) and 0xFF) + Random.nextInt(-30, 30)
            val b = (baseColor and 0xFF) + Random.nextInt(-30, 30)
            val color = (0xFF shl 24) or
                (r.coerceIn(0, 255) shl 16) or
                (g.coerceIn(0, 255) shl 8) or
                b.coerceIn(0, 255)

            particles.add(Particle(
                x = x + (Random.nextFloat() - 0.5f) * 20f,
                y = y + (Random.nextFloat() - 0.5f) * 20f,
                vx = kotlin.math.cos(angle) * speed,
                vy = kotlin.math.sin(angle) * speed,
                size = 3f + Random.nextFloat() * 8f,
                color = color,
                gravity = 0.15f,
                maxLifetime = Constants.PARTICLE_LIFETIME_MS * (0.5f + Random.nextFloat() * 0.5f)
            ))
        }
    }

    private fun triggerScreenShake(now: Long) {
        screenShakeEndTime = now + Constants.SCREEN_SHAKE_DURATION_MS.toLong()
    }

    fun gameOver() {
        state = GameState.GAME_OVER
        if (player.score > highScore) {
            highScore = player.score
            highScoreManager.saveHighScore(highScore)
        }
        soundManager?.playGameOver()
    }

    fun moveLeft() {
        if (player.targetLaneIndex > 0) {
            player.targetLaneIndex--
            soundManager?.playLaneSwitch()
        }
    }

    fun moveRight() {
        if (player.targetLaneIndex < Constants.NUM_LANES - 1) {
            player.targetLaneIndex++
            soundManager?.playLaneSwitch()
        }
    }

    fun activateBoost() {
        if (!player.isBoosting) {
            player.isBoosting = true
            player.boostEndTime = System.currentTimeMillis() + Constants.BOOST_DURATION_MS
            soundManager?.playBoost()
        }
    }

    fun pause() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED
        }
    }

    fun resume() {
        if (state == GameState.PAUSED) {
            state = GameState.PLAYING
        }
    }

    fun release() {
        soundManager?.release()
    }
}
