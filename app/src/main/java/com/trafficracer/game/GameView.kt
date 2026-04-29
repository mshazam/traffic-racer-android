package com.trafficracer.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.abs
import kotlin.math.sqrt

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    private val gameWorld = GameWorld(context)
    private val renderer = GameRenderer()
    private var gameThread: GameThread? = null

    // Multi-touch tracking
    private val pointerActions = mutableMapOf<Int, PointerAction>()

    private enum class PointerAction { GAS, BRAKE, STEER, NOS, NONE }

    init {
        holder.addCallback(this)
        holder.setFormat(PixelFormat.RGBA_8888)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        gameWorld.init(width.toFloat(), height.toFloat())
        gameThread = GameThread(holder, this).also { it.start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        gameWorld.init(width.toFloat(), height.toFloat())
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        gameThread?.running = false
        var retry = true
        while (retry) {
            try { gameThread?.join(); retry = false } catch (_: InterruptedException) {}
        }
        gameThread = null
    }

    fun update(deltaTime: Float) { gameWorld.update(deltaTime) }
    fun render(canvas: Canvas) { renderer.render(canvas, gameWorld) }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        try {
            when (gameWorld.state) {
                GameState.PLAYING, GameState.PAUSED -> handleGameplayTouch(event)
                GameState.START_SCREEN -> handleStartScreenTouch(event)
                GameState.GARAGE -> handleGarageTouch(event)
                GameState.MISSIONS_SCREEN -> handleMissionsTouch(event)
                GameState.GAME_OVER -> handleGameOverTouch(event)
            }
        } catch (_: Exception) {}
        return true
    }

    // ---- NFS multi-touch controls ----
    private fun handleGameplayTouch(event: MotionEvent) {
        if (gameWorld.state == GameState.PAUSED) {
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_POINTER_UP) {
                val idx = if (event.actionMasked == MotionEvent.ACTION_POINTER_UP) event.actionIndex else 0
                val tx = event.getX(idx); val ty = event.getY(idx)
                val btnY = gameWorld.screenHeight * 0.55f; val btnW = gameWorld.screenWidth * 0.5f; val btnH = gameWorld.screenHeight * 0.065f
                if (ty > btnY - btnH && ty < btnY + btnH && tx > gameWorld.screenWidth / 2 - btnW / 2 && tx < gameWorld.screenWidth / 2 + btnW / 2) gameWorld.resume()
            }
            return
        }

        val areas = renderer.getControlAreas(gameWorld)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = if (event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) event.actionIndex else 0
                val pid = event.getPointerId(idx); val tx = event.getX(idx); val ty = event.getY(idx)
                val action = classifyTouch(tx, ty, areas)
                pointerActions[pid] = action
                applyAction(action, tx, ty, areas, true)
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pid = event.getPointerId(i); val tx = event.getX(i); val ty = event.getY(i)
                    val action = pointerActions[pid] ?: continue
                    if (action == PointerAction.STEER) updateSteering(tx, areas)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val idx = if (event.actionMasked == MotionEvent.ACTION_POINTER_UP) event.actionIndex else 0
                val pid = event.getPointerId(idx)
                val action = pointerActions.remove(pid) ?: PointerAction.NONE
                applyAction(action, 0f, 0f, areas, false)
                if (event.actionMasked == MotionEvent.ACTION_CANCEL) {
                    pointerActions.clear(); resetAllControls()
                }
            }
        }
    }

    private fun classifyTouch(x: Float, y: Float, areas: ControlAreas): PointerAction {
        // Check pause button first
        if (areas.pauseArea.contains(x, y)) {
            gameWorld.pause(); return PointerAction.NONE
        }
        // NOS button
        val nosDist = sqrt((x - areas.nosCx) * (x - areas.nosCx) + (y - areas.nosCy) * (y - areas.nosCy))
        if (nosDist < areas.nosRadius) return PointerAction.NOS
        // Brake pedal
        if (areas.brake.contains(x, y)) return PointerAction.BRAKE
        // Gas pedal
        if (areas.gas.contains(x, y)) return PointerAction.GAS
        // Steering zone
        val controlsTop = gameWorld.screenHeight * (1f - Constants.HUD_CONTROLS_HEIGHT_RATIO)
        if (y > controlsTop && x > areas.steerLeft && x < areas.steerRight) return PointerAction.STEER
        return PointerAction.NONE
    }

    private fun applyAction(action: PointerAction, x: Float, y: Float, areas: ControlAreas, isDown: Boolean) {
        when (action) {
            PointerAction.GAS -> gameWorld.setAccelerating(isDown)
            PointerAction.BRAKE -> gameWorld.setBraking(isDown)
            PointerAction.NOS -> if (isDown) gameWorld.activateNOS() else gameWorld.deactivateNOS()
            PointerAction.STEER -> {
                if (isDown) updateSteering(x, areas) else gameWorld.setSteering(0f)
            }
            PointerAction.NONE -> {}
        }
    }

    private fun updateSteering(x: Float, areas: ControlAreas) {
        val centerX = (areas.steerLeft + areas.steerRight) / 2f
        val halfWidth = (areas.steerRight - areas.steerLeft) / 2f
        val input = ((x - centerX) / halfWidth).coerceIn(-1f, 1f)
        gameWorld.setSteering(input)
    }

    private fun resetAllControls() {
        gameWorld.setSteering(0f); gameWorld.setAccelerating(false); gameWorld.setBraking(false); gameWorld.deactivateNOS()
    }

    // ---- Screen touch handlers ----
    private fun handleStartScreenTouch(event: MotionEvent) {
        if (event.actionMasked != MotionEvent.ACTION_UP) return
        val tx = event.x; val ty = event.y
        val btnW = gameWorld.screenWidth * 0.55f; val btnH = gameWorld.screenHeight * 0.065f
        val cx = gameWorld.screenWidth / 2

        val playY = gameWorld.screenHeight * 0.55f
        if (abs(tx - cx) < btnW / 2 && abs(ty - playY) < btnH) { gameWorld.startGame(); return }
        val garageY = gameWorld.screenHeight * 0.64f
        if (abs(tx - cx) < btnW / 2 && abs(ty - garageY) < btnH) { gameWorld.state = GameState.GARAGE; return }
        val missY = gameWorld.screenHeight * 0.73f
        if (abs(tx - cx) < btnW / 2 && abs(ty - missY) < btnH) { gameWorld.state = GameState.MISSIONS_SCREEN; return }
    }

    private fun handleGarageTouch(event: MotionEvent) {
        if (event.actionMasked != MotionEvent.ACTION_UP) return
        val tx = event.x; val ty = event.y
        val sw = gameWorld.screenWidth; val sh = gameWorld.screenHeight
        val btnW = sw * 0.5f; val btnH = sh * 0.06f

        // Nav arrows
        val arrowY = sh * 0.28f
        if (ty > arrowY - sh * 0.1f && ty < arrowY + sh * 0.1f) {
            if (tx < sw * 0.2f && gameWorld.garageSelectedIndex > 0) { gameWorld.selectCar(gameWorld.garageSelectedIndex - 1); return }
            if (tx > sw * 0.8f && gameWorld.garageSelectedIndex < PlayerCarDef.ALL_CARS.size - 1) { gameWorld.selectCar(gameWorld.garageSelectedIndex + 1); return }
        }

        // Action button
        val actionY = sh * 0.68f
        if (abs(tx - sw / 2) < btnW / 2 && abs(ty - actionY) < btnH) {
            val car = PlayerCarDef.ALL_CARS[gameWorld.garageSelectedIndex]
            if (!gameWorld.gameData.isCarUnlocked(car.id)) gameWorld.buyCar()
            else gameWorld.equipCar()
            return
        }

        // Back
        val backY = sh * 0.77f
        if (abs(tx - sw / 2) < btnW / 2 && abs(ty - backY) < btnH) { gameWorld.state = GameState.START_SCREEN; return }
    }

    private fun handleMissionsTouch(event: MotionEvent) {
        if (event.actionMasked != MotionEvent.ACTION_UP) return
        val tx = event.x; val ty = event.y
        val backY = gameWorld.screenHeight * 0.85f; val btnW = gameWorld.screenWidth * 0.5f; val btnH = gameWorld.screenHeight * 0.06f
        if (abs(tx - gameWorld.screenWidth / 2) < btnW / 2 && abs(ty - backY) < btnH) { gameWorld.state = GameState.START_SCREEN }
    }

    private fun handleGameOverTouch(event: MotionEvent) {
        if (event.actionMasked != MotionEvent.ACTION_UP) return
        gameWorld.state = GameState.START_SCREEN
    }

    fun onPause() { if (gameWorld.state == GameState.PLAYING) gameWorld.pause() }
    fun onDestroy() { gameWorld.release() }

    inner class GameThread(
        private val surfaceHolder: SurfaceHolder,
        private val gameView: GameView
    ) : Thread("GameThread") {
        var running = true
        private var lastFrameTime = System.nanoTime()

        override fun run() {
            while (running) {
                val now = System.nanoTime()
                val deltaTime = ((now - lastFrameTime) / 1_000_000_000.0).toFloat().coerceIn(0f, 0.05f)
                lastFrameTime = now
                gameView.update(deltaTime)
                var canvas: Canvas? = null
                try {
                    canvas = surfaceHolder.lockCanvas()
                    if (canvas != null) synchronized(surfaceHolder) { gameView.render(canvas) }
                } catch (_: Exception) {
                } finally {
                    canvas?.let { try { surfaceHolder.unlockCanvasAndPost(it) } catch (_: Exception) {} }
                }
                val frameTime = (System.nanoTime() - now) / 1_000_000
                val sleepTime = Constants.FRAME_PERIOD - frameTime
                if (sleepTime > 0) try { sleep(sleepTime) } catch (_: InterruptedException) {}
            }
        }
    }
}
