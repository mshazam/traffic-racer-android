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
    val floatingTexts = mutableListOf<FloatingText>()
    val hazards = mutableListOf<RoadHazard>()
    val mysteryBoxes = mutableListOf<MysteryBox>()
    val weatherParticles = mutableListOf<WeatherParticle>()
    val tireTrails = mutableListOf<TireTrack>()
    val driftSparks = mutableListOf<DriftSpark>()
    var activeMilestone: MilestoneEvent? = null

    var currentEnvironment: Environment = Environment.CITY
    var nightFactor: Float = 0f
    var screenShakeX: Float = 0f
    var screenShakeY: Float = 0f
    var slowMoFactor: Float = 1f
    var slowMoEndTime: Long = 0
    var cameraZoom: Float = 1f

    var garageSelectedIndex: Int = 0
    var mysteryBoxReward: String? = null
    var mysteryBoxRewardTime: Long = 0

    val gameData by lazy { GameData(context) }
    var currentMissions: List<MissionDef> = emptyList()
    val missionProgress = mutableMapOf<String, Int>()
    var newAchievements = mutableListOf<AchievementDef>()

    private var screenShakeEndTime: Long = 0
    private var lastTrafficSpawn: Long = 0
    private var lastMilestoneDistance: Long = 0
    private var environmentDistance: Float = 0f
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

        soundManager = SoundManager(context)
        currentMissions = MissionBank.generateMissions(gameData.missionDifficulty)
        garageSelectedIndex = PlayerCarDef.ALL_CARS.indexOfFirst { it.id == gameData.selectedCarId }.coerceAtLeast(0)

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
                x = roadLeft - screenWidth * 0.08f, y = y,
                type = Random.nextInt(4), scale = 0.8f + Random.nextFloat() * 0.4f
            ))
            sceneryObjects.add(SceneryObject(
                x = roadRight + screenWidth * 0.08f, y = y + spacing * 0.5f,
                type = Random.nextInt(4), scale = 0.8f + Random.nextFloat() * 0.4f
            ))
            y += spacing
        }
    }

    private fun initWeatherParticles() {
        weatherParticles.clear()
        if (currentEnvironment.hasWeather) {
            for (i in 0 until Constants.WEATHER_PARTICLE_COUNT) {
                weatherParticles.add(WeatherParticle(
                    x = Random.nextFloat() * screenWidth,
                    y = Random.nextFloat() * screenHeight,
                    speed = Constants.WEATHER_PARTICLE_SPEED * (0.7f + Random.nextFloat() * 0.6f),
                    size = if (currentEnvironment.weatherType == WeatherType.SNOW) 3f + Random.nextFloat() * 4f else 1.5f + Random.nextFloat() * 2f,
                    alpha = 0.4f + Random.nextFloat() * 0.6f,
                    windOffset = Random.nextFloat() * 2f - 1f
                ))
            }
        }
    }

    fun startGame() {
        state = GameState.PLAYING
        val selectedCar = gameData.getSelectedCar()
        player = PlayerCar(
            width = screenWidth * Constants.PLAYER_WIDTH_RATIO,
            height = screenHeight * Constants.PLAYER_HEIGHT_RATIO,
            carDef = selectedCar
        )
        player.x = screenWidth / 2f
        player.y = screenHeight * 0.75f

        trafficCars.clear(); coins.clear(); powerUps.clear(); particles.clear()
        floatingTexts.clear(); hazards.clear(); mysteryBoxes.clear()
        tireTrails.clear(); driftSparks.clear()
        nightFactor = 0f; lastTrafficSpawn = 0; lastMilestoneDistance = 0
        environmentDistance = 0f; currentEnvironment = Environment.CITY
        slowMoFactor = 1f; cameraZoom = 1f; activeMilestone = null
        mysteryBoxReward = null; missionProgress.clear()

        initRoadMarkings(); initScenery(); initWeatherParticles()
    }

    fun getLaneCenter(lane: Int): Float = roadLeft + laneWidth * lane + laneWidth / 2f

    // ---- Main update loop ----
    fun update(deltaTime: Float) {
        if (state != GameState.PLAYING) return
        try { updateInternal(deltaTime) } catch (_: Exception) {}
    }

    private fun updateInternal(deltaTime: Float) {
        val now = System.currentTimeMillis()

        updateSlowMo(now)
        val dt = deltaTime * slowMoFactor
        val effectiveSpeed = player.getEffectiveSpeed()

        updateSpeed(dt)
        updateSteering(dt)
        updateNOS(dt)
        updateBrakeLights(dt)

        player.distanceScore += effectiveSpeed * Constants.DISTANCE_SCORE_FACTOR * dt * 60f
        player.distanceWithoutCrash += effectiveSpeed * Constants.DISTANCE_SCORE_FACTOR * dt * 60f
        val kmh = effectiveSpeed * 12f
        if (kmh > player.maxSpeedReachedThisRun) player.maxSpeedReachedThisRun = kmh

        val scoreMult = if (player.hasDoubleScore) 2 else 1
        player.score = (player.distanceScore.toLong() + player.coinScore) * scoreMult

        environmentDistance += effectiveSpeed * dt * 60f
        if (environmentDistance > Constants.ENV_CHANGE_DISTANCE) { cycleEnvironment(); environmentDistance = 0f }

        updateCameraZoom(dt)
        updatePowerUpTimers(now)
        updateSlipTimer(now)
        updateTraffic(dt, now, effectiveSpeed)
        updateCoins(dt, effectiveSpeed)
        updatePowerUps(dt, effectiveSpeed)
        updateHazards(dt, effectiveSpeed)
        updateMysteryBoxes(dt, effectiveSpeed)
        updateParticles(dt)
        updateDriftSparks(dt)
        updateFloatingTexts(now)
        updateRoadMarkings(dt, effectiveSpeed)
        updateScenery(dt, effectiveSpeed)
        updateWeatherParticles(dt, effectiveSpeed)
        updateTireTrails(dt)
        updateNightCycle()
        updateScreenShake(now)
        updateMilestones(now)
        checkCollisions(now)
        checkNearMisses(now)
        checkOvertakes(now)
        checkMissionProgress()
    }

    // ---- NFS Physics ----
    private fun updateSpeed(dt: Float) {
        val accel = when {
            player.isAccelerating -> Constants.ACCEL_FORCE * player.carDef.baseSpeed
            player.isBraking -> -Constants.BRAKE_FORCE
            else -> -Constants.COAST_DECEL
        }
        player.speed += accel * dt * 60f
        player.speed += Constants.IDLE_SPEED * 0.01f * dt * 60f
        player.speed = player.speed.coerceIn(Constants.IDLE_SPEED, Constants.MAX_SPEED * player.carDef.baseSpeed)
    }

    private fun updateSteering(dt: Float) {
        val handling = player.getEffectiveHandling()
        val targetAngle = player.steerInput * Constants.MAX_STEER_ANGLE

        if (abs(player.steerInput) > 0.05f) {
            player.steerAngle += (targetAngle - player.steerAngle) * Constants.STEER_SPEED * handling * dt
        } else {
            player.steerAngle *= (1f - Constants.STEER_RETURN_SPEED * dt)
            if (abs(player.steerAngle) < 0.01f) player.steerAngle = 0f
        }
        player.steerAngle = player.steerAngle.coerceIn(-Constants.MAX_STEER_ANGLE, Constants.MAX_STEER_ANGLE)

        val moveX = player.steerAngle * Constants.STEER_TO_MOVEMENT * player.speed / 15f * dt * 60f
        player.x += moveX

        // Drift
        player.isDrifting = player.isDriftingNow()
        if (player.isDrifting) {
            val driftTarget = player.steerAngle * 0.3f
            player.driftAngle += (driftTarget - player.driftAngle) * Constants.DRIFT_FACTOR * dt * 10f
            if (Random.nextFloat() < 0.3f) spawnDriftSpark()
            player.nosAmount = min(Constants.NOS_MAX, player.nosAmount + 0.5f * dt * 60f)
        } else {
            player.driftAngle *= (1f - dt * 8f)
        }

        // Clamp to road
        val margin = player.width * 0.4f
        player.x = player.x.coerceIn(roadLeft + margin, roadRight - margin)
    }

    private fun updateNOS(dt: Float) {
        if (player.nosActive && player.nosAmount > 0) {
            player.nosAmount -= Constants.NOS_DRAIN_RATE * dt
            if (player.nosAmount <= 0) {
                player.nosAmount = 0f
                player.nosActive = false
            }
        } else if (!player.nosActive) {
            player.nosAmount = min(Constants.NOS_MAX, player.nosAmount + Constants.NOS_FILL_RATE * dt)
        }
    }

    private fun updateBrakeLights(dt: Float) {
        val target = if (player.isBraking) 1f else 0f
        player.brakeLightIntensity += (target - player.brakeLightIntensity) * 10f * dt
    }

    private fun updateSlowMo(now: Long) {
        if (now < slowMoEndTime) {
            val elapsed = (now - (slowMoEndTime - Constants.SLOW_MO_DURATION_MS)).toFloat()
            val progress = (elapsed / Constants.SLOW_MO_DURATION_MS).coerceIn(0f, 1f)
            slowMoFactor = Constants.SLOW_MO_FACTOR + (1f - Constants.SLOW_MO_FACTOR) * progress * 0.3f
        } else {
            slowMoFactor = min(slowMoFactor + 0.05f, 1f)
        }
    }

    private fun updateCameraZoom(dt: Float) {
        val target = when {
            player.nosActive -> Constants.CAMERA_ZOOM_BOOST
            player.speed > Constants.SPEED_BLUR_THRESHOLD -> 1f + (player.speed - Constants.SPEED_BLUR_THRESHOLD) / Constants.MAX_SPEED * 0.02f
            else -> 1f
        }
        cameraZoom += (target - cameraZoom) * Constants.CAMERA_ZOOM_SPEED * dt * 60f
    }

    private fun cycleEnvironment() {
        val envs = Environment.entries
        val nextIndex = (envs.indexOf(currentEnvironment) + 1) % envs.size
        currentEnvironment = envs[nextIndex]
        initScenery(); initWeatherParticles()
        addFloatingText(screenWidth / 2, screenHeight * 0.3f, currentEnvironment.displayName, 0xFFFFFFFF.toInt(), 2.0f)
    }

    private fun updatePowerUpTimers(now: Long) {
        if (player.hasShield && now > player.shieldEndTime) player.hasShield = false
        if (player.hasMagnet && now > player.magnetEndTime) player.hasMagnet = false
        if (player.hasDoubleScore && now > player.doubleScoreEndTime) player.hasDoubleScore = false
        if (player.isInvincible && now > player.invincibleEndTime) player.isInvincible = false
    }

    private fun updateSlipTimer(now: Long) {
        if (player.isSlipping && now > player.slipEndTime) player.isSlipping = false
    }

    // ---- Traffic ----
    private fun updateTraffic(deltaTime: Float, now: Long, effectiveSpeed: Float) {
        val spawnInterval = max(
            Constants.TRAFFIC_MIN_SPAWN_MS,
            Constants.TRAFFIC_SPAWN_INTERVAL_MS - (player.speed * 12).toLong()
        )
        if (now - lastTrafficSpawn > spawnInterval) { spawnTraffic(); lastTrafficSpawn = now }

        val iter = trafficCars.iterator()
        while (iter.hasNext()) {
            val car = iter.next()
            if (car.isOncoming) car.y += (effectiveSpeed + car.speed) * deltaTime * 60f
            else car.y += (effectiveSpeed - car.speed) * deltaTime * 60f
            if (car.y > screenHeight + car.height || car.y < -car.height * 3) iter.remove()
        }
    }

    private fun spawnTraffic() {
        val lane = Random.nextInt(Constants.NUM_LANES)
        val type = CarType.entries[Random.nextInt(CarType.entries.size)]
        val baseW = screenWidth * Constants.PLAYER_WIDTH_RATIO
        val baseH = screenHeight * Constants.PLAYER_HEIGHT_RATIO
        val laneX = getLaneCenter(lane)

        val canSpawn = trafficCars.none { it.laneIndex == lane && it.y < baseH * type.heightMult * 4f } && trafficCars.size < 6
        if (canSpawn) {
            val isOncoming = Random.nextFloat() < Constants.ONCOMING_TRAFFIC_CHANCE
            val sv = 1f + (Random.nextFloat() - 0.5f) * Constants.TRAFFIC_SPEED_VARIANCE * 2f
            val spd = if (isOncoming) player.speed * Constants.ONCOMING_SPEED_MULT * sv
                      else player.speed * 0.6f * type.speedMult * sv
            trafficCars.add(TrafficCar(
                x = laneX, y = if (isOncoming) screenHeight + baseH * type.heightMult else -baseH * type.heightMult,
                width = baseW * type.widthMult, height = baseH * type.heightMult,
                speed = spd, laneIndex = lane, type = type,
                color = trafficColors[Random.nextInt(trafficColors.size)], isOncoming = isOncoming
            ))
        }
        if (Random.nextFloat() < Constants.COIN_SPAWN_CHANCE) spawnCoin()
        if (Random.nextFloat() < Constants.POWERUP_SPAWN_CHANCE) spawnPowerUp()
        if (Random.nextFloat() < Constants.HAZARD_SPAWN_CHANCE) spawnHazard()
        if (Random.nextFloat() < Constants.MYSTERY_BOX_SPAWN_CHANCE) spawnMysteryBox()
    }

    private fun spawnCoin() {
        val lane = Random.nextInt(Constants.NUM_LANES)
        val s = screenWidth * Constants.COIN_SIZE_RATIO
        coins.add(Coin(x = getLaneCenter(lane), y = -s, size = s, laneIndex = lane))
    }
    private fun spawnPowerUp() {
        val lane = Random.nextInt(Constants.NUM_LANES)
        val s = screenWidth * Constants.COIN_SIZE_RATIO * 1.5f
        val t = PowerUpType.entries[Random.nextInt(PowerUpType.entries.size)]
        powerUps.add(PowerUp(x = getLaneCenter(lane), y = -s, size = s, laneIndex = lane, type = t))
    }
    private fun spawnHazard() {
        val lane = Random.nextInt(Constants.NUM_LANES)
        val s = screenWidth * Constants.HAZARD_SIZE_RATIO
        val t = HazardType.entries[Random.nextInt(HazardType.entries.size)]
        hazards.add(RoadHazard(x = getLaneCenter(lane), y = -s, size = s, laneIndex = lane, type = t))
    }
    private fun spawnMysteryBox() {
        val lane = Random.nextInt(Constants.NUM_LANES)
        val s = screenWidth * Constants.MYSTERY_BOX_SIZE_RATIO
        mysteryBoxes.add(MysteryBox(x = getLaneCenter(lane), y = -s, size = s, laneIndex = lane))
    }

    // ---- Item updates ----
    private fun updateCoins(dt: Float, es: Float) {
        val mr = if (player.hasMagnet) screenWidth * Constants.MAGNET_RANGE_RATIO else 0f
        val iter = coins.iterator()
        while (iter.hasNext()) {
            val c = iter.next()
            c.y += es * dt * 60f; c.rotation += Constants.COIN_ROTATION_SPEED * dt * 60f
            if (c.collected) { c.collectAnimProgress += dt * 5f; if (c.collectAnimProgress >= 1f) { iter.remove(); continue } }
            else if (player.hasMagnet) {
                val dx = player.x - c.x; val dy = player.y - c.y
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                if (dist < mr) { val p = (1f - dist / mr) * 0.15f * dt * 60f; c.x += dx * p; c.y += dy * p }
            }
            if (c.y > screenHeight + c.size) iter.remove()
        }
    }
    private fun updatePowerUps(dt: Float, es: Float) {
        val iter = powerUps.iterator()
        while (iter.hasNext()) { val p = iter.next(); p.y += es * dt * 60f; p.rotation += 3f * dt * 60f; p.pulsePhase += dt * 4f; if (p.y > screenHeight + p.size) iter.remove() }
    }
    private fun updateHazards(dt: Float, es: Float) {
        val iter = hazards.iterator()
        while (iter.hasNext()) { val h = iter.next(); h.y += es * dt * 60f; h.rotation += dt * 2f; if (h.y > screenHeight + h.size) iter.remove() }
    }
    private fun updateMysteryBoxes(dt: Float, es: Float) {
        val iter = mysteryBoxes.iterator()
        while (iter.hasNext()) { val m = iter.next(); m.y += es * dt * 60f; m.bouncePhase += dt * 5f; m.rotation += dt * 3f; if (m.y > screenHeight + m.size) iter.remove() }
    }
    private fun updateParticles(dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next(); p.lifetime += dt * 1000f
            if (p.lifetime >= p.maxLifetime) { iter.remove(); continue }
            p.x += p.vx * dt * 60f; p.y += p.vy * dt * 60f; p.vy += p.gravity * dt * 60f
            p.alpha = 1f - p.lifetime / p.maxLifetime
            if (p.shrink) p.size *= (1f - dt * 2f)
        }
    }
    private fun updateDriftSparks(dt: Float) {
        val iter = driftSparks.iterator()
        while (iter.hasNext()) { val s = iter.next(); s.x += s.vx * dt * 60f; s.y += s.vy * dt * 60f; s.life -= dt * 3f; if (s.life <= 0) iter.remove() }
    }
    private fun updateFloatingTexts(now: Long) {
        floatingTexts.removeAll { it.isExpired(now) }
        for (ft in floatingTexts) ft.y -= Constants.FLOATING_TEXT_RISE_SPEED
    }
    private fun updateWeatherParticles(dt: Float, es: Float) {
        if (!currentEnvironment.hasWeather) return
        for (wp in weatherParticles) {
            wp.y += (wp.speed + es * 0.5f) * dt * 60f; wp.x += wp.windOffset * dt * 60f
            if (wp.y > screenHeight) { wp.y = -wp.size; wp.x = Random.nextFloat() * screenWidth }
            if (wp.x < 0) wp.x = screenWidth; if (wp.x > screenWidth) wp.x = 0f
        }
    }
    private fun updateTireTrails(dt: Float) {
        val iter = tireTrails.iterator()
        while (iter.hasNext()) { val t = iter.next(); t.alpha -= dt * 0.8f; if (t.alpha <= 0) iter.remove() }
    }
    private fun updateRoadMarkings(dt: Float, es: Float) {
        val mh = screenHeight * Constants.ROAD_MARKING_LENGTH_RATIO
        val gap = screenHeight * Constants.ROAD_MARKING_GAP_RATIO
        val ts = mh + gap
        for (m in roadMarkings) m.y += es * dt * 60f
        roadMarkings.removeAll { it.y > screenHeight + ts }
        while (roadMarkings.isEmpty() || roadMarkings.minOf { it.y } > -ts)
            roadMarkings.add(RoadMarking((if (roadMarkings.isEmpty()) 0f else roadMarkings.minOf { it.y }) - ts))
    }
    private fun updateScenery(dt: Float, es: Float) {
        val sp = screenHeight * Constants.TREE_SPACING_RATIO
        for (o in sceneryObjects) o.y += es * dt * 60f
        sceneryObjects.removeAll { it.y > screenHeight + sp }
        val l = sceneryObjects.filter { it.x < screenWidth / 2 }
        val r = sceneryObjects.filter { it.x >= screenWidth / 2 }
        if (l.isEmpty() || l.minOf { it.y } > -sp * 0.5f) {
            val minY = if (l.isEmpty()) -sp else l.minOf { it.y }
            sceneryObjects.add(SceneryObject(roadLeft - screenWidth * (0.05f + Random.nextFloat() * 0.06f), minY - sp * (0.8f + Random.nextFloat() * 0.4f), Random.nextInt(4), 0.8f + Random.nextFloat() * 0.4f))
        }
        if (r.isEmpty() || r.minOf { it.y } > -sp * 0.5f) {
            val minY = if (r.isEmpty()) -sp else r.minOf { it.y }
            sceneryObjects.add(SceneryObject(roadRight + screenWidth * (0.05f + Random.nextFloat() * 0.06f), minY - sp * (0.8f + Random.nextFloat() * 0.4f), Random.nextInt(4), 0.8f + Random.nextFloat() * 0.4f))
        }
    }
    private fun updateNightCycle() {
        val cycle = (player.distanceScore / 800f) % 2f
        nightFactor = if (cycle < 1f) cycle.coerceAtMost(0.7f) else (2f - cycle).coerceAtMost(0.7f)
    }
    private fun updateScreenShake(now: Long) {
        if (now < screenShakeEndTime) {
            val p = 1f - (screenShakeEndTime - now).toFloat() / Constants.SCREEN_SHAKE_DURATION_MS
            val i = Constants.SCREEN_SHAKE_INTENSITY * (1f - p)
            screenShakeX = (Random.nextFloat() - 0.5f) * i * 2f; screenShakeY = (Random.nextFloat() - 0.5f) * i * 2f
        } else { screenShakeX = 0f; screenShakeY = 0f }
    }
    private fun updateMilestones(now: Long) {
        activeMilestone?.let { if (it.isExpired(now)) activeMilestone = null }
        val cd = player.distanceScore.toLong()
        val next = ((lastMilestoneDistance / Constants.MILESTONE_INTERVAL) + 1) * Constants.MILESTONE_INTERVAL
        if (cd >= next) {
            lastMilestoneDistance = cd; activeMilestone = MilestoneEvent(next, now)
            addFloatingText(screenWidth / 2, screenHeight * 0.25f, "${next}m!", 0xFF4CAF50.toInt(), 1.8f)
            soundManager?.playMilestone()
        }
    }

    // ---- Collisions ----
    private fun checkCollisions(now: Long) {
        if (player.isInvincible) return
        val pr = player.getRect()
        val sx = pr.width() * 0.12f; val sy = pr.height() * 0.08f
        val ph = android.graphics.RectF(pr.left + sx, pr.top + sy, pr.right - sx, pr.bottom - sy)

        var hitCar: TrafficCar? = null
        for (car in trafficCars) {
            val cr = car.getRect()
            val cxs = cr.width() * 0.1f; val cys = cr.height() * 0.05f
            val ch = android.graphics.RectF(cr.left + cxs, cr.top + cys, cr.right - cxs, cr.bottom - cys)
            if (android.graphics.RectF.intersects(ph, ch)) { hitCar = car; break }
        }
        if (hitCar != null) {
            if (player.hasShield) {
                player.hasShield = false
                spawnParticles(hitCar.x, hitCar.y, Constants.PARTICLE_COUNT_CRASH, 0xFF00E5FF.toInt())
                addFloatingText(hitCar.x, hitCar.y, "SHIELD!", 0xFF00E5FF.toInt(), 1.2f)
                soundManager?.playPowerUp(); trafficCars.remove(hitCar); return
            }
            handleCrash(now, hitCar); return
        }
        // Coins
        val ci = coins.iterator()
        while (ci.hasNext()) { val c = ci.next(); if (c.collected) continue
            val d = dist(player.x, player.y, c.x, c.y)
            if (d < player.width * 0.6f + c.size * 0.5f) {
                c.collected = true; c.collectAnimProgress = 0f; player.coinScore += Constants.COIN_VALUE; player.coinsCollectedThisRun++
                spawnParticles(c.x, c.y, Constants.PARTICLE_COUNT_COIN, 0xFFFFD700.toInt())
                addFloatingText(c.x, c.y - 20f, "+${Constants.COIN_VALUE}", 0xFFFFD700.toInt(), 1f)
                soundManager?.playCoinPickup()
            }
        }
        // PowerUps
        val pi = powerUps.iterator()
        while (pi.hasNext()) { val p = pi.next()
            if (dist(player.x, player.y, p.x, p.y) < player.width * 0.6f + p.size * 0.5f) {
                activatePowerUp(p, now); spawnParticles(p.x, p.y, Constants.PARTICLE_COUNT_POWERUP, p.type.color)
                player.powerUpsCollectedThisRun++; soundManager?.playPowerUp(); pi.remove()
            }
        }
        // Hazards
        val hi = hazards.iterator()
        while (hi.hasNext()) { val h = hi.next()
            if (android.graphics.RectF.intersects(ph, h.getRect())) {
                when (h.type) {
                    HazardType.OIL_SLICK -> if (!player.hasShield) {
                        player.isSlipping = true; player.slipEndTime = now + h.type.durationMs
                        addFloatingText(h.x, h.y, "SLIPPING!", 0xFFFF9800.toInt(), 1.2f); soundManager?.playHazard()
                    }
                    HazardType.CONE -> if (!player.hasShield) {
                        spawnParticles(h.x, h.y, 15, 0xFFFF6D00.toInt()); triggerScreenShake(now)
                        player.speed = max(Constants.IDLE_SPEED, player.speed * 0.8f)
                        addFloatingText(h.x, h.y, "OUCH!", 0xFFFF5722.toInt(), 1f); soundManager?.playHazard()
                    }
                    HazardType.POTHOLE -> if (!player.hasShield) {
                        player.isSlipping = true; player.slipEndTime = now + h.type.durationMs; triggerScreenShake(now)
                        addFloatingText(h.x, h.y, "BUMP!", 0xFF795548.toInt(), 1f); soundManager?.playHazard()
                    }
                }
                hi.remove()
            }
        }
        // Mystery boxes
        val mi = mysteryBoxes.iterator()
        while (mi.hasNext()) { val m = mi.next()
            if (dist(player.x, player.y, m.x, m.y) < player.width * 0.6f + m.size * 0.5f) {
                openMysteryBox(m, now); spawnParticles(m.x, m.y, 25, 0xFFE040FB.toInt())
                soundManager?.playMysteryBox(); mi.remove()
            }
        }
    }

    private fun checkNearMisses(now: Long) {
        if (player.isInvincible) return
        val thresh = screenWidth * Constants.NEAR_MISS_DISTANCE_RATIO + player.width * 0.6f
        for (car in trafficCars) {
            if (car.nearMissScored) continue
            val dy = abs(player.y - car.y); val dx = abs(player.x - car.x)
            if (dy < (player.height + car.height) * 0.6f && dx < thresh + car.width * 0.5f && dx > (player.width + car.width) * 0.45f) {
                car.nearMissScored = true; player.nearMissesThisRun++
                if (now - player.lastComboTime < 2000) player.comboCount = min(player.comboCount + 1, Constants.MAX_COMBO)
                else player.comboCount = 1
                if (player.comboCount > player.maxComboThisRun) player.maxComboThisRun = player.comboCount
                player.lastComboTime = now
                val bonus = (Constants.NEAR_MISS_BONUS * (1f + player.comboCount * Constants.COMBO_MULTIPLIER)).toLong()
                player.coinScore += bonus
                player.nosAmount = min(Constants.NOS_MAX, player.nosAmount + Constants.NOS_FILL_ON_NEAR_MISS)
                slowMoEndTime = now + Constants.SLOW_MO_DURATION_MS; slowMoFactor = Constants.SLOW_MO_FACTOR
                spawnParticles(player.x, player.y - player.height / 2, 8, 0xFF00E676.toInt())
                val txt = if (player.comboCount > 1) "x${player.comboCount} COMBO!" else "CLOSE!"
                val col = if (player.comboCount >= 5) 0xFFFF6D00.toInt() else if (player.comboCount >= 3) 0xFFFFD700.toInt() else 0xFF00E676.toInt()
                addFloatingText(player.x, player.y - player.height, "+$bonus $txt", col, 1f + player.comboCount * 0.1f)
                soundManager?.playNearMiss()
            }
        }
    }

    private fun checkOvertakes(now: Long) {
        for (car in trafficCars) {
            if (car.overtakeScored || car.isOncoming) continue
            if (car.y > player.y + player.height && car.y < player.y + player.height * 3f) {
                val dx = abs(player.x - car.x)
                if (dx < laneWidth * 1.5f && player.getEffectiveSpeed() > car.speed * Constants.OVERTAKE_SPEED_THRESHOLD) {
                    car.overtakeScored = true; player.overtakesThisRun++
                    player.coinScore += Constants.OVERTAKE_BONUS
                    player.nosAmount = min(Constants.NOS_MAX, player.nosAmount + Constants.NOS_FILL_ON_OVERTAKE)
                    addFloatingText(car.x, car.y - car.height, "+${Constants.OVERTAKE_BONUS} OVERTAKE!", 0xFF64FFDA.toInt(), 1.2f)
                    soundManager?.playNearMiss()
                }
            }
        }
    }

    private fun checkMissionProgress() {
        for (m in currentMissions) {
            if (gameData.isMissionCompleted(m.id)) continue
            val progress = when (m.type) {
                MissionType.COLLECT_COINS -> player.coinsCollectedThisRun
                MissionType.NEAR_MISSES -> player.nearMissesThisRun
                MissionType.REACH_SPEED -> if (player.maxSpeedReachedThisRun >= m.target) m.target else 0
                MissionType.TRAVEL_DISTANCE -> player.distanceScore.toInt()
                MissionType.COLLECT_POWERUPS -> player.powerUpsCollectedThisRun
                MissionType.DESTROY_HAZARDS -> 0
                MissionType.REACH_COMBO -> if (player.maxComboThisRun >= m.target) m.target else 0
            }
            missionProgress[m.id] = progress
            if (progress >= m.target) {
                gameData.completeMission(m.id); gameData.addCoins(m.reward)
                addFloatingText(screenWidth / 2, screenHeight * 0.2f, "MISSION COMPLETE! +${m.reward}", 0xFF4CAF50.toInt(), 1.8f)
                soundManager?.playMilestone()
            }
        }
    }

    // ---- Helpers ----
    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2; val dy = y1 - y2; return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    private fun handleCrash(now: Long, car: TrafficCar) {
        player.lives--; player.isInvincible = true; player.invincibleEndTime = now + Constants.INVINCIBILITY_AFTER_HIT_MS
        player.comboCount = 0; player.nosActive = false; player.distanceWithoutCrash = 0f
        player.speed = max(Constants.IDLE_SPEED, player.speed * 0.5f)
        spawnParticles((player.x + car.x) / 2f, (player.y + car.y) / 2f, Constants.PARTICLE_COUNT_CRASH, 0xFFFF5722.toInt())
        addFloatingText(player.x, player.y - player.height, "CRASH!", 0xFFFF1744.toInt(), 1.5f)
        triggerScreenShake(now); soundManager?.playCrash()
        if (player.lives <= 0) gameOver()
    }

    private fun activatePowerUp(pu: PowerUp, now: Long) {
        val name = when (pu.type) {
            PowerUpType.SHIELD -> { player.hasShield = true; player.shieldEndTime = now + Constants.SHIELD_DURATION_MS; "SHIELD!" }
            PowerUpType.MAGNET -> { player.hasMagnet = true; player.magnetEndTime = now + Constants.MAGNET_DURATION_MS; "MAGNET!" }
            PowerUpType.DOUBLE_SCORE -> { player.hasDoubleScore = true; player.doubleScoreEndTime = now + Constants.DOUBLE_SCORE_DURATION_MS; "2X SCORE!" }
            PowerUpType.EXTRA_LIFE -> { player.lives = min(player.lives + 1, Constants.MAX_LIVES); "+1 LIFE!" }
            PowerUpType.NITRO -> { player.nosAmount = Constants.NOS_MAX; "NOS FULL!" }
        }
        addFloatingText(pu.x, pu.y - 20f, name, pu.type.color, 1.3f)
    }

    private fun openMysteryBox(mb: MysteryBox, now: Long) {
        val roll = Random.nextFloat()
        when {
            roll < 0.3f -> { val b = (200..500).random().toLong(); player.coinScore += b; mysteryBoxReward = "+$b coins!"; addFloatingText(mb.x, mb.y - 30f, "+$b", 0xFFFFD700.toInt(), 1.5f) }
            roll < 0.5f -> { player.hasShield = true; player.shieldEndTime = now + Constants.SHIELD_DURATION_MS; mysteryBoxReward = "Shield!"; addFloatingText(mb.x, mb.y - 30f, "SHIELD!", 0xFF00E5FF.toInt(), 1.5f) }
            roll < 0.65f -> { player.hasMagnet = true; player.magnetEndTime = now + Constants.MAGNET_DURATION_MS; mysteryBoxReward = "Magnet!"; addFloatingText(mb.x, mb.y - 30f, "MAGNET!", 0xFFFF6F00.toInt(), 1.5f) }
            roll < 0.8f -> { player.nosAmount = Constants.NOS_MAX; mysteryBoxReward = "NOS Full!"; addFloatingText(mb.x, mb.y - 30f, "NOS FULL!", 0xFF2979FF.toInt(), 1.5f) }
            roll < 0.9f -> { player.hasDoubleScore = true; player.doubleScoreEndTime = now + Constants.DOUBLE_SCORE_DURATION_MS; mysteryBoxReward = "2x Score!"; addFloatingText(mb.x, mb.y - 30f, "2X SCORE!", 0xFFFFD700.toInt(), 1.5f) }
            else -> { if (player.lives < Constants.MAX_LIVES) { player.lives++; mysteryBoxReward = "Extra Life!"; addFloatingText(mb.x, mb.y - 30f, "+1 LIFE!", 0xFFE91E63.toInt(), 1.5f) }
                      else { val b = 1000L; player.coinScore += b; mysteryBoxReward = "+$b coins!"; addFloatingText(mb.x, mb.y - 30f, "+$b", 0xFFFFD700.toInt(), 1.5f) } }
        }
        mysteryBoxRewardTime = now
    }

    private fun spawnParticles(x: Float, y: Float, count: Int, baseColor: Int) {
        for (i in 0 until count) {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            val speed = 2f + Random.nextFloat() * 6f
            val r = ((baseColor shr 16) and 0xFF) + Random.nextInt(-30, 30)
            val g = ((baseColor shr 8) and 0xFF) + Random.nextInt(-30, 30)
            val b = (baseColor and 0xFF) + Random.nextInt(-30, 30)
            val color = (0xFF shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)
            particles.add(Particle(
                x = x + (Random.nextFloat() - 0.5f) * 20f, y = y + (Random.nextFloat() - 0.5f) * 20f,
                vx = kotlin.math.cos(angle) * speed, vy = kotlin.math.sin(angle) * speed,
                size = 3f + Random.nextFloat() * 8f, color = color, gravity = 0.15f,
                maxLifetime = Constants.PARTICLE_LIFETIME_MS * (0.5f + Random.nextFloat() * 0.5f)
            ))
        }
    }

    private fun spawnDriftSpark() {
        val side = if (player.steerAngle > 0) -1f else 1f
        driftSparks.add(DriftSpark(
            x = player.x + side * player.width * 0.4f, y = player.y + player.height * 0.4f,
            vx = side * (1f + Random.nextFloat() * 3f), vy = Random.nextFloat() * 2f,
            life = 0.5f + Random.nextFloat() * 0.5f,
            color = if (Random.nextBoolean()) 0xFFFFAB00.toInt() else 0xFFFF6D00.toInt()
        ))
    }

    fun addFloatingText(x: Float, y: Float, text: String, color: Int, scale: Float = 1f) {
        floatingTexts.add(FloatingText(x, y, text, color, System.currentTimeMillis(), scale = scale))
    }

    private fun triggerScreenShake(now: Long) { screenShakeEndTime = now + Constants.SCREEN_SHAKE_DURATION_MS }

    fun gameOver() {
        state = GameState.GAME_OVER
        val coinsEarned = player.coinsCollectedThisRun.toLong() * Constants.COIN_VALUE + (player.distanceScore * 0.05f).toLong()
        gameData.onGameEnd(player.score, player.distanceScore, coinsEarned, player.maxComboThisRun, player.maxSpeedReachedThisRun, player.distanceWithoutCrash)
        newAchievements = gameData.checkAndUnlockAchievements().toMutableList()
        if (currentMissions.all { gameData.isMissionCompleted(it.id) }) {
            gameData.missionDifficulty++; currentMissions = MissionBank.generateMissions(gameData.missionDifficulty)
        }
        soundManager?.playGameOver()
    }

    // ---- Controls API ----
    fun setSteering(input: Float) { player.steerInput = input.coerceIn(-1f, 1f) }
    fun setAccelerating(active: Boolean) { player.isAccelerating = active }
    fun setBraking(active: Boolean) { player.isBraking = active }
    fun activateNOS() { try { if (player.nosAmount >= Constants.NOS_MIN_TO_ACTIVATE) { player.nosActive = true; soundManager?.playBoost() } } catch (_: Exception) {} }
    fun deactivateNOS() { player.nosActive = false }
    fun pause() { if (state == GameState.PLAYING) state = GameState.PAUSED }
    fun resume() { if (state == GameState.PAUSED) state = GameState.PLAYING }

    fun selectCar(index: Int) { garageSelectedIndex = index.coerceIn(0, PlayerCarDef.ALL_CARS.size - 1) }
    fun buyCar(): Boolean {
        val car = PlayerCarDef.ALL_CARS[garageSelectedIndex]
        if (gameData.isCarUnlocked(car.id)) return false
        if (gameData.spendCoins(car.price)) { gameData.unlockCar(car.id); return true }
        return false
    }
    fun equipCar() { val car = PlayerCarDef.ALL_CARS[garageSelectedIndex]; if (gameData.isCarUnlocked(car.id)) gameData.selectedCarId = car.id }
    fun upgradeStat(stat: String): Boolean {
        val car = PlayerCarDef.ALL_CARS[garageSelectedIndex]; if (!gameData.isCarUnlocked(car.id)) return false
        val lvl = gameData.getCarUpgradeLevel(car.id, stat); if (lvl >= 5) return false
        if (gameData.spendCoins(gameData.getUpgradeCost(lvl))) { gameData.setCarUpgradeLevel(car.id, stat, lvl + 1); return true }
        return false
    }
    fun release() { soundManager?.release() }
}
