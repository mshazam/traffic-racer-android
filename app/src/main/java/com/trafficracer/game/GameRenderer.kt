package com.trafficracer.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class GameRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
    }
    private val path = Path()

    fun render(canvas: Canvas, world: GameWorld) {
        canvas.save()
        canvas.translate(world.screenShakeX, world.screenShakeY)

        val env = world.currentEnvironment
        drawSky(canvas, world, env)
        drawGrass(canvas, world, env)
        drawRoad(canvas, world, env)
        drawRoadMarkings(canvas, world, env)
        drawScenery(canvas, world, env)
        drawTireTrails(canvas, world)
        drawHazards(canvas, world)
        drawMysteryBoxes(canvas, world)
        drawCoins(canvas, world)
        drawPowerUps(canvas, world)
        drawTraffic(canvas, world)
        drawDriftSparks(canvas, world)
        drawPlayer(canvas, world)
        drawParticles(canvas, world)
        drawWeather(canvas, world)
        drawFloatingTexts(canvas, world)

        if (world.player.getEffectiveSpeed() > Constants.SPEED_BLUR_THRESHOLD) drawSpeedLines(canvas, world)
        if (world.slowMoFactor < 0.9f) drawSlowMoOverlay(canvas, world)

        canvas.restore()

        when (world.state) {
            GameState.PLAYING -> { drawHUD(canvas, world); drawControls(canvas, world) }
            GameState.START_SCREEN -> drawStartScreen(canvas, world)
            GameState.GARAGE -> drawGarageScreen(canvas, world)
            GameState.MISSIONS_SCREEN -> drawMissionsScreen(canvas, world)
            GameState.GAME_OVER -> drawGameOverScreen(canvas, world)
            GameState.PAUSED -> { drawHUD(canvas, world); drawControls(canvas, world); drawPauseOverlay(canvas, world) }
        }
    }

    // ---- Environment drawing ----
    private fun drawSky(canvas: Canvas, world: GameWorld, env: Environment) {
        val top = blendColors(env.skyTopDay, env.skyTopNight, world.nightFactor)
        val bot = blendColors(env.skyBottomDay, env.skyBottomNight, world.nightFactor)
        paint.shader = LinearGradient(0f, 0f, 0f, world.screenHeight, top, bot, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, world.screenWidth, world.screenHeight, paint)
        paint.shader = null
    }

    private fun drawGrass(canvas: Canvas, world: GameWorld, env: Environment) {
        val gc = blendColors(env.grassColor, darkenColor(env.grassColor, 0.4f), world.nightFactor)
        val gl = blendColors(env.grassLight, darkenColor(env.grassLight, 0.4f), world.nightFactor)
        paint.color = gc
        canvas.drawRect(0f, 0f, world.roadLeft - world.shoulderWidth, world.screenHeight, paint)
        canvas.drawRect(world.roadRight + world.shoulderWidth, 0f, world.screenWidth, world.screenHeight, paint)
        paint.color = gl
        val sw = world.screenWidth * 0.015f
        var x = 0f
        while (x < world.roadLeft - world.shoulderWidth) { canvas.drawRect(x, 0f, x + sw, world.screenHeight, paint); x += sw * 3f }
        x = world.roadRight + world.shoulderWidth
        while (x < world.screenWidth) { canvas.drawRect(x, 0f, x + sw, world.screenHeight, paint); x += sw * 3f }
    }

    private fun drawRoad(canvas: Canvas, world: GameWorld, env: Environment) {
        val rc = blendColors(env.roadColor, darkenColor(env.roadColor, 0.3f), world.nightFactor)
        val sc = blendColors(env.shoulderColor, darkenColor(env.shoulderColor, 0.3f), world.nightFactor)
        paint.color = sc
        canvas.drawRect(world.roadLeft - world.shoulderWidth, 0f, world.roadLeft, world.screenHeight, paint)
        canvas.drawRect(world.roadRight, 0f, world.roadRight + world.shoulderWidth, world.screenHeight, paint)
        paint.color = rc
        canvas.drawRect(world.roadLeft, 0f, world.roadRight, world.screenHeight, paint)
        paint.color = Color.WHITE; paint.strokeWidth = world.screenWidth * 0.008f
        canvas.drawLine(world.roadLeft, 0f, world.roadLeft, world.screenHeight, paint)
        canvas.drawLine(world.roadRight, 0f, world.roadRight, world.screenHeight, paint)
        paint.color = Color.YELLOW; paint.strokeWidth = world.screenWidth * 0.006f
        canvas.drawLine(world.roadLeft - world.shoulderWidth / 2, 0f, world.roadLeft - world.shoulderWidth / 2, world.screenHeight, paint)
        canvas.drawLine(world.roadRight + world.shoulderWidth / 2, 0f, world.roadRight + world.shoulderWidth / 2, world.screenHeight, paint)
    }

    private fun drawRoadMarkings(canvas: Canvas, world: GameWorld, env: Environment) {
        val mw = world.screenWidth * Constants.ROAD_MARKING_WIDTH_RATIO
        val ml = world.screenHeight * Constants.ROAD_MARKING_LENGTH_RATIO
        paint.color = blendColors(Color.WHITE, 0xFFCCCCCC.toInt(), world.nightFactor)
        for (i in 1 until Constants.NUM_LANES) {
            val lx = world.roadLeft + world.laneWidth * i
            for (m in world.roadMarkings) canvas.drawRoundRect(RectF(lx - mw / 2, m.y, lx + mw / 2, m.y + ml), mw, mw, paint)
        }
    }

    private fun drawScenery(canvas: Canvas, world: GameWorld, env: Environment) {
        for (obj in world.sceneryObjects) drawSceneryItem(canvas, obj, world, env)
    }

    private fun drawSceneryItem(canvas: Canvas, obj: SceneryObject, world: GameWorld, env: Environment) {
        val bw = world.screenWidth * Constants.TREE_WIDTH_RATIO * obj.scale
        when (env.sceneryType) {
            2 -> { // Desert: cactus
                paint.color = blendColors(0xFF2E7D32.toInt(), 0xFF1B3A1B.toInt(), world.nightFactor)
                canvas.drawRoundRect(RectF(obj.x - bw * 0.08f, obj.y - bw * 0.8f, obj.x + bw * 0.08f, obj.y + bw * 0.1f), bw * 0.08f, bw * 0.08f, paint)
                canvas.drawRoundRect(RectF(obj.x - bw * 0.3f, obj.y - bw * 0.5f, obj.x - bw * 0.15f, obj.y - bw * 0.1f), bw * 0.05f, bw * 0.05f, paint)
                canvas.drawRoundRect(RectF(obj.x + bw * 0.15f, obj.y - bw * 0.6f, obj.x + bw * 0.3f, obj.y - bw * 0.2f), bw * 0.05f, bw * 0.05f, paint)
            }
            3 -> { // Snow: pine tree
                paint.color = blendColors(0xFF5D4037.toInt(), 0xFF3E2723.toInt(), world.nightFactor)
                canvas.drawRect(obj.x - bw * 0.06f, obj.y, obj.x + bw * 0.06f, obj.y + bw * 0.5f, paint)
                paint.color = blendColors(0xFF1B5E20.toInt(), 0xFF0D2E10.toInt(), world.nightFactor)
                for (i in 0..2) {
                    val ty = obj.y - bw * (0.2f + i * 0.25f)
                    val tw = bw * (0.5f - i * 0.1f)
                    path.reset(); path.moveTo(obj.x, ty - bw * 0.3f); path.lineTo(obj.x - tw, ty + bw * 0.1f); path.lineTo(obj.x + tw, ty + bw * 0.1f); path.close()
                    canvas.drawPath(path, paint)
                }
                paint.color = 0x55FFFFFF; canvas.drawCircle(obj.x - bw * 0.15f, obj.y - bw * 0.3f, bw * 0.05f, paint)
                canvas.drawCircle(obj.x + bw * 0.1f, obj.y - bw * 0.5f, bw * 0.04f, paint)
            }
            1 -> { // Highway: bush
                paint.color = blendColors(0xFF388E3C.toInt(), 0xFF1E4420.toInt(), world.nightFactor)
                canvas.drawCircle(obj.x, obj.y, bw * 0.35f, paint)
                paint.color = blendColors(0xFF43A047.toInt(), 0xFF2E5A2E.toInt(), world.nightFactor)
                canvas.drawCircle(obj.x - bw * 0.15f, obj.y - bw * 0.1f, bw * 0.2f, paint)
            }
            else -> { // City: tree
                paint.color = blendColors(0xFF5D4037.toInt(), 0xFF3E2723.toInt(), world.nightFactor)
                canvas.drawRect(obj.x - bw * 0.1f, obj.y, obj.x + bw * 0.1f, obj.y + bw * 0.8f, paint)
                paint.color = blendColors(0xFF2E7D32.toInt(), 0xFF1B3A1B.toInt(), world.nightFactor)
                path.reset(); path.moveTo(obj.x, obj.y - bw * 0.6f); path.lineTo(obj.x - bw * 0.45f, obj.y + bw * 0.15f); path.lineTo(obj.x + bw * 0.45f, obj.y + bw * 0.15f); path.close()
                canvas.drawPath(path, paint)
            }
        }
    }

    // ---- Player car with NFS-style rotation ----
    private fun drawPlayer(canvas: Canvas, world: GameWorld) {
        val p = world.player
        if (p.isInvincible && (System.currentTimeMillis() / 100) % 3 == 0L) return

        canvas.save()
        val visualAngle = p.steerAngle * -8f + p.driftAngle * -15f
        canvas.rotate(visualAngle, p.x, p.y)

        drawCarBody(canvas, p.x, p.y, p.width, p.height, p.carDef.color, true, p.brakeLightIntensity)

        canvas.restore()

        if (p.hasShield) drawShieldEffect(canvas, p)
        if (p.nosActive) drawNOSFlame(canvas, p)
    }

    private fun drawCarBody(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float, color: Int, isPlayer: Boolean, brakeLight: Float = 0f) {
        val l = cx - w / 2; val t = cy - h / 2; val r = cx + w / 2; val b = cy + h / 2; val cr = w * 0.15f

        // Wheels
        paint.color = darkenColor(color, 0.3f)
        canvas.drawRoundRect(RectF(l - w * 0.08f, t + h * 0.08f, l + w * 0.02f, t + h * 0.28f), 2f, 2f, paint)
        canvas.drawRoundRect(RectF(r - w * 0.02f, t + h * 0.08f, r + w * 0.08f, t + h * 0.28f), 2f, 2f, paint)
        canvas.drawRoundRect(RectF(l - w * 0.08f, t + h * 0.7f, l + w * 0.02f, t + h * 0.9f), 2f, 2f, paint)
        canvas.drawRoundRect(RectF(r - w * 0.02f, t + h * 0.7f, r + w * 0.08f, t + h * 0.9f), 2f, 2f, paint)

        // Body
        paint.color = color; canvas.drawRoundRect(RectF(l, t, r, b), cr, cr, paint)
        paint.color = lightenColor(color, 0.15f)
        canvas.drawRoundRect(RectF(l + w * 0.08f, t + w * 0.05f, r - w * 0.08f, t + h * 0.15f), cr * 0.5f, cr * 0.5f, paint)

        // Windshield
        paint.color = if (isPlayer) 0xFF64B5F6.toInt() else 0xFF90CAF9.toInt()
        canvas.drawRoundRect(RectF(l + w * 0.12f, t + h * 0.18f, r - w * 0.12f, t + h * 0.35f), cr * 0.3f, cr * 0.3f, paint)
        canvas.drawRoundRect(RectF(l + w * 0.12f, t + h * 0.62f, r - w * 0.12f, t + h * 0.75f), cr * 0.3f, cr * 0.3f, paint)

        if (isPlayer) {
            // Headlights
            paint.color = 0xFFFFEE58.toInt()
            canvas.drawRoundRect(RectF(l + w * 0.1f, t, l + w * 0.3f, t + h * 0.04f), 2f, 2f, paint)
            canvas.drawRoundRect(RectF(r - w * 0.3f, t, r - w * 0.1f, t + h * 0.04f), 2f, 2f, paint)
            // Brake lights with dynamic intensity
            val brakeR = (0xEF + (brakeLight * 16).toInt()).coerceIn(0, 255)
            val brakeAlpha = (0x80 + (brakeLight * 0x7F).toInt()).coerceIn(0, 255)
            paint.color = (brakeAlpha shl 24) or (brakeR shl 16) or (0x30 shl 8) or 0x30
            val blw = w * (0.2f + brakeLight * 0.05f)
            canvas.drawRoundRect(RectF(l + w * 0.1f, b - h * 0.05f, l + w * 0.1f + blw, b), 2f, 2f, paint)
            canvas.drawRoundRect(RectF(r - w * 0.1f - blw, b - h * 0.05f, r - w * 0.1f, b), 2f, 2f, paint)
        }
    }

    private fun drawShieldEffect(canvas: Canvas, p: PlayerCar) {
        val phase = (System.currentTimeMillis() % 1000) / 1000f
        val radius = p.width * 0.8f + sin(phase * Math.PI.toFloat() * 2f) * 5f
        paint.color = ((180 + (sin(phase * Math.PI.toFloat() * 2f) * 40).toInt()).coerceIn(0, 255) shl 24) or 0x00E5FF
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 4f
        canvas.drawCircle(p.x, p.y, radius, paint)
        paint.style = Paint.Style.FILL; paint.color = 0x2000E5FF
        canvas.drawCircle(p.x, p.y, radius, paint)
    }

    private fun drawNOSFlame(canvas: Canvas, p: PlayerCar) {
        val t = System.currentTimeMillis()
        val flicker = sin(t * 0.025f) * 0.3f + 0.7f
        val fh = p.height * 0.5f * flicker
        for (i in 0 until 3) {
            val ox = (i - 1) * p.width * 0.15f; val a = (220 - i * 40).coerceIn(0, 255)
            paint.color = (a shl 24) or 0x2979FF
            path.reset()
            path.moveTo(p.x + ox - p.width * 0.08f, p.y + p.height / 2)
            path.lineTo(p.x + ox + p.width * 0.08f, p.y + p.height / 2)
            path.lineTo(p.x + ox + sin(t * 0.02f + i) * 3f, p.y + p.height / 2 + fh * (1f - i * 0.2f))
            path.close(); canvas.drawPath(path, paint)
        }
        paint.color = 0xCC00E5FF.toInt()
        path.reset()
        path.moveTo(p.x - p.width * 0.05f, p.y + p.height / 2)
        path.lineTo(p.x + p.width * 0.05f, p.y + p.height / 2)
        path.lineTo(p.x, p.y + p.height / 2 + fh * 0.6f)
        path.close(); canvas.drawPath(path, paint)
    }

    // ---- Traffic, items, effects ----
    private fun drawTraffic(canvas: Canvas, world: GameWorld) {
        for (car in world.trafficCars) {
            canvas.save()
            if (car.isOncoming) canvas.rotate(180f, car.x, car.y)
            drawCarBody(canvas, car.x, car.y, car.width, car.height, car.color, false)
            canvas.restore()
        }
    }

    private fun drawCoins(canvas: Canvas, world: GameWorld) {
        for (c in world.coins) {
            if (c.collected) {
                val s = 1f - c.collectAnimProgress; val a = (255 * s).toInt().coerceIn(0, 255)
                drawCoin(canvas, c.x, c.y - c.collectAnimProgress * 30f, c.size * s, c.rotation, a)
            } else drawCoin(canvas, c.x, c.y, c.size, c.rotation, 255)
        }
    }

    private fun drawCoin(canvas: Canvas, x: Float, y: Float, size: Float, rot: Float, alpha: Int) {
        val sx = abs(cos(Math.toRadians(rot.toDouble()))).toFloat() * 0.5f + 0.5f
        paint.color = (alpha shl 24) or 0xFFD700
        canvas.drawOval(RectF(x - size * sx, y - size, x + size * sx, y + size), paint)
        paint.color = (alpha shl 24) or 0xFFC107
        canvas.drawOval(RectF(x - size * sx * 0.7f, y - size * 0.7f, x + size * sx * 0.7f, y + size * 0.7f), paint)
        if (sx > 0.6f) { textPaint.color = (alpha shl 24) or 0xFFD700; textPaint.textSize = size * 1.1f; textPaint.textAlign = Paint.Align.CENTER; canvas.drawText("$", x, y + size * 0.35f, textPaint) }
    }

    private fun drawPowerUps(canvas: Canvas, world: GameWorld) {
        for (pu in world.powerUps) {
            val pulse = 1f + sin(pu.pulsePhase) * 0.15f; val s = pu.size * pulse
            val ga = ((sin(pu.pulsePhase) * 0.3f + 0.3f) * 255).toInt().coerceIn(0, 255)
            paint.color = (ga shl 24) or (pu.type.color and 0x00FFFFFF); canvas.drawCircle(pu.x, pu.y, s * 1.3f, paint)
            paint.color = pu.type.color; canvas.drawCircle(pu.x, pu.y, s * 0.7f, paint)
            paint.color = lightenColor(pu.type.color, 0.4f); canvas.drawCircle(pu.x, pu.y, s * 0.4f, paint)
            textPaint.color = Color.WHITE; textPaint.textSize = s * 0.7f; textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(when (pu.type) { PowerUpType.SHIELD -> "S"; PowerUpType.MAGNET -> "M"; PowerUpType.DOUBLE_SCORE -> "2x"; PowerUpType.EXTRA_LIFE -> "+"; PowerUpType.NITRO -> "N" }, pu.x, pu.y + s * 0.2f, textPaint)
        }
    }

    private fun drawHazards(canvas: Canvas, world: GameWorld) {
        for (h in world.hazards) {
            when (h.type) {
                HazardType.OIL_SLICK -> { paint.color = 0x99333333.toInt(); canvas.drawOval(RectF(h.x - h.size, h.y - h.size * 0.5f, h.x + h.size, h.y + h.size * 0.5f), paint); paint.color = 0x44666666; canvas.drawOval(RectF(h.x - h.size * 0.6f, h.y - h.size * 0.3f, h.x + h.size * 0.6f, h.y + h.size * 0.3f), paint) }
                HazardType.CONE -> { paint.color = 0xFFFF6D00.toInt(); path.reset(); path.moveTo(h.x, h.y - h.size * 0.8f); path.lineTo(h.x - h.size * 0.4f, h.y + h.size * 0.4f); path.lineTo(h.x + h.size * 0.4f, h.y + h.size * 0.4f); path.close(); canvas.drawPath(path, paint); paint.color = Color.WHITE; canvas.drawRect(h.x - h.size * 0.3f, h.y - h.size * 0.1f, h.x + h.size * 0.3f, h.y + h.size * 0.05f, paint) }
                HazardType.POTHOLE -> { paint.color = 0xFF1A1A1A.toInt(); canvas.drawOval(RectF(h.x - h.size * 0.6f, h.y - h.size * 0.4f, h.x + h.size * 0.6f, h.y + h.size * 0.4f), paint); paint.color = 0xFF333333.toInt(); canvas.drawOval(RectF(h.x - h.size * 0.5f, h.y - h.size * 0.3f, h.x + h.size * 0.5f, h.y + h.size * 0.3f), paint) }
            }
        }
    }

    private fun drawMysteryBoxes(canvas: Canvas, world: GameWorld) {
        for (mb in world.mysteryBoxes) {
            val bounce = sin(mb.bouncePhase) * 4f; val s = mb.size
            canvas.save(); canvas.translate(0f, bounce)
            paint.color = 0xFFE040FB.toInt(); canvas.drawRoundRect(RectF(mb.x - s / 2, mb.y - s / 2, mb.x + s / 2, mb.y + s / 2), s * 0.15f, s * 0.15f, paint)
            paint.color = 0xFFCE93D8.toInt(); canvas.drawRoundRect(RectF(mb.x - s * 0.35f, mb.y - s * 0.35f, mb.x + s * 0.35f, mb.y + s * 0.35f), s * 0.1f, s * 0.1f, paint)
            textPaint.color = Color.WHITE; textPaint.textSize = s * 0.5f; textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("?", mb.x, mb.y + s * 0.15f, textPaint)
            canvas.restore()
        }
    }

    private fun drawTireTrails(canvas: Canvas, world: GameWorld) {
        for (t in world.tireTrails) { paint.color = ((t.alpha * 80).toInt().coerceIn(0, 255) shl 24) or 0x222222; canvas.drawRect(t.x - t.width / 2, t.y, t.x + t.width / 2, t.y + 6f, paint) }
    }

    private fun drawDriftSparks(canvas: Canvas, world: GameWorld) {
        for (s in world.driftSparks) { val a = (s.life * 255).toInt().coerceIn(0, 255); paint.color = (a shl 24) or (s.color and 0x00FFFFFF); canvas.drawCircle(s.x, s.y, 2f + s.life * 3f, paint) }
    }

    private fun drawParticles(canvas: Canvas, world: GameWorld) {
        for (p in world.particles) { val a = (p.alpha * 255).toInt().coerceIn(0, 255); paint.color = (a shl 24) or (p.color and 0x00FFFFFF); canvas.drawCircle(p.x, p.y, p.size, paint) }
    }

    private fun drawWeather(canvas: Canvas, world: GameWorld) {
        if (!world.currentEnvironment.hasWeather) return
        for (wp in world.weatherParticles) {
            val a = (wp.alpha * 200).toInt().coerceIn(0, 255)
            if (world.currentEnvironment.weatherType == WeatherType.SNOW) {
                paint.color = (a shl 24) or 0xFFFFFF; canvas.drawCircle(wp.x, wp.y, wp.size, paint)
            } else {
                paint.color = (a shl 24) or 0xAABBCC; paint.strokeWidth = 1.5f; canvas.drawLine(wp.x, wp.y, wp.x + wp.windOffset * 2f, wp.y + wp.size * 3f, paint)
            }
        }
    }

    private fun drawFloatingTexts(canvas: Canvas, world: GameWorld) {
        val now = System.currentTimeMillis()
        for (ft in world.floatingTexts) {
            val prog = ft.getProgress(now); val alpha = ((1f - prog) * 255).toInt().coerceIn(0, 255)
            val scale = ft.scale * (1f + prog * 0.3f)
            textPaint.textAlign = Paint.Align.CENTER; textPaint.textSize = world.screenHeight * 0.025f * scale
            textPaint.color = (alpha shl 24) or (ft.color and 0x00FFFFFF)
            canvas.drawText(ft.text, ft.x, ft.y, textPaint)
        }
    }

    private fun drawSpeedLines(canvas: Canvas, world: GameWorld) {
        val intensity = ((world.player.getEffectiveSpeed() - Constants.SPEED_BLUR_THRESHOLD) / (Constants.MAX_SPEED - Constants.SPEED_BLUR_THRESHOLD)).coerceIn(0f, 1f)
        val alpha = (intensity * 80).toInt(); paint.color = (alpha shl 24) or 0xFFFFFF; paint.strokeWidth = 2f
        val t = System.currentTimeMillis()
        for (i in 0 until (10 + (intensity * 15).toInt())) {
            val seed = i * 1337L; val x = world.roadLeft + (seed % world.roadWidth.toLong()).toFloat()
            val baseY = ((t * (0.5f + intensity) + seed) % world.screenHeight.toLong()).toFloat()
            canvas.drawLine(x, baseY, x, baseY + 20f + intensity * 40f + (seed % 30), paint)
        }
    }

    private fun drawSlowMoOverlay(canvas: Canvas, world: GameWorld) {
        val intensity = (1f - world.slowMoFactor).coerceIn(0f, 1f)
        val alpha = (intensity * 60).toInt()
        paint.color = (alpha shl 24) or 0x001122
        canvas.drawRect(0f, 0f, world.screenWidth, world.screenHeight * 0.15f, paint)
        canvas.drawRect(0f, world.screenHeight * 0.85f, world.screenWidth, world.screenHeight, paint)
        canvas.drawRect(0f, 0f, world.screenWidth * 0.05f, world.screenHeight, paint)
        canvas.drawRect(world.screenWidth * 0.95f, 0f, world.screenWidth, world.screenHeight, paint)
    }

    // ---- NFS-Style HUD ----
    private fun drawHUD(canvas: Canvas, world: GameWorld) {
        val p = world.player; val hh = world.screenHeight * 0.065f; val pad = world.screenWidth * 0.03f

        // Top bar
        paint.color = 0xBB000000.toInt(); canvas.drawRect(0f, 0f, world.screenWidth, hh, paint)

        // Score
        textPaint.textAlign = Paint.Align.LEFT; textPaint.color = 0xAAFFFFFF.toInt(); textPaint.textSize = hh * 0.32f
        canvas.drawText("SCORE", pad, hh * 0.38f, textPaint)
        textPaint.color = 0xFFFFD700.toInt(); textPaint.textSize = hh * 0.48f
        canvas.drawText("${p.score}", pad, hh * 0.82f, textPaint)

        // Coins
        textPaint.textAlign = Paint.Align.CENTER; textPaint.color = 0xFFFFD700.toInt(); textPaint.textSize = hh * 0.4f
        canvas.drawText("${world.gameData.totalCoins + p.coinsCollectedThisRun * Constants.COIN_VALUE}", world.screenWidth * 0.5f, hh * 0.65f, textPaint)

        // Lives
        val hs = hh * 0.25f; val startX = world.screenWidth - pad - (hs * 2.2f * Constants.MAX_LIVES)
        for (i in 0 until Constants.MAX_LIVES) {
            val hx = startX + i * hs * 2.2f + hs; val hy = hh * 0.55f
            paint.color = if (i < p.lives) 0xFFEF5350.toInt() else 0x44FFFFFF; drawHeart(canvas, hx, hy, hs)
        }

        // Combo
        if (p.comboCount > 1) {
            textPaint.textAlign = Paint.Align.CENTER; textPaint.textSize = world.screenHeight * 0.025f
            textPaint.color = if (p.comboCount >= 5) 0xFFFF6D00.toInt() else 0xFF00E676.toInt()
            canvas.drawText("COMBO x${p.comboCount}!", world.screenWidth / 2, hh + world.screenHeight * 0.035f, textPaint)
        }

        // Active power-ups
        drawActivePowerUps(canvas, world, hh)

        // Environment name (fading)
        // Speedometer
        drawSpeedometer(canvas, world)

        // Pause button
        val ps = hh * 0.4f; val px = world.screenWidth - pad - ps * 1.5f; val py = hh * 0.5f
        paint.color = 0x55FFFFFF; canvas.drawRoundRect(RectF(px - ps, py - ps * 0.6f, px + ps, py + ps * 0.6f), 4f, 4f, paint)
        paint.color = Color.WHITE
        canvas.drawRect(px - ps * 0.25f, py - ps * 0.3f, px - ps * 0.05f, py + ps * 0.3f, paint)
        canvas.drawRect(px + ps * 0.05f, py - ps * 0.3f, px + ps * 0.25f, py + ps * 0.3f, paint)
    }

    private fun drawSpeedometer(canvas: Canvas, world: GameWorld) {
        val p = world.player; val size = world.screenWidth * Constants.SPEEDO_SIZE_RATIO
        val cx = world.screenWidth - size * 1.3f; val cy = world.screenHeight - world.screenHeight * Constants.HUD_CONTROLS_HEIGHT_RATIO - size * 1.5f

        // Background arc
        paint.color = 0x88000000.toInt(); paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, size, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = size * 0.08f; paint.color = 0xFF333333.toInt()
        canvas.drawArc(RectF(cx - size * 0.8f, cy - size * 0.8f, cx + size * 0.8f, cy + size * 0.8f), 135f, 270f, false, paint)

        // Speed arc (colored by speed)
        val speedRatio = (p.getEffectiveSpeed() / (Constants.MAX_SPEED * p.carDef.baseSpeed)).coerceIn(0f, 1f)
        val arcColor = when {
            speedRatio > 0.8f -> 0xFFFF1744.toInt()
            speedRatio > 0.5f -> 0xFFFF9100.toInt()
            else -> 0xFF00E676.toInt()
        }
        paint.color = arcColor
        canvas.drawArc(RectF(cx - size * 0.8f, cy - size * 0.8f, cx + size * 0.8f, cy + size * 0.8f), 135f, 270f * speedRatio, false, paint)
        paint.style = Paint.Style.FILL

        // Needle
        val needleAngle = 135f + 270f * speedRatio
        val rad = Math.toRadians(needleAngle.toDouble())
        paint.color = Color.WHITE; paint.strokeWidth = 3f; paint.style = Paint.Style.STROKE
        canvas.drawLine(cx, cy, cx + (cos(rad) * size * 0.7f).toFloat(), cy + (sin(rad) * size * 0.7f).toFloat(), paint)
        paint.style = Paint.Style.FILL; paint.color = Color.WHITE; canvas.drawCircle(cx, cy, size * 0.08f, paint)

        // Speed text
        textPaint.textAlign = Paint.Align.CENTER; textPaint.color = Color.WHITE; textPaint.textSize = size * 0.45f
        canvas.drawText("${p.getSpeedKmh()}", cx, cy + size * 0.35f, textPaint)
        textPaint.textSize = size * 0.2f; textPaint.color = 0xAAFFFFFF.toInt()
        canvas.drawText("km/h", cx, cy + size * 0.55f, textPaint)

        // NOS gauge (below speedometer)
        val nosBarW = size * 1.6f; val nosBarH = size * 0.18f
        val nosX = cx - nosBarW / 2; val nosY = cy + size * 0.75f
        paint.color = 0x66000000; canvas.drawRoundRect(RectF(nosX, nosY, nosX + nosBarW, nosY + nosBarH), 4f, 4f, paint)
        val nosRatio = p.nosAmount / Constants.NOS_MAX
        paint.color = if (p.nosActive) 0xFF00E5FF.toInt() else 0xFF2979FF.toInt()
        canvas.drawRoundRect(RectF(nosX + 2, nosY + 2, nosX + 2 + (nosBarW - 4) * nosRatio, nosY + nosBarH - 2), 3f, 3f, paint)
        textPaint.textSize = nosBarH * 0.7f; textPaint.color = Color.WHITE
        canvas.drawText("NOS", cx, nosY + nosBarH * 0.75f, textPaint)
    }

    // ---- NFS On-Screen Controls ----
    fun drawControls(canvas: Canvas, world: GameWorld) {
        val sw = world.screenWidth; val sh = world.screenHeight
        val controlsTop = sh * (1f - Constants.HUD_CONTROLS_HEIGHT_RATIO)
        val controlsH = sh * Constants.HUD_CONTROLS_HEIGHT_RATIO

        // Dim control area background
        paint.color = 0x22000000; canvas.drawRect(0f, controlsTop, sw, sh, paint)

        // LEFT: Brake pedal
        val brakeW = sw * Constants.PEDAL_WIDTH_RATIO
        val brakeH = controlsH * 0.75f
        val brakeX = sw * 0.02f; val brakeY = controlsTop + (controlsH - brakeH) / 2

        paint.color = if (world.player.isBraking) 0xCCFF1744.toInt() else 0x66FF5252.toInt()
        canvas.drawRoundRect(RectF(brakeX, brakeY, brakeX + brakeW, brakeY + brakeH), 12f, 12f, paint)
        textPaint.textAlign = Paint.Align.CENTER; textPaint.textSize = brakeH * 0.18f; textPaint.color = Color.WHITE
        canvas.drawText("BRAKE", brakeX + brakeW / 2, brakeY + brakeH * 0.55f, textPaint)

        // RIGHT: Gas pedal
        val gasW = sw * Constants.PEDAL_WIDTH_RATIO
        val gasH = controlsH * 0.75f
        val gasX = sw - sw * 0.02f - gasW; val gasY = controlsTop + (controlsH - gasH) / 2

        paint.color = if (world.player.isAccelerating) 0xCC00E676.toInt() else 0x6669F0AE.toInt()
        canvas.drawRoundRect(RectF(gasX, gasY, gasX + gasW, gasY + gasH), 12f, 12f, paint)
        textPaint.textSize = gasH * 0.18f
        canvas.drawText("GAS", gasX + gasW / 2, gasY + gasH * 0.55f, textPaint)

        // CENTER: Steering zone with visual indicator
        val steerLeft = brakeX + brakeW + sw * 0.03f
        val steerRight = gasX - sw * 0.03f
        val steerCx = (steerLeft + steerRight) / 2
        val steerCy = controlsTop + controlsH / 2
        val steerRadius = controlsH * 0.32f

        // Steering wheel outline
        paint.color = 0x44FFFFFF; paint.style = Paint.Style.STROKE; paint.strokeWidth = 4f
        canvas.drawCircle(steerCx, steerCy, steerRadius, paint)
        paint.style = Paint.Style.FILL

        // Steering indicator
        val steerX = steerCx + world.player.steerAngle / Constants.MAX_STEER_ANGLE * steerRadius * 0.8f
        paint.color = 0xCCFFFFFF.toInt()
        canvas.drawCircle(steerX, steerCy, steerRadius * 0.25f, paint)

        // Steering arrows
        textPaint.textSize = controlsH * 0.2f; textPaint.color = 0x88FFFFFF.toInt()
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("◀", steerLeft + 20f, steerCy + controlsH * 0.07f, textPaint)
        canvas.drawText("▶", steerRight - 20f, steerCy + controlsH * 0.07f, textPaint)

        // NOS button (between gas and steering)
        val nosSize = sw * Constants.NOS_BUTTON_SIZE_RATIO
        val nosCx = gasX - nosSize * 0.8f; val nosCy = controlsTop + controlsH * 0.35f
        val nosActive = world.player.nosActive
        paint.color = if (nosActive) 0xFF00E5FF.toInt() else if (world.player.nosAmount >= Constants.NOS_MIN_TO_ACTIVATE) 0x992979FF.toInt() else 0x44555555.toInt()
        canvas.drawCircle(nosCx, nosCy, nosSize, paint)
        if (nosActive) {
            paint.color = 0x3300E5FF; canvas.drawCircle(nosCx, nosCy, nosSize * 1.3f, paint)
        }
        textPaint.color = Color.WHITE; textPaint.textSize = nosSize * 0.55f
        canvas.drawText("NOS", nosCx, nosCy + nosSize * 0.18f, textPaint)
    }

    // Returns control hit areas for touch handling
    fun getControlAreas(world: GameWorld): ControlAreas {
        val sw = world.screenWidth; val sh = world.screenHeight
        val ct = sh * (1f - Constants.HUD_CONTROLS_HEIGHT_RATIO); val ch = sh * Constants.HUD_CONTROLS_HEIGHT_RATIO
        val bw = sw * Constants.PEDAL_WIDTH_RATIO; val bh = ch * 0.75f
        val bx = sw * 0.02f; val by = ct + (ch - bh) / 2
        val gw = sw * Constants.PEDAL_WIDTH_RATIO; val gh = ch * 0.75f
        val gx = sw - sw * 0.02f - gw; val gy = ct + (ch - gh) / 2
        val sl = bx + bw + sw * 0.03f; val sr = gx - sw * 0.03f
        val ns = sw * Constants.NOS_BUTTON_SIZE_RATIO
        val nx = gx - ns * 0.8f; val ny = ct + ch * 0.35f
        val hh = sh * 0.065f
        val ps = hh * 0.4f; val px = sw - sw * 0.03f - ps * 1.5f
        return ControlAreas(
            brake = RectF(bx, by, bx + bw, by + bh),
            gas = RectF(gx, gy, gx + gw, gy + gh),
            steerLeft = sl, steerRight = sr, steerCy = ct + ch / 2,
            nosCx = nx, nosCy = ny, nosRadius = ns * 1.3f,
            pauseArea = RectF(px - ps * 1.5f, 0f, px + ps * 1.5f, hh)
        )
    }

    private fun drawActivePowerUps(canvas: Canvas, world: GameWorld, hh: Float) {
        val p = world.player; val now = System.currentTimeMillis(); val is2 = world.screenHeight * 0.018f
        var oy = hh + world.screenHeight * 0.05f; val x = world.screenWidth * 0.05f
        val active = mutableListOf<Pair<String, Float>>()
        if (p.hasShield) active.add("SHIELD" to ((p.shieldEndTime - now) / 1000f))
        if (p.hasMagnet) active.add("MAGNET" to ((p.magnetEndTime - now) / 1000f))
        if (p.hasDoubleScore) active.add("2x SCORE" to ((p.doubleScoreEndTime - now) / 1000f))
        for ((name, timeLeft) in active) {
            val bw2 = world.screenWidth * 0.18f; val bh2 = is2 * 0.8f
            paint.color = 0x55000000; canvas.drawRoundRect(RectF(x - 2f, oy - bh2 / 2 - 2f, x + bw2 + 2f, oy + bh2 / 2 + 2f), 4f, 4f, paint)
            val maxT = when (name) { "SHIELD" -> Constants.SHIELD_DURATION_MS / 1000f; "MAGNET" -> Constants.MAGNET_DURATION_MS / 1000f; else -> Constants.DOUBLE_SCORE_DURATION_MS / 1000f }
            val prog = (timeLeft / maxT).coerceIn(0f, 1f)
            paint.color = when (name) { "SHIELD" -> 0xFF00E5FF.toInt(); "MAGNET" -> 0xFFFF6F00.toInt(); else -> 0xFFFFD700.toInt() }
            canvas.drawRoundRect(RectF(x, oy - bh2 / 2, x + bw2 * prog, oy + bh2 / 2), 3f, 3f, paint)
            textPaint.color = Color.WHITE; textPaint.textSize = is2 * 0.8f; textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(name, x + 4f, oy + is2 * 0.3f, textPaint)
            oy += bh2 + is2 * 0.5f
        }
    }

    private fun drawHeart(canvas: Canvas, cx: Float, cy: Float, s: Float) {
        path.reset(); path.moveTo(cx, cy + s * 0.6f)
        path.cubicTo(cx - s * 1.2f, cy - s * 0.2f, cx - s * 0.6f, cy - s * 0.9f, cx, cy - s * 0.3f)
        path.cubicTo(cx + s * 0.6f, cy - s * 0.9f, cx + s * 1.2f, cy - s * 0.2f, cx, cy + s * 0.6f)
        canvas.drawPath(path, paint)
    }

    // ---- Screens ----
    fun drawStartScreen(canvas: Canvas, world: GameWorld) {
        paint.color = 0xFF0D1B2A.toInt(); canvas.drawRect(0f, 0f, world.screenWidth, world.screenHeight, paint)
        val t = System.currentTimeMillis()
        for (i in 0 until 25) { val seed = i * 997L; val x = ((seed * 7) % world.screenWidth.toLong()).toFloat(); val baseY = ((t * 0.4f + seed * 3) % (world.screenHeight * 1.5f)) - world.screenHeight * 0.25f; paint.color = 0x18FFFFFF; paint.strokeWidth = 2f; canvas.drawLine(x, baseY, x, baseY + 50f + (seed % 40), paint) }

        val titleY = world.screenHeight * 0.15f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = world.screenWidth * 0.12f; textPaint.color = 0xFF42A5F5.toInt()
        canvas.drawText("TRAFFIC", world.screenWidth / 2, titleY, textPaint)
        textPaint.textSize = world.screenWidth * 0.15f; textPaint.color = 0xFFFF5722.toInt()
        canvas.drawText("RACER", world.screenWidth / 2, titleY + world.screenWidth * 0.14f, textPaint)
        textPaint.textSize = world.screenWidth * 0.04f; textPaint.color = 0xFF00E5FF.toInt()
        canvas.drawText("NFS EDITION", world.screenWidth / 2, titleY + world.screenWidth * 0.2f, textPaint)

        val carY = world.screenHeight * 0.4f; val bob = sin(t * 0.003) * 8f
        val selCar = world.gameData.getSelectedCar()
        drawCarBody(canvas, world.screenWidth / 2, carY + bob.toFloat(), world.screenWidth * 0.18f, world.screenHeight * 0.1f, selCar.color, true)

        // Buttons
        val btnW = world.screenWidth * 0.55f; val btnH = world.screenHeight * 0.065f
        val pulse = 1f + sin(t * 0.004) * 0.02f
        val playY = world.screenHeight * 0.56f
        paint.color = 0xFF4CAF50.toInt()
        canvas.drawRoundRect(RectF(world.screenWidth / 2 - btnW / 2 * pulse.toFloat(), playY - btnH / 2 * pulse.toFloat(), world.screenWidth / 2 + btnW / 2 * pulse.toFloat(), playY + btnH / 2 * pulse.toFloat()), btnH / 2, btnH / 2, paint)
        textPaint.color = Color.WHITE; textPaint.textSize = btnH * 0.45f
        canvas.drawText("RACE!", world.screenWidth / 2, playY + btnH * 0.15f, textPaint)

        val garageY = world.screenHeight * 0.66f
        paint.color = 0xFF1565C0.toInt()
        canvas.drawRoundRect(RectF(world.screenWidth / 2 - btnW / 2, garageY - btnH / 2, world.screenWidth / 2 + btnW / 2, garageY + btnH / 2), btnH / 2, btnH / 2, paint)
        textPaint.textSize = btnH * 0.4f; canvas.drawText("GARAGE", world.screenWidth / 2, garageY + btnH * 0.13f, textPaint)

        val missionsY = world.screenHeight * 0.75f
        paint.color = 0xFFFF6F00.toInt()
        canvas.drawRoundRect(RectF(world.screenWidth / 2 - btnW / 2, missionsY - btnH / 2, world.screenWidth / 2 + btnW / 2, missionsY + btnH / 2), btnH / 2, btnH / 2, paint)
        canvas.drawText("MISSIONS", world.screenWidth / 2, missionsY + btnH * 0.13f, textPaint)

        // Stats
        textPaint.textSize = world.screenWidth * 0.035f; textPaint.color = 0xAAFFFFFF.toInt()
        canvas.drawText("High Score: ${world.gameData.highScore}", world.screenWidth / 2, world.screenHeight * 0.84f, textPaint)
        textPaint.color = 0xFFFFD700.toInt()
        canvas.drawText("Coins: ${world.gameData.totalCoins}", world.screenWidth / 2, world.screenHeight * 0.89f, textPaint)
        textPaint.color = 0x77FFFFFF.toInt(); textPaint.textSize = world.screenWidth * 0.028f
        canvas.drawText("Steer to dodge • Gas to accelerate • NOS for boost!", world.screenWidth / 2, world.screenHeight * 0.95f, textPaint)
    }

    fun drawGarageScreen(canvas: Canvas, world: GameWorld) {
        paint.color = 0xFF1A1A2E.toInt(); canvas.drawRect(0f, 0f, world.screenWidth, world.screenHeight, paint)
        textPaint.textAlign = Paint.Align.CENTER; textPaint.color = 0xFF42A5F5.toInt(); textPaint.textSize = world.screenWidth * 0.08f
        canvas.drawText("GARAGE", world.screenWidth / 2, world.screenHeight * 0.08f, textPaint)

        textPaint.color = 0xFFFFD700.toInt(); textPaint.textSize = world.screenWidth * 0.04f
        canvas.drawText("Coins: ${world.gameData.totalCoins}", world.screenWidth / 2, world.screenHeight * 0.13f, textPaint)

        val car = PlayerCarDef.ALL_CARS[world.garageSelectedIndex]
        val unlocked = world.gameData.isCarUnlocked(car.id)
        val equipped = world.gameData.selectedCarId == car.id

        // Car preview
        val carY = world.screenHeight * 0.28f
        val bob = sin(System.currentTimeMillis() * 0.003) * 5f
        drawCarBody(canvas, world.screenWidth / 2, carY + bob.toFloat(), world.screenWidth * 0.22f, world.screenHeight * 0.13f, car.color, true)

        // Car name & description
        textPaint.textSize = world.screenWidth * 0.06f; textPaint.color = Color.WHITE
        canvas.drawText(car.name, world.screenWidth / 2, world.screenHeight * 0.42f, textPaint)
        textPaint.textSize = world.screenWidth * 0.03f; textPaint.color = 0xAAFFFFFF.toInt()
        canvas.drawText(car.description, world.screenWidth / 2, world.screenHeight * 0.46f, textPaint)

        // Stats bars
        val barY = world.screenHeight * 0.50f; val barW = world.screenWidth * 0.5f; val barH = world.screenHeight * 0.02f; val barX = world.screenWidth * 0.25f
        drawStatBar(canvas, "SPEED", car.baseSpeed / 1.4f, barX, barY, barW, barH, 0xFF4CAF50.toInt())
        drawStatBar(canvas, "HANDLING", car.baseHandling / 1.4f, barX, barY + barH * 2.5f, barW, barH, 0xFF2196F3.toInt())
        drawStatBar(canvas, "NITRO", car.baseNitro / 1.5f, barX, barY + barH * 5f, barW, barH, 0xFFFF9800.toInt())

        // Nav arrows
        val arrowY = world.screenHeight * 0.28f
        if (world.garageSelectedIndex > 0) { textPaint.textSize = world.screenWidth * 0.08f; textPaint.color = 0xAAFFFFFF.toInt(); textPaint.textAlign = Paint.Align.CENTER; canvas.drawText("◀", world.screenWidth * 0.08f, arrowY, textPaint) }
        if (world.garageSelectedIndex < PlayerCarDef.ALL_CARS.size - 1) { textPaint.textSize = world.screenWidth * 0.08f; canvas.drawText("▶", world.screenWidth * 0.92f, arrowY, textPaint) }

        // Action button
        val btnW = world.screenWidth * 0.5f; val btnH = world.screenHeight * 0.06f
        val btnY = world.screenHeight * 0.68f
        if (!unlocked) {
            paint.color = 0xFFFF6F00.toInt()
            canvas.drawRoundRect(RectF(world.screenWidth / 2 - btnW / 2, btnY - btnH / 2, world.screenWidth / 2 + btnW / 2, btnY + btnH / 2), btnH / 2, btnH / 2, paint)
            textPaint.textAlign = Paint.Align.CENTER; textPaint.color = Color.WHITE; textPaint.textSize = btnH * 0.4f
            canvas.drawText("BUY - ${car.price} coins", world.screenWidth / 2, btnY + btnH * 0.13f, textPaint)
        } else if (!equipped) {
            paint.color = 0xFF4CAF50.toInt()
            canvas.drawRoundRect(RectF(world.screenWidth / 2 - btnW / 2, btnY - btnH / 2, world.screenWidth / 2 + btnW / 2, btnY + btnH / 2), btnH / 2, btnH / 2, paint)
            textPaint.textAlign = Paint.Align.CENTER; textPaint.color = Color.WHITE; textPaint.textSize = btnH * 0.4f
            canvas.drawText("EQUIP", world.screenWidth / 2, btnY + btnH * 0.13f, textPaint)
        } else {
            textPaint.textAlign = Paint.Align.CENTER; textPaint.color = 0xFF00E676.toInt(); textPaint.textSize = btnH * 0.4f
            canvas.drawText("EQUIPPED", world.screenWidth / 2, btnY + btnH * 0.13f, textPaint)
        }

        // Back button
        val backY = world.screenHeight * 0.78f
        paint.color = 0x44FFFFFF
        canvas.drawRoundRect(RectF(world.screenWidth / 2 - btnW / 2, backY - btnH / 2, world.screenWidth / 2 + btnW / 2, backY + btnH / 2), btnH / 2, btnH / 2, paint)
        textPaint.color = Color.WHITE; canvas.drawText("BACK", world.screenWidth / 2, backY + btnH * 0.13f, textPaint)

        // Pagination dots
        val dotY = world.screenHeight * 0.85f; val dotSpacing = world.screenWidth * 0.025f
        val totalWidth = (PlayerCarDef.ALL_CARS.size - 1) * dotSpacing
        val dotStartX = world.screenWidth / 2 - totalWidth / 2
        for (i in PlayerCarDef.ALL_CARS.indices) {
            paint.color = if (i == world.garageSelectedIndex) 0xFFFFFFFF.toInt() else 0x55FFFFFF
            canvas.drawCircle(dotStartX + i * dotSpacing, dotY, if (i == world.garageSelectedIndex) 5f else 3f, paint)
        }
    }

    fun drawMissionsScreen(canvas: Canvas, world: GameWorld) {
        paint.color = 0xFF1A1A2E.toInt(); canvas.drawRect(0f, 0f, world.screenWidth, world.screenHeight, paint)
        textPaint.textAlign = Paint.Align.CENTER; textPaint.color = 0xFFFF6F00.toInt(); textPaint.textSize = world.screenWidth * 0.08f
        canvas.drawText("MISSIONS", world.screenWidth / 2, world.screenHeight * 0.08f, textPaint)

        val missions = world.currentMissions
        val startY = world.screenHeight * 0.14f; val mh = world.screenHeight * 0.08f
        for (i in missions.indices) {
            val m = missions[i]; val my = startY + i * (mh + world.screenHeight * 0.015f)
            val done = world.gameData.isMissionCompleted(m.id)
            val progress = world.missionProgress[m.id] ?: 0
            val progRatio = (progress.toFloat() / m.target).coerceIn(0f, 1f)
            paint.color = if (done) 0x44004D40 else 0x44263238
            canvas.drawRoundRect(RectF(world.screenWidth * 0.05f, my, world.screenWidth * 0.95f, my + mh), 8f, 8f, paint)
            textPaint.textAlign = Paint.Align.LEFT; textPaint.textSize = mh * 0.3f; textPaint.color = if (done) 0xFF00E676.toInt() else Color.WHITE
            canvas.drawText(m.description, world.screenWidth * 0.08f, my + mh * 0.4f, textPaint)
            textPaint.textSize = mh * 0.22f; textPaint.color = 0xFFFFD700.toInt()
            canvas.drawText("+${m.reward} coins", world.screenWidth * 0.08f, my + mh * 0.7f, textPaint)
            if (!done) {
                val barX2 = world.screenWidth * 0.6f; val barW2 = world.screenWidth * 0.3f; val barH2 = mh * 0.15f; val barY2 = my + mh * 0.55f
                paint.color = 0x44FFFFFF; canvas.drawRoundRect(RectF(barX2, barY2, barX2 + barW2, barY2 + barH2), 3f, 3f, paint)
                paint.color = 0xFF4CAF50.toInt(); canvas.drawRoundRect(RectF(barX2, barY2, barX2 + barW2 * progRatio, barY2 + barH2), 3f, 3f, paint)
                textPaint.textAlign = Paint.Align.RIGHT; textPaint.textSize = mh * 0.2f; textPaint.color = 0xAAFFFFFF.toInt()
                canvas.drawText("$progress/${m.target}", world.screenWidth * 0.92f, my + mh * 0.4f, textPaint)
            } else {
                textPaint.textAlign = Paint.Align.RIGHT; textPaint.textSize = mh * 0.3f; textPaint.color = 0xFF00E676.toInt()
                canvas.drawText("DONE", world.screenWidth * 0.92f, my + mh * 0.5f, textPaint)
            }
        }

        val backY = world.screenHeight * 0.85f; val btnW = world.screenWidth * 0.5f; val btnH = world.screenHeight * 0.06f
        paint.color = 0x44FFFFFF
        canvas.drawRoundRect(RectF(world.screenWidth / 2 - btnW / 2, backY - btnH / 2, world.screenWidth / 2 + btnW / 2, backY + btnH / 2), btnH / 2, btnH / 2, paint)
        textPaint.textAlign = Paint.Align.CENTER; textPaint.color = Color.WHITE; textPaint.textSize = btnH * 0.4f
        canvas.drawText("BACK", world.screenWidth / 2, backY + btnH * 0.13f, textPaint)
    }

    fun drawGameOverScreen(canvas: Canvas, world: GameWorld) {
        paint.color = 0xDD000000.toInt(); canvas.drawRect(0f, 0f, world.screenWidth, world.screenHeight, paint)
        val cy = world.screenHeight * 0.18f
        textPaint.textAlign = Paint.Align.CENTER; textPaint.color = 0xFFEF5350.toInt(); textPaint.textSize = world.screenWidth * 0.1f
        canvas.drawText("GAME OVER", world.screenWidth / 2, cy, textPaint)

        val isNew = world.player.score >= world.gameData.highScore && world.player.score > 0
        if (isNew) { val flash = ((sin(System.currentTimeMillis() * 0.005) + 1f) / 2f * 255).toInt(); textPaint.color = (0xFF shl 24) or (flash shl 16) or (0xD7 shl 8); textPaint.textSize = world.screenWidth * 0.05f; canvas.drawText("NEW HIGH SCORE!", world.screenWidth / 2, cy + world.screenHeight * 0.05f, textPaint) }

        val sy = cy + world.screenHeight * 0.1f; val ss = world.screenHeight * 0.045f
        val lx = world.screenWidth * 0.2f; val vx = world.screenWidth * 0.8f
        val p = world.player

        drawStatLine(canvas, "DISTANCE", "${p.distanceScore.toLong()}m", lx, vx, sy, ss, Color.WHITE)
        drawStatLine(canvas, "TOP SPEED", "${p.getSpeedKmh()} km/h", lx, vx, sy + ss, ss, 0xFF42A5F5.toInt())
        drawStatLine(canvas, "COINS", "${p.coinsCollectedThisRun}", lx, vx, sy + ss * 2, ss, 0xFFFFD700.toInt())
        drawStatLine(canvas, "NEAR MISSES", "${p.nearMissesThisRun}", lx, vx, sy + ss * 3, ss, 0xFF00E676.toInt())
        drawStatLine(canvas, "OVERTAKES", "${p.overtakesThisRun}", lx, vx, sy + ss * 4, ss, 0xFF64FFDA.toInt())
        drawStatLine(canvas, "BEST COMBO", "x${p.maxComboThisRun}", lx, vx, sy + ss * 5, ss, 0xFFFF6D00.toInt())
        drawStatLine(canvas, "TOTAL SCORE", "${p.score}", lx, vx, sy + ss * 6.5f, ss * 1.2f, 0xFF4CAF50.toInt())

        // Achievements
        if (world.newAchievements.isNotEmpty()) {
            val ay = sy + ss * 8f; textPaint.color = 0xFFFFD700.toInt(); textPaint.textSize = world.screenWidth * 0.04f; textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("ACHIEVEMENTS UNLOCKED!", world.screenWidth / 2, ay, textPaint)
            for ((i, a) in world.newAchievements.withIndex()) { textPaint.textSize = world.screenWidth * 0.03f; textPaint.color = 0xFFFFD700.toInt(); canvas.drawText("${a.icon} ${a.name}", world.screenWidth / 2, ay + (i + 1) * world.screenHeight * 0.03f, textPaint) }
        }

        val btnY = world.screenHeight * 0.82f; val btnW = world.screenWidth * 0.55f; val btnH = world.screenHeight * 0.065f
        paint.color = 0xFF4CAF50.toInt()
        canvas.drawRoundRect(RectF(world.screenWidth / 2 - btnW / 2, btnY - btnH / 2, world.screenWidth / 2 + btnW / 2, btnY + btnH / 2), btnH / 2, btnH / 2, paint)
        textPaint.color = Color.WHITE; textPaint.textSize = btnH * 0.45f; textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("RACE AGAIN", world.screenWidth / 2, btnY + btnH * 0.15f, textPaint)
        textPaint.color = 0x88FFFFFF.toInt(); textPaint.textSize = world.screenWidth * 0.03f
        canvas.drawText("Tap to continue", world.screenWidth / 2, world.screenHeight * 0.92f, textPaint)
    }

    private fun drawPauseOverlay(canvas: Canvas, world: GameWorld) {
        paint.color = 0xAA000000.toInt(); canvas.drawRect(0f, 0f, world.screenWidth, world.screenHeight, paint)
        textPaint.textAlign = Paint.Align.CENTER; textPaint.color = Color.WHITE; textPaint.textSize = world.screenWidth * 0.1f
        canvas.drawText("PAUSED", world.screenWidth / 2, world.screenHeight * 0.4f, textPaint)
        val btnY = world.screenHeight * 0.55f; val btnW = world.screenWidth * 0.5f; val btnH = world.screenHeight * 0.065f
        paint.color = 0xFF4CAF50.toInt()
        canvas.drawRoundRect(RectF(world.screenWidth / 2 - btnW / 2, btnY - btnH / 2, world.screenWidth / 2 + btnW / 2, btnY + btnH / 2), btnH / 2, btnH / 2, paint)
        textPaint.textSize = btnH * 0.45f; canvas.drawText("RESUME", world.screenWidth / 2, btnY + btnH * 0.15f, textPaint)
    }

    // ---- Helpers ----
    private fun drawStatBar(canvas: Canvas, label: String, ratio: Float, x: Float, y: Float, w: Float, h: Float, color: Int) {
        textPaint.textAlign = Paint.Align.LEFT; textPaint.textSize = h * 0.9f; textPaint.color = 0xAAFFFFFF.toInt()
        canvas.drawText(label, x, y - h * 0.3f, textPaint)
        paint.color = 0x44FFFFFF; canvas.drawRoundRect(RectF(x, y, x + w, y + h), 3f, 3f, paint)
        paint.color = color; canvas.drawRoundRect(RectF(x, y, x + w * ratio.coerceIn(0f, 1f), y + h), 3f, 3f, paint)
    }

    private fun drawStatLine(canvas: Canvas, label: String, value: String, lx: Float, vx: Float, y: Float, size: Float, valueColor: Int) {
        textPaint.textAlign = Paint.Align.LEFT; textPaint.color = 0xAAFFFFFF.toInt(); textPaint.textSize = size * 0.7f
        canvas.drawText(label, lx, y, textPaint)
        textPaint.textAlign = Paint.Align.RIGHT; textPaint.color = valueColor; textPaint.textSize = size * 0.8f
        canvas.drawText(value, vx, y, textPaint)
    }

    private fun blendColors(c1: Int, c2: Int, ratio: Float): Int {
        val inv = 1f - ratio
        return Color.rgb(
            ((Color.red(c1) * inv + Color.red(c2) * ratio)).toInt().coerceIn(0, 255),
            ((Color.green(c1) * inv + Color.green(c2) * ratio)).toInt().coerceIn(0, 255),
            ((Color.blue(c1) * inv + Color.blue(c2) * ratio)).toInt().coerceIn(0, 255)
        )
    }
    private fun darkenColor(c: Int, f: Float): Int = Color.rgb((Color.red(c) * (1f - f)).toInt().coerceIn(0, 255), (Color.green(c) * (1f - f)).toInt().coerceIn(0, 255), (Color.blue(c) * (1f - f)).toInt().coerceIn(0, 255))
    private fun lightenColor(c: Int, f: Float): Int = Color.rgb(min(255, Color.red(c) + ((255 - Color.red(c)) * f).toInt()), min(255, Color.green(c) + ((255 - Color.green(c)) * f).toInt()), min(255, Color.blue(c) + ((255 - Color.blue(c)) * f).toInt()))
}

data class ControlAreas(
    val brake: RectF, val gas: RectF,
    val steerLeft: Float, val steerRight: Float, val steerCy: Float,
    val nosCx: Float, val nosCy: Float, val nosRadius: Float,
    val pauseArea: RectF
)
