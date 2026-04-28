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

        drawSky(canvas, world)
        drawGrass(canvas, world)
        drawRoad(canvas, world)
        drawRoadMarkings(canvas, world)
        drawScenery(canvas, world)
        drawCoins(canvas, world)
        drawPowerUps(canvas, world)
        drawTraffic(canvas, world)
        drawPlayer(canvas, world)
        drawParticles(canvas, world)

        if (world.player.isBoosting) {
            drawSpeedLines(canvas, world)
        }

        canvas.restore()

        when (world.state) {
            GameState.PLAYING -> drawHUD(canvas, world)
            GameState.START_SCREEN -> drawStartScreen(canvas, world)
            GameState.GAME_OVER -> drawGameOverScreen(canvas, world)
            GameState.PAUSED -> {
                drawHUD(canvas, world)
                drawPauseOverlay(canvas, world)
            }
        }
    }

    private fun drawSky(canvas: Canvas, world: GameWorld) {
        val dayTop = Color.rgb(135, 206, 235)
        val dayBottom = Color.rgb(176, 226, 255)
        val nightTop = Color.rgb(10, 10, 40)
        val nightBottom = Color.rgb(25, 25, 80)

        val topColor = blendColors(dayTop, nightTop, world.nightFactor)
        val bottomColor = blendColors(dayBottom, nightBottom, world.nightFactor)

        paint.shader = LinearGradient(
            0f, 0f, 0f, world.screenHeight,
            topColor, bottomColor,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, world.screenWidth, world.screenHeight, paint)
        paint.shader = null
    }

    private fun drawGrass(canvas: Canvas, world: GameWorld) {
        val grassColor = blendColors(0xFF2E7D32.toInt(), 0xFF1B3A1B.toInt(), world.nightFactor)
        val grassLight = blendColors(0xFF388E3C.toInt(), 0xFF1E4420.toInt(), world.nightFactor)

        paint.color = grassColor
        canvas.drawRect(0f, 0f, world.roadLeft - world.shoulderWidth, world.screenHeight, paint)
        canvas.drawRect(
            world.roadRight + world.shoulderWidth, 0f,
            world.screenWidth, world.screenHeight, paint
        )

        paint.color = grassLight
        val stripeWidth = world.screenWidth * 0.015f
        var x = 0f
        while (x < world.roadLeft - world.shoulderWidth) {
            canvas.drawRect(x, 0f, x + stripeWidth, world.screenHeight, paint)
            x += stripeWidth * 3f
        }
        x = world.roadRight + world.shoulderWidth
        while (x < world.screenWidth) {
            canvas.drawRect(x, 0f, x + stripeWidth, world.screenHeight, paint)
            x += stripeWidth * 3f
        }
    }

    private fun drawRoad(canvas: Canvas, world: GameWorld) {
        val roadColor = blendColors(0xFF424242.toInt(), 0xFF1A1A1A.toInt(), world.nightFactor)
        val shoulderColor = blendColors(0xFF616161.toInt(), 0xFF2A2A2A.toInt(), world.nightFactor)

        paint.color = shoulderColor
        canvas.drawRect(
            world.roadLeft - world.shoulderWidth, 0f,
            world.roadLeft, world.screenHeight, paint
        )
        canvas.drawRect(
            world.roadRight, 0f,
            world.roadRight + world.shoulderWidth, world.screenHeight, paint
        )

        paint.color = roadColor
        canvas.drawRect(world.roadLeft, 0f, world.roadRight, world.screenHeight, paint)

        paint.color = Color.WHITE
        paint.strokeWidth = world.screenWidth * 0.008f
        canvas.drawLine(world.roadLeft, 0f, world.roadLeft, world.screenHeight, paint)
        canvas.drawLine(world.roadRight, 0f, world.roadRight, world.screenHeight, paint)

        paint.color = Color.YELLOW
        paint.strokeWidth = world.screenWidth * 0.006f
        canvas.drawLine(
            world.roadLeft - world.shoulderWidth / 2, 0f,
            world.roadLeft - world.shoulderWidth / 2, world.screenHeight, paint
        )
        canvas.drawLine(
            world.roadRight + world.shoulderWidth / 2, 0f,
            world.roadRight + world.shoulderWidth / 2, world.screenHeight, paint
        )
    }

    private fun drawRoadMarkings(canvas: Canvas, world: GameWorld) {
        val markingWidth = world.screenWidth * Constants.ROAD_MARKING_WIDTH_RATIO
        val markingLength = world.screenHeight * Constants.ROAD_MARKING_LENGTH_RATIO
        paint.color = blendColors(Color.WHITE, 0xFFCCCCCC.toInt(), world.nightFactor)

        for (i in 1 until Constants.NUM_LANES) {
            val laneX = world.roadLeft + world.laneWidth * i
            for (marking in world.roadMarkings) {
                canvas.drawRoundRect(
                    RectF(
                        laneX - markingWidth / 2, marking.y,
                        laneX + markingWidth / 2, marking.y + markingLength
                    ),
                    markingWidth, markingWidth, paint
                )
            }
        }
    }

    private fun drawScenery(canvas: Canvas, world: GameWorld) {
        for (obj in world.sceneryObjects) {
            drawTree(canvas, obj, world)
        }
    }

    private fun drawTree(canvas: Canvas, obj: SceneryObject, world: GameWorld) {
        val baseWidth = world.screenWidth * Constants.TREE_WIDTH_RATIO * obj.scale
        val x = obj.x
        val y = obj.y

        when (obj.type) {
            0 -> {
                val trunkColor = blendColors(0xFF5D4037.toInt(), 0xFF3E2723.toInt(), world.nightFactor)
                val leafColor = blendColors(0xFF2E7D32.toInt(), 0xFF1B3A1B.toInt(), world.nightFactor)
                paint.color = trunkColor
                canvas.drawRect(x - baseWidth * 0.1f, y, x + baseWidth * 0.1f, y + baseWidth * 0.8f, paint)
                paint.color = leafColor
                path.reset()
                path.moveTo(x, y - baseWidth * 0.6f)
                path.lineTo(x - baseWidth * 0.45f, y + baseWidth * 0.15f)
                path.lineTo(x + baseWidth * 0.45f, y + baseWidth * 0.15f)
                path.close()
                canvas.drawPath(path, paint)
            }
            1 -> {
                val bushColor = blendColors(0xFF388E3C.toInt(), 0xFF1E4420.toInt(), world.nightFactor)
                paint.color = bushColor
                canvas.drawCircle(x, y, baseWidth * 0.35f, paint)
                paint.color = blendColors(0xFF43A047.toInt(), 0xFF2E5A2E.toInt(), world.nightFactor)
                canvas.drawCircle(x - baseWidth * 0.15f, y - baseWidth * 0.1f, baseWidth * 0.2f, paint)
            }
            2 -> {
                val poleColor = blendColors(0xFF9E9E9E.toInt(), 0xFF616161.toInt(), world.nightFactor)
                paint.color = poleColor
                paint.strokeWidth = baseWidth * 0.08f
                canvas.drawLine(x, y, x, y - baseWidth * 1.2f, paint)
                paint.color = blendColors(0xFF1565C0.toInt(), 0xFF0D3B8C.toInt(), world.nightFactor)
                canvas.drawRoundRect(
                    RectF(x - baseWidth * 0.3f, y - baseWidth * 1.2f, x + baseWidth * 0.3f, y - baseWidth * 0.7f),
                    baseWidth * 0.05f, baseWidth * 0.05f, paint
                )
                textPaint.color = Color.WHITE
                textPaint.textSize = baseWidth * 0.2f
                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("→", x, y - baseWidth * 0.85f, textPaint)
            }
        }
    }

    private fun drawPlayer(canvas: Canvas, world: GameWorld) {
        val p = world.player
        if (p.isInvincible) {
            val blink = (System.currentTimeMillis() / 100) % 3 != 0L
            if (!blink) return
        }

        canvas.save()
        val tiltAngle = when {
            abs(p.x - world.getLaneCenter(p.targetLaneIndex)) > 2f -> {
                if (p.targetLaneIndex > p.laneIndex) -3f else 3f
            }
            else -> 0f
        }
        canvas.rotate(tiltAngle, p.x, p.y)

        drawCarBody(canvas, p.x, p.y, p.width, p.height, 0xFFE53935.toInt(), true)

        canvas.restore()

        if (p.hasShield) {
            drawShieldEffect(canvas, p)
        }
        if (p.isBoosting) {
            drawBoostFlame(canvas, p)
        }
    }

    private fun drawCarBody(
        canvas: Canvas, cx: Float, cy: Float,
        width: Float, height: Float, color: Int, isPlayer: Boolean
    ) {
        val left = cx - width / 2
        val top = cy - height / 2
        val right = cx + width / 2
        val bottom = cy + height / 2
        val cornerRadius = width * 0.15f

        paint.color = darkenColor(color, 0.3f)
        canvas.drawRoundRect(
            RectF(left - width * 0.08f, top + height * 0.08f,
                left + width * 0.02f, top + height * 0.28f),
            2f, 2f, paint
        )
        canvas.drawRoundRect(
            RectF(right - width * 0.02f, top + height * 0.08f,
                right + width * 0.08f, top + height * 0.28f),
            2f, 2f, paint
        )
        canvas.drawRoundRect(
            RectF(left - width * 0.08f, top + height * 0.7f,
                left + width * 0.02f, top + height * 0.9f),
            2f, 2f, paint
        )
        canvas.drawRoundRect(
            RectF(right - width * 0.02f, top + height * 0.7f,
                right + width * 0.08f, top + height * 0.9f),
            2f, 2f, paint
        )

        paint.color = color
        canvas.drawRoundRect(RectF(left, top, right, bottom), cornerRadius, cornerRadius, paint)

        paint.color = lightenColor(color, 0.15f)
        canvas.drawRoundRect(
            RectF(left + width * 0.08f, top + width * 0.05f,
                right - width * 0.08f, top + height * 0.15f),
            cornerRadius * 0.5f, cornerRadius * 0.5f, paint
        )

        if (isPlayer) {
            paint.color = 0xFF64B5F6.toInt()
        } else {
            paint.color = 0xFF90CAF9.toInt()
        }
        canvas.drawRoundRect(
            RectF(left + width * 0.12f, top + height * 0.18f,
                right - width * 0.12f, top + height * 0.35f),
            cornerRadius * 0.3f, cornerRadius * 0.3f, paint
        )
        canvas.drawRoundRect(
            RectF(left + width * 0.12f, top + height * 0.62f,
                right - width * 0.12f, top + height * 0.75f),
            cornerRadius * 0.3f, cornerRadius * 0.3f, paint
        )

        if (isPlayer) {
            paint.color = 0xFFFFEE58.toInt()
            canvas.drawRoundRect(
                RectF(left + width * 0.1f, top, left + width * 0.3f, top + height * 0.04f),
                2f, 2f, paint
            )
            canvas.drawRoundRect(
                RectF(right - width * 0.3f, top, right - width * 0.1f, top + height * 0.04f),
                2f, 2f, paint
            )
            paint.color = 0xFFEF5350.toInt()
            canvas.drawRoundRect(
                RectF(left + width * 0.1f, bottom - height * 0.04f, left + width * 0.3f, bottom),
                2f, 2f, paint
            )
            canvas.drawRoundRect(
                RectF(right - width * 0.3f, bottom - height * 0.04f, right - width * 0.1f, bottom),
                2f, 2f, paint
            )
        }
    }

    private fun drawShieldEffect(canvas: Canvas, player: PlayerCar) {
        val phase = (System.currentTimeMillis() % 1000) / 1000f
        val radius = player.width * 0.8f + sin(phase * Math.PI.toFloat() * 2f) * 5f
        paint.color = (((180 + (sin(phase * Math.PI.toFloat() * 2f) * 40).toInt())
            .coerceIn(0, 255)) shl 24) or 0x00E5FF
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawCircle(player.x, player.y, radius, paint)
        paint.style = Paint.Style.FILL
        paint.color = 0x2000E5FF
        canvas.drawCircle(player.x, player.y, radius, paint)
    }

    private fun drawBoostFlame(canvas: Canvas, player: PlayerCar) {
        val time = System.currentTimeMillis()
        val flicker = sin(time * 0.02f) * 0.3f + 0.7f
        val flameHeight = player.height * 0.4f * flicker

        for (i in 0 until 3) {
            val offsetX = (i - 1) * player.width * 0.15f
            val alpha = ((200 - i * 40).coerceIn(0, 255))

            paint.color = (alpha shl 24) or 0xFF6D00
            path.reset()
            path.moveTo(player.x + offsetX - player.width * 0.08f, player.y + player.height / 2)
            path.lineTo(player.x + offsetX + player.width * 0.08f, player.y + player.height / 2)
            path.lineTo(
                player.x + offsetX + sin(time * 0.015f + i) * 3f,
                player.y + player.height / 2 + flameHeight * (1f - i * 0.2f)
            )
            path.close()
            canvas.drawPath(path, paint)
        }

        paint.color = 0xCCFFAB00.toInt()
        path.reset()
        path.moveTo(player.x - player.width * 0.05f, player.y + player.height / 2)
        path.lineTo(player.x + player.width * 0.05f, player.y + player.height / 2)
        path.lineTo(player.x, player.y + player.height / 2 + flameHeight * 0.6f)
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawTraffic(canvas: Canvas, world: GameWorld) {
        for (car in world.trafficCars) {
            drawCarBody(canvas, car.x, car.y, car.width, car.height, car.color, false)
        }
    }

    private fun drawCoins(canvas: Canvas, world: GameWorld) {
        for (coin in world.coins) {
            if (coin.collected) {
                val scale = 1f - coin.collectAnimProgress
                val alpha = (255 * scale).toInt().coerceIn(0, 255)
                drawCoin(canvas, coin.x, coin.y - coin.collectAnimProgress * 30f,
                    coin.size * scale, coin.rotation, alpha)
            } else {
                drawCoin(canvas, coin.x, coin.y, coin.size, coin.rotation, 255)
            }
        }
    }

    private fun drawCoin(canvas: Canvas, x: Float, y: Float, size: Float, rotation: Float, alpha: Int) {
        val scaleX = abs(cos(Math.toRadians(rotation.toDouble()))).toFloat() * 0.5f + 0.5f

        paint.color = (alpha shl 24) or 0xFFD700
        canvas.drawOval(
            RectF(x - size * scaleX, y - size, x + size * scaleX, y + size),
            paint
        )

        paint.color = (alpha shl 24) or 0xFFC107
        canvas.drawOval(
            RectF(x - size * scaleX * 0.7f, y - size * 0.7f,
                x + size * scaleX * 0.7f, y + size * 0.7f),
            paint
        )

        if (scaleX > 0.6f) {
            textPaint.color = (alpha shl 24) or 0xFFD700
            textPaint.textSize = size * 1.1f
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("$", x, y + size * 0.35f, textPaint)
        }
    }

    private fun drawPowerUps(canvas: Canvas, world: GameWorld) {
        for (pu in world.powerUps) {
            val pulse = 1f + sin(pu.pulsePhase) * 0.15f
            val size = pu.size * pulse
            val glowAlpha = ((sin(pu.pulsePhase) * 0.3f + 0.3f) * 255).toInt().coerceIn(0, 255)

            paint.color = (glowAlpha shl 24) or (pu.type.color and 0x00FFFFFF)
            canvas.drawCircle(pu.x, pu.y, size * 1.3f, paint)

            paint.color = pu.type.color
            canvas.drawCircle(pu.x, pu.y, size * 0.7f, paint)

            paint.color = lightenColor(pu.type.color, 0.4f)
            canvas.drawCircle(pu.x, pu.y, size * 0.4f, paint)

            textPaint.color = Color.WHITE
            textPaint.textSize = size * 0.7f
            textPaint.textAlign = Paint.Align.CENTER
            val icon = when (pu.type) {
                PowerUpType.SHIELD -> "S"
                PowerUpType.MAGNET -> "M"
                PowerUpType.DOUBLE_SCORE -> "2x"
                PowerUpType.EXTRA_LIFE -> "+"
                PowerUpType.NITRO -> "N"
            }
            canvas.drawText(icon, pu.x, pu.y + size * 0.2f, textPaint)
        }
    }

    private fun drawParticles(canvas: Canvas, world: GameWorld) {
        for (p in world.particles) {
            val a = (p.alpha * 255).toInt().coerceIn(0, 255)
            paint.color = (a shl 24) or (p.color and 0x00FFFFFF)
            canvas.drawCircle(p.x, p.y, p.size, paint)
        }
    }

    private fun drawSpeedLines(canvas: Canvas, world: GameWorld) {
        paint.color = 0x40FFFFFF
        paint.strokeWidth = 2f
        val time = System.currentTimeMillis()
        for (i in 0 until 15) {
            val seed = i * 1337L
            val x = world.roadLeft + (seed % world.roadWidth.toLong()).toFloat()
            val baseY = ((time * 0.8f + seed) % world.screenHeight.toLong()).toFloat()
            val lineLen = 30f + (seed % 40)
            canvas.drawLine(x, baseY, x, baseY + lineLen, paint)
        }
    }

    private fun drawHUD(canvas: Canvas, world: GameWorld) {
        val p = world.player
        val hudHeight = world.screenHeight * 0.08f
        val padding = world.screenWidth * 0.03f

        paint.color = 0xCC000000.toInt()
        canvas.drawRect(0f, 0f, world.screenWidth, hudHeight, paint)

        paint.color = 0x66000000
        canvas.drawRect(0f, hudHeight, world.screenWidth, hudHeight + 2f, paint)

        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = Color.WHITE
        textPaint.textSize = hudHeight * 0.35f
        canvas.drawText("SCORE", padding, hudHeight * 0.35f, textPaint)
        textPaint.textSize = hudHeight * 0.45f
        textPaint.color = 0xFFFFD700.toInt()
        canvas.drawText("${p.score}", padding, hudHeight * 0.78f, textPaint)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.WHITE
        textPaint.textSize = hudHeight * 0.3f
        canvas.drawText("SPEED", world.screenWidth / 2, hudHeight * 0.35f, textPaint)
        textPaint.textSize = hudHeight * 0.4f
        val speedKmh = (p.getEffectiveSpeed() * 12).toInt()
        textPaint.color = if (p.isBoosting) 0xFF42A5F5.toInt() else Color.WHITE
        canvas.drawText("${speedKmh} km/h", world.screenWidth / 2, hudHeight * 0.78f, textPaint)

        val heartSize = hudHeight * 0.28f
        val heartsStartX = world.screenWidth - padding - (heartSize * 2.2f * Constants.MAX_LIVES)
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.color = Color.WHITE
        textPaint.textSize = hudHeight * 0.3f
        canvas.drawText("LIVES", world.screenWidth - padding, hudHeight * 0.35f, textPaint)

        for (i in 0 until Constants.MAX_LIVES) {
            val hx = heartsStartX + i * heartSize * 2.2f + heartSize
            val hy = hudHeight * 0.65f
            paint.color = if (i < p.lives) 0xFFEF5350.toInt() else 0x44FFFFFF
            drawHeart(canvas, hx, hy, heartSize)
        }

        if (p.comboCount > 1) {
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = 0xFF00E676.toInt()
            textPaint.textSize = world.screenHeight * 0.025f
            canvas.drawText(
                "COMBO x${p.comboCount}!",
                world.screenWidth / 2,
                hudHeight + world.screenHeight * 0.04f,
                textPaint
            )
        }

        drawActivePowerUps(canvas, world, hudHeight)

        val pauseBtnSize = hudHeight * 0.5f
        val pauseX = world.screenWidth / 2 + world.screenWidth * 0.35f
        val pauseY = hudHeight * 0.5f
        paint.color = 0x66FFFFFF
        canvas.drawRoundRect(
            RectF(pauseX - pauseBtnSize, pauseY - pauseBtnSize * 0.6f,
                pauseX + pauseBtnSize, pauseY + pauseBtnSize * 0.6f),
            4f, 4f, paint
        )
        paint.color = Color.WHITE
        canvas.drawRect(
            pauseX - pauseBtnSize * 0.3f, pauseY - pauseBtnSize * 0.3f,
            pauseX - pauseBtnSize * 0.1f, pauseY + pauseBtnSize * 0.3f, paint
        )
        canvas.drawRect(
            pauseX + pauseBtnSize * 0.1f, pauseY - pauseBtnSize * 0.3f,
            pauseX + pauseBtnSize * 0.3f, pauseY + pauseBtnSize * 0.3f, paint
        )
    }

    private fun drawActivePowerUps(canvas: Canvas, world: GameWorld, hudHeight: Float) {
        val p = world.player
        val now = System.currentTimeMillis()
        val iconSize = world.screenHeight * 0.02f
        var offsetY = hudHeight + world.screenHeight * 0.06f
        val x = world.screenWidth * 0.06f

        val activePowerUps = mutableListOf<Pair<String, Float>>()
        if (p.hasShield) activePowerUps.add("SHIELD" to ((p.shieldEndTime - now) / 1000f))
        if (p.hasMagnet) activePowerUps.add("MAGNET" to ((p.magnetEndTime - now) / 1000f))
        if (p.hasDoubleScore) activePowerUps.add("2x SCORE" to ((p.doubleScoreEndTime - now) / 1000f))
        if (p.isBoosting) activePowerUps.add("NITRO" to ((p.boostEndTime - now) / 1000f))

        for ((name, timeLeft) in activePowerUps) {
            val barWidth = world.screenWidth * 0.2f
            val barHeight = iconSize * 0.8f

            paint.color = 0x66000000
            canvas.drawRoundRect(
                RectF(x - 2f, offsetY - barHeight / 2 - 2f, x + barWidth + 2f, offsetY + barHeight / 2 + 2f),
                4f, 4f, paint
            )

            val maxTime = when (name) {
                "SHIELD" -> Constants.SHIELD_DURATION_MS / 1000f
                "MAGNET" -> Constants.MAGNET_DURATION_MS / 1000f
                "2x SCORE" -> Constants.DOUBLE_SCORE_DURATION_MS / 1000f
                "NITRO" -> Constants.BOOST_DURATION_MS / 1000f
                else -> 5f
            }
            val progress = (timeLeft / maxTime).coerceIn(0f, 1f)

            paint.color = when (name) {
                "SHIELD" -> 0xFF00E5FF.toInt()
                "MAGNET" -> 0xFFFF6F00.toInt()
                "2x SCORE" -> 0xFFFFD700.toInt()
                "NITRO" -> 0xFF2979FF.toInt()
                else -> Color.WHITE
            }
            canvas.drawRoundRect(
                RectF(x, offsetY - barHeight / 2, x + barWidth * progress, offsetY + barHeight / 2),
                3f, 3f, paint
            )

            textPaint.color = Color.WHITE
            textPaint.textSize = iconSize * 0.9f
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(name, x + 4f, offsetY + iconSize * 0.3f, textPaint)

            offsetY += barHeight + iconSize * 0.5f
        }
    }

    private fun drawHeart(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        path.reset()
        path.moveTo(cx, cy + size * 0.6f)
        path.cubicTo(cx - size * 1.2f, cy - size * 0.2f, cx - size * 0.6f, cy - size * 0.9f, cx, cy - size * 0.3f)
        path.cubicTo(cx + size * 0.6f, cy - size * 0.9f, cx + size * 1.2f, cy - size * 0.2f, cx, cy + size * 0.6f)
        canvas.drawPath(path, paint)
    }

    fun drawStartScreen(canvas: Canvas, world: GameWorld) {
        paint.color = 0xFF1A237E.toInt()
        canvas.drawRect(0f, 0f, world.screenWidth, world.screenHeight, paint)

        val time = System.currentTimeMillis()
        for (i in 0 until 20) {
            val seed = i * 997L
            val x = ((seed * 7) % world.screenWidth.toLong()).toFloat()
            val baseY = ((time * 0.3f + seed * 3) % (world.screenHeight * 1.5f)) - world.screenHeight * 0.25f
            paint.color = 0x15FFFFFF
            paint.strokeWidth = 3f
            canvas.drawLine(x, baseY, x, baseY + 40f + (seed % 30), paint)
        }

        val titleY = world.screenHeight * 0.2f

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = world.screenWidth * 0.13f
        textPaint.color = 0xFF42A5F5.toInt()
        canvas.drawText("TRAFFIC", world.screenWidth / 2, titleY, textPaint)

        textPaint.textSize = world.screenWidth * 0.16f
        textPaint.color = 0xFFFF5722.toInt()
        canvas.drawText("RACER", world.screenWidth / 2, titleY + world.screenWidth * 0.15f, textPaint)

        val carY = world.screenHeight * 0.45f
        val bobOffset = sin(time * 0.003) * 8f
        drawCarBody(
            canvas,
            world.screenWidth / 2, carY + bobOffset.toFloat(),
            world.screenWidth * 0.2f, world.screenHeight * 0.12f,
            0xFFE53935.toInt(), true
        )

        val btnY = world.screenHeight * 0.65f
        val btnWidth = world.screenWidth * 0.55f
        val btnHeight = world.screenHeight * 0.07f
        val pulse = 1f + sin(time * 0.004) * 0.03f

        paint.color = 0xFF4CAF50.toInt()
        canvas.drawRoundRect(
            RectF(
                world.screenWidth / 2 - btnWidth / 2 * pulse, btnY - btnHeight / 2 * pulse,
                world.screenWidth / 2 + btnWidth / 2 * pulse, btnY + btnHeight / 2 * pulse
            ),
            btnHeight / 2, btnHeight / 2, paint
        )
        textPaint.color = Color.WHITE
        textPaint.textSize = btnHeight * 0.45f
        canvas.drawText("TAP TO PLAY", world.screenWidth / 2, btnY + btnHeight * 0.15f, textPaint)

        if (world.highScore > 0) {
            textPaint.color = 0xFFFFD700.toInt()
            textPaint.textSize = world.screenWidth * 0.045f
            canvas.drawText(
                "HIGH SCORE: ${world.highScore}",
                world.screenWidth / 2,
                world.screenHeight * 0.78f,
                textPaint
            )
        }

        textPaint.color = 0xAAFFFFFF.toInt()
        textPaint.textSize = world.screenWidth * 0.035f
        canvas.drawText("Swipe left/right to change lanes", world.screenWidth / 2, world.screenHeight * 0.86f, textPaint)
        canvas.drawText("Swipe up for nitro boost", world.screenWidth / 2, world.screenHeight * 0.90f, textPaint)
        canvas.drawText("Dodge traffic & collect coins!", world.screenWidth / 2, world.screenHeight * 0.94f, textPaint)
    }

    fun drawGameOverScreen(canvas: Canvas, world: GameWorld) {
        paint.color = 0xDD000000.toInt()
        canvas.drawRect(0f, 0f, world.screenWidth, world.screenHeight, paint)

        val centerY = world.screenHeight * 0.3f

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = 0xFFEF5350.toInt()
        textPaint.textSize = world.screenWidth * 0.12f
        canvas.drawText("GAME OVER", world.screenWidth / 2, centerY, textPaint)

        val isNewHighScore = world.player.score >= world.highScore && world.player.score > 0
        if (isNewHighScore) {
            val time = System.currentTimeMillis()
            val flash = ((sin(time * 0.005) + 1f) / 2f * 255).toInt()
            textPaint.color = (0xFF shl 24) or (flash shl 16) or (0xD7 shl 8) or 0x00
            textPaint.textSize = world.screenWidth * 0.06f
            canvas.drawText("NEW HIGH SCORE!", world.screenWidth / 2, centerY + world.screenHeight * 0.06f, textPaint)
        }

        val statsY = centerY + world.screenHeight * 0.12f
        val statSpacing = world.screenHeight * 0.055f

        textPaint.color = 0xCCFFFFFF.toInt()
        textPaint.textSize = world.screenWidth * 0.04f

        textPaint.textAlign = Paint.Align.LEFT
        val labelX = world.screenWidth * 0.2f
        textPaint.textAlign = Paint.Align.RIGHT
        val valueX = world.screenWidth * 0.8f

        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = 0xAAFFFFFF.toInt()
        canvas.drawText("DISTANCE", labelX, statsY, textPaint)
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.color = Color.WHITE
        canvas.drawText("${world.player.distanceScore.toLong()}", valueX, statsY, textPaint)

        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = 0xAAFFFFFF.toInt()
        canvas.drawText("COINS", labelX, statsY + statSpacing, textPaint)
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.color = 0xFFFFD700.toInt()
        canvas.drawText("${world.player.coinScore}", valueX, statsY + statSpacing, textPaint)

        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = 0xAAFFFFFF.toInt()
        canvas.drawText("TOTAL SCORE", labelX, statsY + statSpacing * 2, textPaint)
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.color = 0xFF4CAF50.toInt()
        textPaint.textSize = world.screenWidth * 0.055f
        canvas.drawText("${world.player.score}", valueX, statsY + statSpacing * 2, textPaint)

        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = 0xAAFFFFFF.toInt()
        textPaint.textSize = world.screenWidth * 0.04f
        canvas.drawText("BEST", labelX, statsY + statSpacing * 3, textPaint)
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.color = 0xFFFFD700.toInt()
        canvas.drawText("${world.highScore}", valueX, statsY + statSpacing * 3, textPaint)

        val btnY = world.screenHeight * 0.72f
        val btnWidth = world.screenWidth * 0.55f
        val btnHeight = world.screenHeight * 0.07f

        paint.color = 0xFF4CAF50.toInt()
        canvas.drawRoundRect(
            RectF(
                world.screenWidth / 2 - btnWidth / 2, btnY - btnHeight / 2,
                world.screenWidth / 2 + btnWidth / 2, btnY + btnHeight / 2
            ),
            btnHeight / 2, btnHeight / 2, paint
        )
        textPaint.color = Color.WHITE
        textPaint.textSize = btnHeight * 0.45f
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("PLAY AGAIN", world.screenWidth / 2, btnY + btnHeight * 0.15f, textPaint)

        textPaint.color = 0x88FFFFFF.toInt()
        textPaint.textSize = world.screenWidth * 0.035f
        canvas.drawText("Tap anywhere to restart", world.screenWidth / 2, world.screenHeight * 0.85f, textPaint)
    }

    private fun drawPauseOverlay(canvas: Canvas, world: GameWorld) {
        paint.color = 0xAA000000.toInt()
        canvas.drawRect(0f, 0f, world.screenWidth, world.screenHeight, paint)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.WHITE
        textPaint.textSize = world.screenWidth * 0.1f
        canvas.drawText("PAUSED", world.screenWidth / 2, world.screenHeight * 0.4f, textPaint)

        val btnY = world.screenHeight * 0.55f
        val btnWidth = world.screenWidth * 0.5f
        val btnHeight = world.screenHeight * 0.065f

        paint.color = 0xFF4CAF50.toInt()
        canvas.drawRoundRect(
            RectF(
                world.screenWidth / 2 - btnWidth / 2, btnY - btnHeight / 2,
                world.screenWidth / 2 + btnWidth / 2, btnY + btnHeight / 2
            ),
            btnHeight / 2, btnHeight / 2, paint
        )
        textPaint.color = Color.WHITE
        textPaint.textSize = btnHeight * 0.45f
        canvas.drawText("RESUME", world.screenWidth / 2, btnY + btnHeight * 0.15f, textPaint)
    }

    private fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
        val inv = 1f - ratio
        val r = ((Color.red(color1) * inv + Color.red(color2) * ratio)).toInt().coerceIn(0, 255)
        val g = ((Color.green(color1) * inv + Color.green(color2) * ratio)).toInt().coerceIn(0, 255)
        val b = ((Color.blue(color1) * inv + Color.blue(color2) * ratio)).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val r = (Color.red(color) * (1f - factor)).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * (1f - factor)).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * (1f - factor)).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private fun lightenColor(color: Int, factor: Float): Int {
        val r = min(255, Color.red(color) + ((255 - Color.red(color)) * factor).toInt())
        val g = min(255, Color.green(color) + ((255 - Color.green(color)) * factor).toInt())
        val b = min(255, Color.blue(color) + ((255 - Color.blue(color)) * factor).toInt())
        return Color.rgb(r, g, b)
    }
}
