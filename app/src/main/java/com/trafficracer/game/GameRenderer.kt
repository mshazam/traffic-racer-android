package com.trafficracer.game

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.max
import kotlin.math.sin

class GameRenderer(private val spriteManager: SpriteManager) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val spriteMatrix = Matrix()

    fun render(canvas: Canvas, world: GameWorld) {
        try { renderInternal(canvas, world) } catch (_: Exception) {}
    }

    private fun renderInternal(canvas: Canvas, world: GameWorld) {
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

    // ---- Sky & Grass ----
    private fun drawSky(canvas: Canvas, world: GameWorld, env: Environment) {
        val top = blendColors(env.skyTopDay, env.skyTopNight, world.nightFactor)
        val bot = blendColors(env.skyBottomDay, env.skyBottomNight, world.nightFactor)
        paint.shader = LinearGradient(0f, 0f, 0f, world.screenHeight, top, bot, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, world.screenWidth, world.screenHeight, paint)
        paint.shader = null
    }

    private fun drawGrass(canvas: Canvas, world: GameWorld, env: Environment) {
        val gc = blendColors(env.grassColor, darkenColor(env.grassColor, 0.4f), world.nightFactor)
        paint.color = gc
        canvas.drawRect(0f, 0f, world.roadLeft - world.shoulderWidth, world.screenHeight, paint)
        canvas.drawRect(world.roadRight + world.shoulderWidth, 0f, world.screenWidth, world.screenHeight, paint)
        // Grass texture stripes
        val gl = blendColors(env.grassLight, darkenColor(env.grassLight, 0.4f), world.nightFactor)
        paint.color = gl
        val sw = world.screenWidth * 0.02f
        var x = 0f
        while (x < world.roadLeft - world.shoulderWidth) { canvas.drawRect(x, 0f, x + sw * 0.4f, world.screenHeight, paint); x += sw * 2.5f }
        x = world.roadRight + world.shoulderWidth
        while (x < world.screenWidth) { canvas.drawRect(x, 0f, x + sw * 0.4f, world.screenHeight, paint); x += sw * 2.5f }
    }

    // ---- Road with gradient ----
    private fun drawRoad(canvas: Canvas, world: GameWorld, env: Environment) {
        val rc = blendColors(env.roadColor, darkenColor(env.roadColor, 0.3f), world.nightFactor)
        val sc = blendColors(env.shoulderColor, darkenColor(env.shoulderColor, 0.3f), world.nightFactor)

        // Shoulder
        paint.color = sc
        canvas.drawRect(world.roadLeft - world.shoulderWidth, 0f, world.roadLeft, world.screenHeight, paint)
        canvas.drawRect(world.roadRight, 0f, world.roadRight + world.shoulderWidth, world.screenHeight, paint)

        // Road surface with subtle gradient
        paint.shader = LinearGradient(world.roadLeft, 0f, world.roadRight, 0f, darkenColor(rc, 0.05f), lightenColor(rc, 0.05f), Shader.TileMode.CLAMP)
        canvas.drawRect(world.roadLeft, 0f, world.roadRight, world.screenHeight, paint)
        paint.shader = null

        // Edge lines
        paint.strokeWidth = world.screenWidth * 0.007f
        paint.color = Color.WHITE
        canvas.drawLine(world.roadLeft, 0f, world.roadLeft, world.screenHeight, paint)
        canvas.drawLine(world.roadRight, 0f, world.roadRight, world.screenHeight, paint)
        // Yellow rumble strips on shoulder edges
        paint.color = 0xFFFFD54F.toInt(); paint.strokeWidth = world.screenWidth * 0.005f
        canvas.drawLine(world.roadLeft - world.shoulderWidth, 0f, world.roadLeft - world.shoulderWidth, world.screenHeight, paint)
        canvas.drawLine(world.roadRight + world.shoulderWidth, 0f, world.roadRight + world.shoulderWidth, world.screenHeight, paint)
    }

    private fun drawRoadMarkings(canvas: Canvas, world: GameWorld, env: Environment) {
        val mw = world.screenWidth * Constants.ROAD_MARKING_WIDTH_RATIO
        val ml = world.screenHeight * Constants.ROAD_MARKING_LENGTH_RATIO
        paint.color = blendColors(0xDDFFFFFF.toInt(), 0xFFAAAAAA.toInt(), world.nightFactor)
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
            2 -> { // Cactus
                paint.color = blendColors(0xFF2E7D32.toInt(), 0xFF1B3A1B.toInt(), world.nightFactor)
                canvas.drawRoundRect(RectF(obj.x - bw * 0.08f, obj.y - bw * 0.8f, obj.x + bw * 0.08f, obj.y + bw * 0.1f), bw * 0.08f, bw * 0.08f, paint)
                canvas.drawRoundRect(RectF(obj.x - bw * 0.3f, obj.y - bw * 0.5f, obj.x - bw * 0.15f, obj.y - bw * 0.1f), bw * 0.05f, bw * 0.05f, paint)
                canvas.drawRoundRect(RectF(obj.x + bw * 0.15f, obj.y - bw * 0.6f, obj.x + bw * 0.3f, obj.y - bw * 0.2f), bw * 0.05f, bw * 0.05f, paint)
            }
            3 -> { // Pine tree
                paint.color = blendColors(0xFF5D4037.toInt(), 0xFF3E2723.toInt(), world.nightFactor)
                canvas.drawRect(obj.x - bw * 0.06f, obj.y, obj.x + bw * 0.06f, obj.y + bw * 0.5f, paint)
                paint.color = blendColors(0xFF1B5E20.toInt(), 0xFF0D2E10.toInt(), world.nightFactor)
                for (i in 0..2) {
                    val ty = obj.y - bw * (0.2f + i * 0.25f); val tw = bw * (0.5f - i * 0.1f)
                    path.reset(); path.moveTo(obj.x, ty - bw * 0.3f); path.lineTo(obj.x - tw, ty + bw * 0.1f); path.lineTo(obj.x + tw, ty + bw * 0.1f); path.close()
                    canvas.drawPath(path, paint)
                }
                paint.color = 0x55FFFFFF; canvas.drawCircle(obj.x - bw * 0.15f, obj.y - bw * 0.3f, bw * 0.05f, paint)
            }
            1 -> { // Bush
                paint.color = blendColors(0xFF388E3C.toInt(), 0xFF1E4420.toInt(), world.nightFactor)
                canvas.drawCircle(obj.x, obj.y, bw * 0.35f, paint)
                paint.color = blendColors(0xFF43A047.toInt(), 0xFF2E5A2E.toInt(), world.nightFactor)
                canvas.drawCircle(obj.x - bw * 0.15f, obj.y - bw * 0.1f, bw * 0.2f, paint)
            }
            else -> { // Tree
                paint.color = blendColors(0xFF5D4037.toInt(), 0xFF3E2723.toInt(), world.nightFactor)
                canvas.drawRect(obj.x - bw * 0.1f, obj.y, obj.x + bw * 0.1f, obj.y + bw * 0.8f, paint)
                paint.color = blendColors(0xFF2E7D32.toInt(), 0xFF1B3A1B.toInt(), world.nightFactor)
                canvas.drawCircle(obj.x, obj.y - bw * 0.1f, bw * 0.45f, paint)
                paint.color = blendColors(0xFF43A047.toInt(), 0xFF2E7D32.toInt(), world.nightFactor)
                canvas.drawCircle(obj.x + bw * 0.15f, obj.y - bw * 0.25f, bw * 0.3f, paint)
            }
        }
    }

    // ---- Premium car rendering with sprites ----
    private fun drawPlayer(canvas: Canvas, world: GameWorld) {
        val p = world.player
        if (p.isInvincible && (System.currentTimeMillis() / 120) % 3 == 0L) return

        canvas.save()
        val visualAngle = p.steerAngle * -6f + p.driftAngle * -12f
        canvas.rotate(visualAngle, p.x, p.y)

        // Shadow under car
        shadowPaint.color = 0x33000000
        canvas.drawOval(RectF(p.x - p.width * 0.55f, p.y + p.height * 0.3f, p.x + p.width * 0.55f, p.y + p.height * 0.55f), shadowPaint)

        val spritePath = spriteManager.getPlayerSprite(p.carDef.id)
        val sprite = spriteManager.getScaled(spritePath, p.width * 1.1f, p.height * 1.1f)
        if (sprite != null && !sprite.isRecycled) {
            canvas.drawBitmap(sprite, p.x - sprite.width / 2f, p.y - sprite.height / 2f, bitmapPaint)
        } else {
            drawDetailedCar(canvas, p.x, p.y, p.width, p.height, p.carDef.color, true, p.brakeLightIntensity)
        }
        canvas.restore()

        if (p.hasShield) drawShieldEffect(canvas, p)
        if (p.nosActive) drawNOSFlame(canvas, p)
    }

    private fun drawDetailedCar(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float, color: Int, isPlayer: Boolean, brakeLight: Float = 0f) {
        val l = cx - w / 2; val t = cy - h / 2; val r = cx + w / 2; val b = cy + h / 2
        val cr = w * 0.18f

        // Wheels with treads
        val wheelW = w * 0.12f; val wheelH = h * 0.22f
        val darkWheel = 0xFF1A1A1A.toInt()
        val rimColor = 0xFF888888.toInt()
        drawWheel(canvas, l - wheelW * 0.3f, t + h * 0.1f, wheelW, wheelH, darkWheel, rimColor)
        drawWheel(canvas, r - wheelW * 0.7f, t + h * 0.1f, wheelW, wheelH, darkWheel, rimColor)
        drawWheel(canvas, l - wheelW * 0.3f, t + h * 0.68f, wheelW, wheelH, darkWheel, rimColor)
        drawWheel(canvas, r - wheelW * 0.7f, t + h * 0.68f, wheelW, wheelH, darkWheel, rimColor)

        // Car body with gradient (3D effect)
        paint.shader = LinearGradient(l, t, r, t, lightenColor(color, 0.2f), darkenColor(color, 0.1f), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(RectF(l, t, r, b), cr, cr, paint)
        paint.shader = null

        // Hood highlight
        paint.shader = LinearGradient(l + w * 0.15f, t, r - w * 0.15f, t, lightenColor(color, 0.35f), lightenColor(color, 0.1f), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(RectF(l + w * 0.1f, t + h * 0.02f, r - w * 0.1f, t + h * 0.15f), cr * 0.6f, cr * 0.6f, paint)
        paint.shader = null

        // Center body line (subtle)
        paint.color = darkenColor(color, 0.08f)
        canvas.drawRect(l + w * 0.05f, t + h * 0.38f, r - w * 0.05f, t + h * 0.42f, paint)

        // Windshield with reflection gradient
        paint.shader = LinearGradient(l + w * 0.15f, t + h * 0.18f, r - w * 0.15f, t + h * 0.35f, 0xFF78B8E8.toInt(), 0xFF4A90C2.toInt(), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(RectF(l + w * 0.13f, t + h * 0.18f, r - w * 0.13f, t + h * 0.35f), cr * 0.4f, cr * 0.4f, paint)
        paint.shader = null
        // Windshield reflection streak
        paint.color = 0x33FFFFFF
        canvas.drawRect(l + w * 0.2f, t + h * 0.2f, l + w * 0.35f, t + h * 0.22f, paint)

        // Rear window
        paint.shader = LinearGradient(l + w * 0.15f, t + h * 0.62f, r - w * 0.15f, t + h * 0.75f, 0xFF5A9CC8.toInt(), 0xFF3A7CA8.toInt(), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(RectF(l + w * 0.15f, t + h * 0.62f, r - w * 0.15f, t + h * 0.75f), cr * 0.3f, cr * 0.3f, paint)
        paint.shader = null

        // Headlights (glowing)
        if (isPlayer) {
            val hlGlow = 0x44FFEE58.toInt()
            paint.color = hlGlow
            canvas.drawOval(RectF(l + w * 0.08f, t - h * 0.02f, l + w * 0.32f, t + h * 0.06f), paint)
            canvas.drawOval(RectF(r - w * 0.32f, t - h * 0.02f, r - w * 0.08f, t + h * 0.06f), paint)
        }
        paint.color = 0xFFFFEE58.toInt()
        canvas.drawRoundRect(RectF(l + w * 0.12f, t, l + w * 0.3f, t + h * 0.035f), 2f, 2f, paint)
        canvas.drawRoundRect(RectF(r - w * 0.3f, t, r - w * 0.12f, t + h * 0.035f), 2f, 2f, paint)

        // Tail lights with dynamic brake intensity
        val brakeAlpha = (0xAA + (brakeLight * 0x55).toInt()).coerceIn(0, 255)
        val brakeColor = (brakeAlpha shl 24) or 0xFF2020
        paint.color = brakeColor
        val blw = w * (0.18f + brakeLight * 0.04f)
        canvas.drawRoundRect(RectF(l + w * 0.08f, b - h * 0.04f, l + w * 0.08f + blw, b), 2f, 2f, paint)
        canvas.drawRoundRect(RectF(r - w * 0.08f - blw, b - h * 0.04f, r - w * 0.08f, b), 2f, 2f, paint)
        // Brake glow
        if (brakeLight > 0.3f) {
            paint.color = ((brakeLight * 40).toInt().coerceIn(0, 255) shl 24) or 0xFF3030
            canvas.drawOval(RectF(l + w * 0.05f, b - h * 0.08f, l + w * 0.3f, b + h * 0.03f), paint)
            canvas.drawOval(RectF(r - w * 0.3f, b - h * 0.08f, r - w * 0.05f, b + h * 0.03f), paint)
        }

        // Side mirrors
        paint.color = darkenColor(color, 0.15f)
        canvas.drawOval(RectF(l - w * 0.05f, t + h * 0.22f, l + w * 0.02f, t + h * 0.3f), paint)
        canvas.drawOval(RectF(r - w * 0.02f, t + h * 0.22f, r + w * 0.05f, t + h * 0.3f), paint)
    }

    private fun drawWheel(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, tireColor: Int, rimColor: Int) {
        paint.color = tireColor
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), w * 0.3f, w * 0.3f, paint)
        paint.color = rimColor
        canvas.drawRoundRect(RectF(x + w * 0.2f, y + h * 0.15f, x + w * 0.8f, y + h * 0.85f), w * 0.2f, w * 0.2f, paint)
    }

    private fun drawShieldEffect(canvas: Canvas, p: PlayerCar) {
        val phase = (System.currentTimeMillis() % 1000) / 1000f
        val radius = p.width * 0.85f + sin(phase * Math.PI.toFloat() * 2f) * 4f
        // Outer glow
        paint.color = 0x1500E5FF
        canvas.drawCircle(p.x, p.y, radius * 1.2f, paint)
        // Shield ring
        paint.color = ((180 + (sin(phase * Math.PI.toFloat() * 2f) * 50).toInt()).coerceIn(0, 255) shl 24) or 0x00E5FF
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 3f
        canvas.drawCircle(p.x, p.y, radius, paint)
        paint.style = Paint.Style.FILL
        paint.color = 0x1800E5FF
        canvas.drawCircle(p.x, p.y, radius, paint)
    }

    private fun drawNOSFlame(canvas: Canvas, p: PlayerCar) {
        val t = System.currentTimeMillis()
        val flicker = sin(t * 0.03f) * 0.25f + 0.75f
        val fh = p.height * 0.55f * flicker
        // Blue core flame
        for (i in 0 until 4) {
            val ox = (i - 1.5f) * p.width * 0.12f; val a = (200 - i * 35).coerceIn(0, 255)
            paint.color = (a shl 24) or 0x2979FF
            path.reset()
            path.moveTo(p.x + ox - p.width * 0.07f, p.y + p.height / 2)
            path.lineTo(p.x + ox + p.width * 0.07f, p.y + p.height / 2)
            path.lineTo(p.x + ox + sin(t * 0.025f + i) * 4f, p.y + p.height / 2 + fh * (1f - i * 0.15f))
            path.close(); canvas.drawPath(path, paint)
        }
        // Bright white core
        paint.color = 0xAA80D8FF.toInt()
        path.reset()
        path.moveTo(p.x - p.width * 0.04f, p.y + p.height / 2)
        path.lineTo(p.x + p.width * 0.04f, p.y + p.height / 2)
        path.lineTo(p.x, p.y + p.height / 2 + fh * 0.5f)
        path.close(); canvas.drawPath(path, paint)
    }

    // ---- Traffic ----
    private fun drawTraffic(canvas: Canvas, world: GameWorld) {
        for (car in world.trafficCars) {
            canvas.save()
            // Shadow
            shadowPaint.color = 0x22000000
            canvas.drawOval(RectF(car.x - car.width * 0.5f, car.y + car.height * 0.3f, car.x + car.width * 0.5f, car.y + car.height * 0.5f), shadowPaint)

            val spritePath = spriteManager.getTrafficSprite(car.spriteIndex)
            val sprite = spriteManager.getScaled(spritePath, car.width * 1.1f, car.height * 1.1f)
            if (sprite != null && !sprite.isRecycled) {
                if (car.isOncoming) {
                    canvas.rotate(180f, car.x, car.y)
                }
                canvas.drawBitmap(sprite, car.x - sprite.width / 2f, car.y - sprite.height / 2f, bitmapPaint)
            } else {
                if (car.isOncoming) canvas.rotate(180f, car.x, car.y)
                drawDetailedCar(canvas, car.x, car.y, car.width, car.height, car.color, false)
            }
            canvas.restore()
        }
    }

    // ---- Items ----
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
        // Coin shadow
        paint.color = (alpha / 4 shl 24) or 0x886600
        canvas.drawOval(RectF(x - size * sx + 2f, y - size + 2f, x + size * sx + 2f, y + size + 2f), paint)
        // Outer ring
        paint.color = (alpha shl 24) or 0xFFD700
        canvas.drawOval(RectF(x - size * sx, y - size, x + size * sx, y + size), paint)
        // Inner disc
        paint.color = (alpha shl 24) or 0xFFC107
        canvas.drawOval(RectF(x - size * sx * 0.75f, y - size * 0.75f, x + size * sx * 0.75f, y + size * 0.75f), paint)
        // $ sign
        if (sx > 0.6f) { textPaint.color = (alpha shl 24) or 0xB8860B; textPaint.textSize = size * 1f; textPaint.textAlign = Paint.Align.CENTER; canvas.drawText("$", x, y + size * 0.35f, textPaint) }
    }

    private fun drawPowerUps(canvas: Canvas, world: GameWorld) {
        for (pu in world.powerUps) {
            val pulse = 1f + sin(pu.pulsePhase) * 0.12f; val s = pu.size * pulse
            val ga = ((sin(pu.pulsePhase) * 0.25f + 0.35f) * 255).toInt().coerceIn(0, 255)
            // Glow
            paint.color = (ga shl 24) or (pu.type.color and 0x00FFFFFF); canvas.drawCircle(pu.x, pu.y, s * 1.4f, paint)
            // Outer ring
            paint.color = darkenColor(pu.type.color, 0.2f); canvas.drawCircle(pu.x, pu.y, s * 0.8f, paint)
            // Inner
            paint.color = pu.type.color; canvas.drawCircle(pu.x, pu.y, s * 0.65f, paint)
            paint.color = lightenColor(pu.type.color, 0.3f); canvas.drawCircle(pu.x, pu.y, s * 0.35f, paint)
            textPaint.color = Color.WHITE; textPaint.textSize = s * 0.6f; textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(when (pu.type) { PowerUpType.SHIELD -> "S"; PowerUpType.MAGNET -> "M"; PowerUpType.DOUBLE_SCORE -> "2x"; PowerUpType.EXTRA_LIFE -> "+"; PowerUpType.NITRO -> "N" }, pu.x, pu.y + s * 0.18f, textPaint)
        }
    }

    private fun drawHazards(canvas: Canvas, world: GameWorld) {
        for (h in world.hazards) {
            val spriteKey = when (h.type) {
                HazardType.OIL_SLICK -> "oil"
                HazardType.CONE -> "cone"
                HazardType.POTHOLE -> "rock"
            }
            val sprite = spriteManager.objectSprites[spriteKey]?.let {
                spriteManager.getScaled(it, h.size * 1.5f, h.size * 1.5f)
            }
            if (sprite != null && !sprite.isRecycled) {
                canvas.drawBitmap(sprite, h.x - sprite.width / 2f, h.y - sprite.height / 2f, bitmapPaint)
            } else {
                when (h.type) {
                    HazardType.OIL_SLICK -> {
                        paint.color = 0x66222222
                        canvas.drawOval(RectF(h.x - h.size, h.y - h.size * 0.5f, h.x + h.size, h.y + h.size * 0.5f), paint)
                    }
                    HazardType.CONE -> {
                        paint.color = 0xFFFF6D00.toInt()
                        path.reset(); path.moveTo(h.x, h.y - h.size * 0.8f); path.lineTo(h.x - h.size * 0.35f, h.y + h.size * 0.35f); path.lineTo(h.x + h.size * 0.35f, h.y + h.size * 0.35f); path.close()
                        canvas.drawPath(path, paint)
                    }
                    HazardType.POTHOLE -> {
                        paint.color = 0xFF111111.toInt()
                        canvas.drawOval(RectF(h.x - h.size * 0.55f, h.y - h.size * 0.35f, h.x + h.size * 0.55f, h.y + h.size * 0.35f), paint)
                    }
                }
            }
        }
    }

    private fun drawMysteryBoxes(canvas: Canvas, world: GameWorld) {
        for (mb in world.mysteryBoxes) {
            val bounce = sin(mb.bouncePhase) * 5f; val s = mb.size
            canvas.save(); canvas.translate(0f, bounce)
            // Box shadow
            paint.color = 0x22000000; canvas.drawRoundRect(RectF(mb.x - s / 2 + 3f, mb.y - s / 2 + 3f, mb.x + s / 2 + 3f, mb.y + s / 2 + 3f), s * 0.15f, s * 0.15f, paint)
            // Box body
            paint.shader = LinearGradient(mb.x - s / 2, mb.y - s / 2, mb.x + s / 2, mb.y + s / 2, 0xFFE040FB.toInt(), 0xFFAA00FF.toInt(), Shader.TileMode.CLAMP)
            canvas.drawRoundRect(RectF(mb.x - s / 2, mb.y - s / 2, mb.x + s / 2, mb.y + s / 2), s * 0.15f, s * 0.15f, paint)
            paint.shader = null
            // Inner highlight
            paint.color = 0x33FFFFFF
            canvas.drawRoundRect(RectF(mb.x - s * 0.35f, mb.y - s * 0.35f, mb.x + s * 0.35f, mb.y - s * 0.1f), s * 0.08f, s * 0.08f, paint)
            // ?
            textPaint.color = Color.WHITE; textPaint.textSize = s * 0.55f; textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("?", mb.x, mb.y + s * 0.18f, textPaint)
            canvas.restore()
        }
    }

    private fun drawTireTrails(canvas: Canvas, world: GameWorld) {
        for (t in world.tireTrails) { paint.color = ((t.alpha * 60).toInt().coerceIn(0, 255) shl 24) or 0x222222; canvas.drawRect(t.x - t.width / 2, t.y, t.x + t.width / 2, t.y + 5f, paint) }
    }

    private fun drawDriftSparks(canvas: Canvas, world: GameWorld) {
        for (s in world.driftSparks) { val a = (s.life * 255).toInt().coerceIn(0, 255); paint.color = (a shl 24) or (s.color and 0x00FFFFFF); canvas.drawCircle(s.x, s.y, 1.5f + s.life * 3.5f, paint) }
    }

    private fun drawParticles(canvas: Canvas, world: GameWorld) {
        for (p in world.particles) { val a = (p.alpha * 255).toInt().coerceIn(0, 255); paint.color = (a shl 24) or (p.color and 0x00FFFFFF); canvas.drawCircle(p.x, p.y, p.size, paint) }
    }

    private fun drawWeather(canvas: Canvas, world: GameWorld) {
        if (!world.currentEnvironment.hasWeather) return
        for (wp in world.weatherParticles) {
            val a = (wp.alpha * 180).toInt().coerceIn(0, 255)
            if (world.currentEnvironment.weatherType == WeatherType.SNOW) {
                paint.color = (a shl 24) or 0xFFFFFF; canvas.drawCircle(wp.x, wp.y, wp.size, paint)
            } else {
                paint.color = (a shl 24) or 0xAABBDD; paint.strokeWidth = 1.5f
                canvas.drawLine(wp.x, wp.y, wp.x + wp.windOffset * 2f, wp.y + wp.size * 3f, paint)
            }
        }
    }

    private fun drawFloatingTexts(canvas: Canvas, world: GameWorld) {
        val now = System.currentTimeMillis()
        for (ft in world.floatingTexts) {
            val prog = ft.getProgress(now); val alpha = ((1f - prog) * 255).toInt().coerceIn(0, 255)
            val scale = ft.scale * (1f + prog * 0.2f)
            textPaint.textAlign = Paint.Align.CENTER; textPaint.textSize = world.screenHeight * 0.022f * scale
            // Shadow
            textPaint.color = ((alpha * 0.5f).toInt().coerceIn(0, 255) shl 24) or 0x000000
            canvas.drawText(ft.text, ft.x + 1.5f, ft.y + 1.5f, textPaint)
            // Text
            textPaint.color = (alpha shl 24) or (ft.color and 0x00FFFFFF)
            canvas.drawText(ft.text, ft.x, ft.y, textPaint)
        }
    }

    private fun drawSpeedLines(canvas: Canvas, world: GameWorld) {
        val intensity = ((world.player.getEffectiveSpeed() - Constants.SPEED_BLUR_THRESHOLD) / (Constants.MAX_SPEED - Constants.SPEED_BLUR_THRESHOLD)).coerceIn(0f, 1f)
        val alpha = (intensity * 60).toInt(); paint.color = (alpha shl 24) or 0xFFFFFF; paint.strokeWidth = 1.5f
        val t = System.currentTimeMillis()
        for (i in 0 until (8 + (intensity * 12).toInt())) {
            val seed = i * 1337L; val x = world.roadLeft + (seed % world.roadWidth.toLong()).toFloat()
            val baseY = ((t * (0.4f + intensity * 0.5f) + seed) % world.screenHeight.toLong()).toFloat()
            canvas.drawLine(x, baseY, x, baseY + 15f + intensity * 35f + (seed % 25), paint)
        }
    }

    private fun drawSlowMoOverlay(canvas: Canvas, world: GameWorld) {
        val intensity = (1f - world.slowMoFactor).coerceIn(0f, 1f)
        val alpha = (intensity * 45).toInt()
        paint.color = (alpha shl 24) or 0x001828
        canvas.drawRect(0f, 0f, world.screenWidth, world.screenHeight * 0.1f, paint)
        canvas.drawRect(0f, world.screenHeight * 0.9f, world.screenWidth, world.screenHeight, paint)
    }

    // ---- Polished HUD ----
    private fun drawHUD(canvas: Canvas, world: GameWorld) {
        val p = world.player; val hh = world.screenHeight * 0.058f; val pad = world.screenWidth * 0.025f

        // Top bar with gradient
        paint.shader = LinearGradient(0f, 0f, 0f, hh, 0xCC000000.toInt(), 0x88000000.toInt(), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, world.screenWidth, hh, paint)
        paint.shader = null

        // Score
        textPaint.textAlign = Paint.Align.LEFT; textPaint.color = 0x99FFFFFF.toInt(); textPaint.textSize = hh * 0.3f
        canvas.drawText("SCORE", pad, hh * 0.36f, textPaint)
        textPaint.color = 0xFFFFD700.toInt(); textPaint.textSize = hh * 0.48f
        canvas.drawText("${p.score}", pad, hh * 0.82f, textPaint)

        // Coins
        textPaint.textAlign = Paint.Align.CENTER; textPaint.color = 0xFFFFD700.toInt(); textPaint.textSize = hh * 0.38f
        canvas.drawText("${world.gameData.totalCoins + p.coinsCollectedThisRun * Constants.COIN_VALUE}", world.screenWidth * 0.5f, hh * 0.65f, textPaint)

        // Lives as hearts
        val hs = hh * 0.22f; val startX = world.screenWidth - pad - (hs * 2.2f * Constants.MAX_LIVES)
        for (i in 0 until Constants.MAX_LIVES) {
            val hx = startX + i * hs * 2.2f + hs; val hy = hh * 0.55f
            paint.color = if (i < p.lives) 0xFFEF5350.toInt() else 0x33FFFFFF; drawHeart(canvas, hx, hy, hs)
        }

        // Combo indicator
        if (p.comboCount > 1) {
            textPaint.textAlign = Paint.Align.CENTER; textPaint.textSize = world.screenHeight * 0.022f
            textPaint.color = if (p.comboCount >= 5) 0xFFFF6D00.toInt() else 0xFF00E676.toInt()
            canvas.drawText("COMBO x${p.comboCount}!", world.screenWidth / 2, hh + world.screenHeight * 0.032f, textPaint)
        }

        drawActivePowerUps(canvas, world, hh)
        drawSpeedometer(canvas, world)

        // Pause button
        val ps = hh * 0.35f; val px = world.screenWidth - pad - ps * 1.2f; val py = hh * 0.5f
        paint.color = 0x44FFFFFF; canvas.drawRoundRect(RectF(px - ps, py - ps * 0.55f, px + ps, py + ps * 0.55f), ps * 0.2f, ps * 0.2f, paint)
        paint.color = Color.WHITE
        canvas.drawRoundRect(RectF(px - ps * 0.22f, py - ps * 0.28f, px - ps * 0.06f, py + ps * 0.28f), 1f, 1f, paint)
        canvas.drawRoundRect(RectF(px + ps * 0.06f, py - ps * 0.28f, px + ps * 0.22f, py + ps * 0.28f), 1f, 1f, paint)
    }

    private fun drawSpeedometer(canvas: Canvas, world: GameWorld) {
        val p = world.player; val size = world.screenWidth * Constants.SPEEDO_SIZE_RATIO
        val cx = world.screenWidth - size * 1.3f
        val cy = world.screenHeight - world.screenHeight * Constants.HUD_CONTROLS_HEIGHT_RATIO - size * 1.6f

        // Background circle
        paint.shader = RadialGradient(cx, cy, size, 0xAA111111.toInt(), 0x66000000, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, size, paint)
        paint.shader = null

        // Outer ring
        paint.style = Paint.Style.STROKE; paint.strokeWidth = size * 0.06f; paint.color = 0xFF444444.toInt()
        canvas.drawArc(RectF(cx - size * 0.82f, cy - size * 0.82f, cx + size * 0.82f, cy + size * 0.82f), 135f, 270f, false, paint)

        // Speed arc
        val speedRatio = (p.getEffectiveSpeed() / (Constants.MAX_SPEED * p.carDef.baseSpeed)).coerceIn(0f, 1f)
        val arcColor = when {
            p.nosActive -> 0xFF00E5FF.toInt()
            speedRatio > 0.8f -> 0xFFFF1744.toInt()
            speedRatio > 0.5f -> 0xFFFF9100.toInt()
            else -> 0xFF00E676.toInt()
        }
        paint.color = arcColor; paint.strokeWidth = size * 0.08f
        canvas.drawArc(RectF(cx - size * 0.82f, cy - size * 0.82f, cx + size * 0.82f, cy + size * 0.82f), 135f, 270f * speedRatio, false, paint)
        paint.style = Paint.Style.FILL

        // Tick marks
        paint.color = 0x88FFFFFF.toInt()
        for (i in 0..10) {
            val angle = Math.toRadians((135.0 + i * 27.0))
            val inner = size * 0.65f; val outer = size * 0.73f
            canvas.drawLine(cx + (cos(angle) * inner).toFloat(), cy + (sin(angle) * inner).toFloat(), cx + (cos(angle) * outer).toFloat(), cy + (sin(angle) * outer).toFloat(), paint)
        }

        // Needle
        val needleAngle = Math.toRadians((135.0 + 270.0 * speedRatio))
        paint.color = 0xFFFF1744.toInt(); paint.strokeWidth = 2.5f; paint.style = Paint.Style.STROKE
        canvas.drawLine(cx, cy, cx + (cos(needleAngle) * size * 0.65f).toFloat(), cy + (sin(needleAngle) * size * 0.65f).toFloat(), paint)
        paint.style = Paint.Style.FILL; paint.color = 0xFFFF1744.toInt(); canvas.drawCircle(cx, cy, size * 0.06f, paint)
        paint.color = 0xFF222222.toInt(); canvas.drawCircle(cx, cy, size * 0.04f, paint)

        // Speed text
        textPaint.textAlign = Paint.Align.CENTER; textPaint.color = Color.WHITE; textPaint.textSize = size * 0.42f
        canvas.drawText("${p.getSpeedKmh()}", cx, cy + size * 0.32f, textPaint)
        textPaint.textSize = size * 0.18f; textPaint.color = 0x99FFFFFF.toInt()
        canvas.drawText("km/h", cx, cy + size * 0.5f, textPaint)

        // NOS gauge bar below speedometer
        val nosBarW = size * 1.5f; val nosBarH = size * 0.16f
        val nosX = cx - nosBarW / 2; val nosY = cy + size * 0.7f
        paint.color = 0x55000000; canvas.drawRoundRect(RectF(nosX, nosY, nosX + nosBarW, nosY + nosBarH), nosBarH / 2, nosBarH / 2, paint)
        val nosRatio = p.nosAmount / Constants.NOS_MAX
        val nosColor = if (p.nosActive) 0xFF00E5FF.toInt() else 0xFF2979FF.toInt()
        paint.shader = LinearGradient(nosX, nosY, nosX + nosBarW * nosRatio, nosY, darkenColor(nosColor, 0.2f), nosColor, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(RectF(nosX + 2, nosY + 2, nosX + 2 + (nosBarW - 4) * nosRatio, nosY + nosBarH - 2), nosBarH / 2, nosBarH / 2, paint)
        paint.shader = null
        textPaint.textSize = nosBarH * 0.65f; textPaint.color = Color.WHITE
        canvas.drawText("NOS", cx, nosY + nosBarH * 0.72f, textPaint)
    }

    // ---- Polished Controls ----
    fun drawControls(canvas: Canvas, world: GameWorld) {
        val sw = world.screenWidth; val sh = world.screenHeight
        val controlsTop = sh * (1f - Constants.HUD_CONTROLS_HEIGHT_RATIO)
        val controlsH = sh * Constants.HUD_CONTROLS_HEIGHT_RATIO

        // Subtle gradient background for control area
        paint.shader = LinearGradient(0f, controlsTop, 0f, sh, 0x11000000, 0x33000000, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, controlsTop, sw, sh, paint)
        paint.shader = null

        // Brake pedal (left)
        val brakeW = sw * Constants.PEDAL_WIDTH_RATIO
        val brakeH = controlsH * 0.72f
        val brakeX = sw * 0.02f; val brakeY = controlsTop + (controlsH - brakeH) / 2
        val brakeActive = world.player.isBraking
        drawPedal(canvas, brakeX, brakeY, brakeW, brakeH, "BRAKE",
            if (brakeActive) 0xDDFF1744.toInt() else 0x55FF5252.toInt(),
            if (brakeActive) 0xFFFF1744.toInt() else 0x88FF5252.toInt())

        // Gas pedal (right)
        val gasW = sw * Constants.PEDAL_WIDTH_RATIO
        val gasH = controlsH * 0.72f
        val gasX = sw - sw * 0.02f - gasW; val gasY = controlsTop + (controlsH - gasH) / 2
        val gasActive = world.player.isAccelerating
        drawPedal(canvas, gasX, gasY, gasW, gasH, "GAS",
            if (gasActive) 0xDD00E676.toInt() else 0x5569F0AE.toInt(),
            if (gasActive) 0xFF00E676.toInt() else 0x8869F0AE.toInt())

        // Steering zone
        val steerLeft = brakeX + brakeW + sw * 0.03f
        val steerRight = gasX - sw * 0.03f
        val steerCx = (steerLeft + steerRight) / 2
        val steerCy = controlsTop + controlsH / 2
        val steerRadius = controlsH * 0.30f

        // Steering wheel outline
        paint.color = 0x33FFFFFF; paint.style = Paint.Style.STROKE; paint.strokeWidth = 3f
        canvas.drawCircle(steerCx, steerCy, steerRadius, paint)
        paint.style = Paint.Style.FILL

        // Steering dot
        val steerX = steerCx + world.player.steerAngle / Constants.MAX_STEER_ANGLE * steerRadius * 0.8f
        paint.shader = RadialGradient(steerX, steerCy, steerRadius * 0.25f, 0xEEFFFFFF.toInt(), 0x88AAAAAA.toInt(), Shader.TileMode.CLAMP)
        canvas.drawCircle(steerX, steerCy, steerRadius * 0.22f, paint)
        paint.shader = null

        // Direction arrows
        textPaint.textSize = controlsH * 0.17f; textPaint.color = 0x55FFFFFF.toInt(); textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("◀", steerLeft + 15f, steerCy + controlsH * 0.06f, textPaint)
        canvas.drawText("▶", steerRight - 15f, steerCy + controlsH * 0.06f, textPaint)

        // NOS button
        val nosSize = sw * Constants.NOS_BUTTON_SIZE_RATIO
        val nosCx = gasX - nosSize * 0.8f; val nosCy = controlsTop + controlsH * 0.35f
        val nosActive = world.player.nosActive
        val canActivate = world.player.nosAmount >= Constants.NOS_MIN_TO_ACTIVATE

        if (nosActive) {
            // Active glow
            paint.color = 0x2200E5FF; canvas.drawCircle(nosCx, nosCy, nosSize * 1.5f, paint)
            paint.color = 0xFF00E5FF.toInt(); canvas.drawCircle(nosCx, nosCy, nosSize, paint)
        } else {
            paint.color = if (canActivate) 0x882979FF.toInt() else 0x33444444
            canvas.drawCircle(nosCx, nosCy, nosSize, paint)
        }
        // NOS ring
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f
        paint.color = if (nosActive) 0xFF00E5FF.toInt() else if (canActivate) 0x992979FF.toInt() else 0x33666666
        canvas.drawCircle(nosCx, nosCy, nosSize, paint)
        paint.style = Paint.Style.FILL
        textPaint.color = Color.WHITE; textPaint.textSize = nosSize * 0.5f
        canvas.drawText("NOS", nosCx, nosCy + nosSize * 0.16f, textPaint)
    }

    private fun drawPedal(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, label: String, fillColor: Int, borderColor: Int) {
        val cr = min(w, h) * 0.15f
        // Shadow
        paint.color = 0x22000000; canvas.drawRoundRect(RectF(x + 2f, y + 2f, x + w + 2f, y + h + 2f), cr, cr, paint)
        // Fill
        paint.color = fillColor; canvas.drawRoundRect(RectF(x, y, x + w, y + h), cr, cr, paint)
        // Border
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f; paint.color = borderColor
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), cr, cr, paint)
        paint.style = Paint.Style.FILL
        // Label
        textPaint.textAlign = Paint.Align.CENTER; textPaint.textSize = h * 0.15f; textPaint.color = Color.WHITE
        canvas.drawText(label, x + w / 2, y + h * 0.55f, textPaint)
    }

    fun getControlAreas(world: GameWorld): ControlAreas {
        val sw = world.screenWidth; val sh = world.screenHeight
        val ct = sh * (1f - Constants.HUD_CONTROLS_HEIGHT_RATIO); val ch = sh * Constants.HUD_CONTROLS_HEIGHT_RATIO
        val bw = sw * Constants.PEDAL_WIDTH_RATIO; val bh = ch * 0.72f
        val bx = sw * 0.02f; val by = ct + (ch - bh) / 2
        val gw = sw * Constants.PEDAL_WIDTH_RATIO; val gh = ch * 0.72f
        val gx = sw - sw * 0.02f - gw; val gy = ct + (ch - gh) / 2
        val sl = bx + bw + sw * 0.03f; val sr = gx - sw * 0.03f
        val ns = sw * Constants.NOS_BUTTON_SIZE_RATIO
        val nx = gx - ns * 0.8f; val ny = ct + ch * 0.35f
        val hh = sh * 0.058f
        val ps = hh * 0.35f; val px = sw - sw * 0.025f - ps * 1.2f
        return ControlAreas(
            brake = RectF(bx, by, bx + bw, by + bh),
            gas = RectF(gx, gy, gx + gw, gy + gh),
            steerLeft = sl, steerRight = sr, steerCy = ct + ch / 2,
            nosCx = nx, nosCy = ny, nosRadius = ns * 1.5f,
            pauseArea = RectF(px - ps * 2f, 0f, px + ps * 2f, hh)
        )
    }

    private fun drawActivePowerUps(canvas: Canvas, world: GameWorld, hh: Float) {
        val p = world.player; val now = System.currentTimeMillis(); val iconS = world.screenHeight * 0.016f
        var oy = hh + world.screenHeight * 0.045f; val x = world.screenWidth * 0.04f
        val active = mutableListOf<Pair<String, Float>>()
        if (p.hasShield) active.add("SHIELD" to ((p.shieldEndTime - now) / 1000f))
        if (p.hasMagnet) active.add("MAGNET" to ((p.magnetEndTime - now) / 1000f))
        if (p.hasDoubleScore) active.add("2x SCORE" to ((p.doubleScoreEndTime - now) / 1000f))
        for ((name, timeLeft) in active) {
            val bw = world.screenWidth * 0.17f; val bh = iconS * 0.8f
            paint.color = 0x44000000; canvas.drawRoundRect(RectF(x, oy - bh / 2, x + bw, oy + bh / 2), bh / 2, bh / 2, paint)
            val maxT = when (name) { "SHIELD" -> Constants.SHIELD_DURATION_MS / 1000f; "MAGNET" -> Constants.MAGNET_DURATION_MS / 1000f; else -> Constants.DOUBLE_SCORE_DURATION_MS / 1000f }
            val prog = (timeLeft / maxT).coerceIn(0f, 1f)
            paint.color = when (name) { "SHIELD" -> 0xFF00E5FF.toInt(); "MAGNET" -> 0xFFFF6F00.toInt(); else -> 0xFFFFD700.toInt() }
            canvas.drawRoundRect(RectF(x + 1, oy - bh / 2 + 1, x + 1 + (bw - 2) * prog, oy + bh / 2 - 1), bh / 2, bh / 2, paint)
            textPaint.color = Color.WHITE; textPaint.textSize = iconS * 0.7f; textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(name, x + 4f, oy + iconS * 0.22f, textPaint)
            oy += bh + iconS * 0.4f
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
        // Dark gradient background
        paint.shader = LinearGradient(0f, 0f, 0f, world.screenHeight, 0xFF0D1B2A.toInt(), 0xFF1B2838.toInt(), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, world.screenWidth, world.screenHeight, paint)
        paint.shader = null

        val t = System.currentTimeMillis()
        // Animated speed lines background
        for (i in 0 until 20) {
            val seed = i * 997L; val x = ((seed * 7) % world.screenWidth.toLong()).toFloat()
            val baseY = ((t * 0.3f + seed * 3) % (world.screenHeight * 1.5f)) - world.screenHeight * 0.25f
            paint.color = 0x0AFFFFFF; paint.strokeWidth = 1.5f
            canvas.drawLine(x, baseY, x, baseY + 40f + (seed % 35), paint)
        }

        val sw = world.screenWidth; val sh = world.screenHeight

        // Title
        val titleY = sh * 0.14f
        textPaint.textAlign = Paint.Align.CENTER
        // Shadow
        textPaint.textSize = sw * 0.11f; textPaint.color = 0x44000000
        canvas.drawText("TRAFFIC", sw / 2 + 2f, titleY + 2f, textPaint)
        textPaint.color = 0xFF42A5F5.toInt()
        canvas.drawText("TRAFFIC", sw / 2, titleY, textPaint)

        textPaint.textSize = sw * 0.14f; textPaint.color = 0x44000000
        canvas.drawText("RACER", sw / 2 + 2f, titleY + sw * 0.13f + 2f, textPaint)
        textPaint.color = 0xFFFF5722.toInt()
        canvas.drawText("RACER", sw / 2, titleY + sw * 0.13f, textPaint)

        textPaint.textSize = sw * 0.035f; textPaint.color = 0xFF00E5FF.toInt()
        canvas.drawText("NFS EDITION", sw / 2, titleY + sw * 0.19f, textPaint)

        // Car preview with sprite
        val carY = sh * 0.4f; val bob = sin(t * 0.003) * 6f
        val selCar = world.gameData.getSelectedCar()
        shadowPaint.color = 0x22000000
        canvas.drawOval(RectF(sw / 2 - sw * 0.12f, carY + sh * 0.06f, sw / 2 + sw * 0.12f, carY + sh * 0.09f), shadowPaint)
        val carW = sw * 0.17f; val carH = sh * 0.095f
        val startSprite = spriteManager.getScaled(spriteManager.getPlayerSprite(selCar.id), carW * 1.3f, carH * 1.3f)
        if (startSprite != null && !startSprite.isRecycled) {
            canvas.drawBitmap(startSprite, sw / 2 - startSprite.width / 2f, carY + bob.toFloat() - startSprite.height / 2f, bitmapPaint)
        } else {
            drawDetailedCar(canvas, sw / 2, carY + bob.toFloat(), carW, carH, selCar.color, true)
        }

        // Buttons
        val btnW = sw * 0.52f; val btnH = sh * 0.06f

        // RACE button with pulse
        val pulse = 1f + sin(t * 0.004f).toFloat() * 0.015f
        val playY = sh * 0.55f
        drawMenuButton(canvas, sw / 2, playY, btnW * pulse, btnH * pulse, "RACE!", 0xFF4CAF50.toInt(), 0xFF388E3C.toInt())

        // Garage
        val garageY = sh * 0.64f
        drawMenuButton(canvas, sw / 2, garageY, btnW, btnH, "GARAGE", 0xFF1565C0.toInt(), 0xFF0D47A1.toInt())

        // Missions
        val missY = sh * 0.73f
        drawMenuButton(canvas, sw / 2, missY, btnW, btnH, "MISSIONS", 0xFFFF6F00.toInt(), 0xFFE65100.toInt())

        // Stats
        textPaint.textSize = sw * 0.033f; textPaint.color = 0x99FFFFFF.toInt()
        canvas.drawText("High Score: ${world.gameData.highScore}", sw / 2, sh * 0.83f, textPaint)
        textPaint.color = 0xFFFFD700.toInt()
        canvas.drawText("Coins: ${world.gameData.totalCoins}", sw / 2, sh * 0.875f, textPaint)

        textPaint.color = 0x55FFFFFF.toInt(); textPaint.textSize = sw * 0.025f
        canvas.drawText("Steer  •  Gas  •  Brake  •  NOS  •  Drift", sw / 2, sh * 0.95f, textPaint)
    }

    private fun drawMenuButton(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float, text: String, color: Int, darkColor: Int) {
        val cr = h / 2
        // Shadow
        paint.color = 0x33000000; canvas.drawRoundRect(RectF(cx - w / 2 + 3f, cy - h / 2 + 3f, cx + w / 2 + 3f, cy + h / 2 + 3f), cr, cr, paint)
        // Gradient fill
        paint.shader = LinearGradient(cx - w / 2, cy - h / 2, cx - w / 2, cy + h / 2, color, darkColor, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(RectF(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2), cr, cr, paint)
        paint.shader = null
        // Top highlight
        paint.color = 0x22FFFFFF
        canvas.drawRoundRect(RectF(cx - w / 2 + w * 0.05f, cy - h / 2 + 1f, cx + w / 2 - w * 0.05f, cy - h * 0.1f), cr, cr, paint)
        // Text
        textPaint.textAlign = Paint.Align.CENTER; textPaint.color = Color.WHITE; textPaint.textSize = h * 0.42f
        canvas.drawText(text, cx, cy + h * 0.14f, textPaint)
    }

    fun drawGarageScreen(canvas: Canvas, world: GameWorld) {
        paint.shader = LinearGradient(0f, 0f, 0f, world.screenHeight, 0xFF0D1B2A.toInt(), 0xFF1A1A2E.toInt(), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, world.screenWidth, world.screenHeight, paint); paint.shader = null

        val sw = world.screenWidth; val sh = world.screenHeight

        textPaint.textAlign = Paint.Align.CENTER; textPaint.color = 0xFF42A5F5.toInt(); textPaint.textSize = sw * 0.07f
        canvas.drawText("GARAGE", sw / 2, sh * 0.07f, textPaint)
        textPaint.color = 0xFFFFD700.toInt(); textPaint.textSize = sw * 0.035f
        canvas.drawText("Coins: ${world.gameData.totalCoins}", sw / 2, sh * 0.115f, textPaint)

        val car = PlayerCarDef.ALL_CARS[world.garageSelectedIndex]
        val unlocked = world.gameData.isCarUnlocked(car.id)
        val equipped = world.gameData.selectedCarId == car.id

        // Car preview with sprite
        val carY = sh * 0.27f; val bob = sin(System.currentTimeMillis() * 0.003) * 4f
        shadowPaint.color = 0x22000000
        canvas.drawOval(RectF(sw / 2 - sw * 0.14f, carY + sh * 0.07f, sw / 2 + sw * 0.14f, carY + sh * 0.1f), shadowPaint)
        val gcW = sw * 0.2f; val gcH = sh * 0.12f
        val garageSprite = spriteManager.getScaled(spriteManager.getPlayerSprite(car.id), gcW * 1.4f, gcH * 1.4f)
        if (garageSprite != null && !garageSprite.isRecycled) {
            canvas.drawBitmap(garageSprite, sw / 2 - garageSprite.width / 2f, carY + bob.toFloat() - garageSprite.height / 2f, bitmapPaint)
        } else {
            drawDetailedCar(canvas, sw / 2, carY + bob.toFloat(), gcW, gcH, car.color, true)
        }

        // Name & desc
        textPaint.textSize = sw * 0.055f; textPaint.color = Color.WHITE
        canvas.drawText(car.name, sw / 2, sh * 0.42f, textPaint)
        textPaint.textSize = sw * 0.028f; textPaint.color = 0x99FFFFFF.toInt()
        canvas.drawText(car.description, sw / 2, sh * 0.455f, textPaint)

        // Stats
        val barY = sh * 0.49f; val barW = sw * 0.48f; val barH = sh * 0.018f; val barX = sw * 0.26f
        drawStatBar(canvas, "SPEED", car.baseSpeed / 1.4f, barX, barY, barW, barH, 0xFF4CAF50.toInt())
        drawStatBar(canvas, "HANDLING", car.baseHandling / 1.4f, barX, barY + barH * 2.8f, barW, barH, 0xFF2196F3.toInt())
        drawStatBar(canvas, "NITRO", car.baseNitro / 1.5f, barX, barY + barH * 5.6f, barW, barH, 0xFFFF9800.toInt())

        // Nav arrows
        val arrowY = sh * 0.28f
        if (world.garageSelectedIndex > 0) { textPaint.textSize = sw * 0.07f; textPaint.color = 0x88FFFFFF.toInt(); canvas.drawText("◀", sw * 0.07f, arrowY, textPaint) }
        if (world.garageSelectedIndex < PlayerCarDef.ALL_CARS.size - 1) { textPaint.textSize = sw * 0.07f; canvas.drawText("▶", sw * 0.93f, arrowY, textPaint) }

        // Action button
        val btnW = sw * 0.48f; val btnH = sh * 0.055f; val btnY = sh * 0.68f
        if (!unlocked) {
            drawMenuButton(canvas, sw / 2, btnY, btnW, btnH, "BUY - ${car.price} coins", 0xFFFF6F00.toInt(), 0xFFE65100.toInt())
        } else if (!equipped) {
            drawMenuButton(canvas, sw / 2, btnY, btnW, btnH, "EQUIP", 0xFF4CAF50.toInt(), 0xFF388E3C.toInt())
        } else {
            textPaint.color = 0xFF00E676.toInt(); textPaint.textSize = btnH * 0.42f
            canvas.drawText("EQUIPPED", sw / 2, btnY + btnH * 0.14f, textPaint)
        }

        // Back button
        drawMenuButton(canvas, sw / 2, sh * 0.77f, btnW, btnH, "BACK", 0x55FFFFFF, 0x44FFFFFF)

        // Dots
        val dotY = sh * 0.84f; val dotSpacing = sw * 0.022f
        val totalW = (PlayerCarDef.ALL_CARS.size - 1) * dotSpacing
        for (i in PlayerCarDef.ALL_CARS.indices) {
            paint.color = if (i == world.garageSelectedIndex) 0xFFFFFFFF.toInt() else 0x44FFFFFF
            canvas.drawCircle(sw / 2 - totalW / 2 + i * dotSpacing, dotY, if (i == world.garageSelectedIndex) 4f else 2.5f, paint)
        }
    }

    fun drawMissionsScreen(canvas: Canvas, world: GameWorld) {
        paint.shader = LinearGradient(0f, 0f, 0f, world.screenHeight, 0xFF0D1B2A.toInt(), 0xFF1A1A2E.toInt(), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, world.screenWidth, world.screenHeight, paint); paint.shader = null

        val sw = world.screenWidth; val sh = world.screenHeight
        textPaint.textAlign = Paint.Align.CENTER; textPaint.color = 0xFFFF6F00.toInt(); textPaint.textSize = sw * 0.07f
        canvas.drawText("MISSIONS", sw / 2, sh * 0.07f, textPaint)

        val missions = world.currentMissions
        val startY = sh * 0.12f; val mh = sh * 0.075f
        for (i in missions.indices) {
            val m = missions[i]; val my = startY + i * (mh + sh * 0.012f)
            val done = world.gameData.isMissionCompleted(m.id)
            val progress = world.missionProgress[m.id] ?: 0
            val progRatio = (progress.toFloat() / m.target).coerceIn(0f, 1f)

            paint.color = if (done) 0x33004D40 else 0x33263238
            canvas.drawRoundRect(RectF(sw * 0.04f, my, sw * 0.96f, my + mh), 10f, 10f, paint)

            textPaint.textAlign = Paint.Align.LEFT; textPaint.textSize = mh * 0.28f
            textPaint.color = if (done) 0xFF00E676.toInt() else 0xDDFFFFFF.toInt()
            canvas.drawText(m.description, sw * 0.07f, my + mh * 0.4f, textPaint)
            textPaint.textSize = mh * 0.2f; textPaint.color = 0xFFFFD700.toInt()
            canvas.drawText("+${m.reward} coins", sw * 0.07f, my + mh * 0.68f, textPaint)

            if (!done) {
                val barX = sw * 0.58f; val barW2 = sw * 0.32f; val barH2 = mh * 0.13f; val barY2 = my + mh * 0.55f
                paint.color = 0x33FFFFFF; canvas.drawRoundRect(RectF(barX, barY2, barX + barW2, barY2 + barH2), barH2 / 2, barH2 / 2, paint)
                paint.color = 0xFF4CAF50.toInt(); canvas.drawRoundRect(RectF(barX, barY2, barX + barW2 * progRatio, barY2 + barH2), barH2 / 2, barH2 / 2, paint)
                textPaint.textAlign = Paint.Align.RIGHT; textPaint.textSize = mh * 0.2f; textPaint.color = 0x99FFFFFF.toInt()
                canvas.drawText("$progress/${m.target}", sw * 0.93f, my + mh * 0.4f, textPaint)
            } else {
                textPaint.textAlign = Paint.Align.RIGHT; textPaint.textSize = mh * 0.28f; textPaint.color = 0xFF00E676.toInt()
                canvas.drawText("DONE", sw * 0.93f, my + mh * 0.5f, textPaint)
            }
        }

        val btnW = sw * 0.48f; val btnH = sh * 0.055f
        drawMenuButton(canvas, sw / 2, sh * 0.85f, btnW, btnH, "BACK", 0x55FFFFFF, 0x44FFFFFF)
    }

    fun drawGameOverScreen(canvas: Canvas, world: GameWorld) {
        paint.color = 0xDD000000.toInt(); canvas.drawRect(0f, 0f, world.screenWidth, world.screenHeight, paint)
        val sw = world.screenWidth; val sh = world.screenHeight
        val cy = sh * 0.16f

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = sw * 0.09f; textPaint.color = 0x44000000
        canvas.drawText("GAME OVER", sw / 2 + 2f, cy + 2f, textPaint)
        textPaint.color = 0xFFEF5350.toInt()
        canvas.drawText("GAME OVER", sw / 2, cy, textPaint)

        val isNew = world.player.score >= world.gameData.highScore && world.player.score > 0
        if (isNew) {
            val flash = ((sin(System.currentTimeMillis() * 0.005) + 1f) / 2f * 255).toInt()
            textPaint.color = (0xFF shl 24) or (flash shl 16) or (0xD7 shl 8)
            textPaint.textSize = sw * 0.045f
            canvas.drawText("NEW HIGH SCORE!", sw / 2, cy + sh * 0.045f, textPaint)
        }

        val sy = cy + sh * 0.09f; val ss = sh * 0.04f; val p = world.player
        val lx = sw * 0.18f; val vx = sw * 0.82f

        drawStatLine(canvas, "DISTANCE", "${p.distanceScore.toLong()}m", lx, vx, sy, ss, Color.WHITE)
        drawStatLine(canvas, "TOP SPEED", "${p.getSpeedKmh()} km/h", lx, vx, sy + ss, ss, 0xFF42A5F5.toInt())
        drawStatLine(canvas, "COINS", "${p.coinsCollectedThisRun}", lx, vx, sy + ss * 2, ss, 0xFFFFD700.toInt())
        drawStatLine(canvas, "NEAR MISSES", "${p.nearMissesThisRun}", lx, vx, sy + ss * 3, ss, 0xFF00E676.toInt())
        drawStatLine(canvas, "OVERTAKES", "${p.overtakesThisRun}", lx, vx, sy + ss * 4, ss, 0xFF64FFDA.toInt())
        drawStatLine(canvas, "BEST COMBO", "x${p.maxComboThisRun}", lx, vx, sy + ss * 5, ss, 0xFFFF6D00.toInt())

        // Separator
        paint.color = 0x33FFFFFF; canvas.drawRect(sw * 0.15f, sy + ss * 5.8f, sw * 0.85f, sy + ss * 5.8f + 1f, paint)

        drawStatLine(canvas, "TOTAL SCORE", "${p.score}", lx, vx, sy + ss * 6.5f, ss * 1.2f, 0xFF4CAF50.toInt())

        // Achievements
        if (world.newAchievements.isNotEmpty()) {
            val ay = sy + ss * 8f
            textPaint.color = 0xFFFFD700.toInt(); textPaint.textSize = sw * 0.035f; textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("ACHIEVEMENTS UNLOCKED!", sw / 2, ay, textPaint)
            for ((i, a) in world.newAchievements.withIndex()) {
                textPaint.textSize = sw * 0.028f; textPaint.color = 0xFFFFD700.toInt()
                canvas.drawText("${a.icon} ${a.name}", sw / 2, ay + (i + 1) * sh * 0.028f, textPaint)
            }
        }

        val btnW = sw * 0.52f; val btnH = sh * 0.06f
        drawMenuButton(canvas, sw / 2, sh * 0.82f, btnW, btnH, "RACE AGAIN", 0xFF4CAF50.toInt(), 0xFF388E3C.toInt())

        textPaint.color = 0x55FFFFFF.toInt(); textPaint.textSize = sw * 0.025f
        canvas.drawText("Tap to continue", sw / 2, sh * 0.92f, textPaint)
    }

    private fun drawPauseOverlay(canvas: Canvas, world: GameWorld) {
        paint.color = 0xBB000000.toInt(); canvas.drawRect(0f, 0f, world.screenWidth, world.screenHeight, paint)
        val sw = world.screenWidth; val sh = world.screenHeight
        textPaint.textAlign = Paint.Align.CENTER; textPaint.color = Color.WHITE; textPaint.textSize = sw * 0.09f
        canvas.drawText("PAUSED", sw / 2, sh * 0.4f, textPaint)
        drawMenuButton(canvas, sw / 2, sh * 0.55f, sw * 0.48f, sh * 0.06f, "RESUME", 0xFF4CAF50.toInt(), 0xFF388E3C.toInt())
    }

    // ---- Helpers ----
    private fun drawStatBar(canvas: Canvas, label: String, ratio: Float, x: Float, y: Float, w: Float, h: Float, color: Int) {
        textPaint.textAlign = Paint.Align.LEFT; textPaint.textSize = h * 0.85f; textPaint.color = 0x99FFFFFF.toInt()
        canvas.drawText(label, x, y - h * 0.3f, textPaint)
        paint.color = 0x33FFFFFF; canvas.drawRoundRect(RectF(x, y, x + w, y + h), h / 2, h / 2, paint)
        paint.shader = LinearGradient(x, y, x + w * ratio.coerceIn(0f, 1f), y, darkenColor(color, 0.2f), color, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(RectF(x, y, x + w * ratio.coerceIn(0f, 1f), y + h), h / 2, h / 2, paint)
        paint.shader = null
    }

    private fun drawStatLine(canvas: Canvas, label: String, value: String, lx: Float, vx: Float, y: Float, size: Float, valueColor: Int) {
        textPaint.textAlign = Paint.Align.LEFT; textPaint.color = 0x88FFFFFF.toInt(); textPaint.textSize = size * 0.65f
        canvas.drawText(label, lx, y, textPaint)
        textPaint.textAlign = Paint.Align.RIGHT; textPaint.color = valueColor; textPaint.textSize = size * 0.75f
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
    private fun darkenColor(c: Int, f: Float) = Color.rgb((Color.red(c) * (1f - f)).toInt().coerceIn(0, 255), (Color.green(c) * (1f - f)).toInt().coerceIn(0, 255), (Color.blue(c) * (1f - f)).toInt().coerceIn(0, 255))
    private fun lightenColor(c: Int, f: Float) = Color.rgb(min(255, Color.red(c) + ((255 - Color.red(c)) * f).toInt()), min(255, Color.green(c) + ((255 - Color.green(c)) * f).toInt()), min(255, Color.blue(c) + ((255 - Color.blue(c)) * f).toInt()))
}

data class ControlAreas(
    val brake: RectF, val gas: RectF,
    val steerLeft: Float, val steerRight: Float, val steerCy: Float,
    val nosCx: Float, val nosCy: Float, val nosRadius: Float,
    val pauseArea: RectF
)
