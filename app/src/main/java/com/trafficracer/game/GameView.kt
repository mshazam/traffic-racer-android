package com.trafficracer.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.abs

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    private val gameWorld = GameWorld(context)
    private val renderer = GameRenderer()
    private var gameThread: GameThread? = null

    private var touchStartX = 0f
    private var touchStartY = 0f
    private var touchStartTime = 0L
    private val SWIPE_THRESHOLD = 50f
    private val TAP_THRESHOLD = 15f
    private val TAP_TIME_THRESHOLD = 250L

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
            try {
                gameThread?.join()
                retry = false
            } catch (e: InterruptedException) {
                // retry
            }
        }
        gameThread = null
    }

    fun update(deltaTime: Float) {
        gameWorld.update(deltaTime)
    }

    fun render(canvas: Canvas) {
        renderer.render(canvas, gameWorld)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y
                touchStartTime = System.currentTimeMillis()
                return true
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.x - touchStartX
                val dy = event.y - touchStartY
                val dt = System.currentTimeMillis() - touchStartTime
                val absDx = abs(dx)
                val absDy = abs(dy)

                when (gameWorld.state) {
                    GameState.START_SCREEN -> {
                        gameWorld.startGame()
                    }
                    GameState.GAME_OVER -> {
                        gameWorld.startGame()
                    }
                    GameState.PAUSED -> {
                        gameWorld.resume()
                    }
                    GameState.PLAYING -> {
                        val hudHeight = gameWorld.screenHeight * 0.08f
                        if (touchStartY < hudHeight && event.y < hudHeight) {
                            val pauseX = gameWorld.screenWidth / 2 + gameWorld.screenWidth * 0.35f
                            if (abs(touchStartX - pauseX) < hudHeight) {
                                gameWorld.pause()
                                return true
                            }
                        }

                        if (absDx > SWIPE_THRESHOLD && absDx > absDy) {
                            if (dx > 0) gameWorld.moveRight() else gameWorld.moveLeft()
                        } else if (absDy > SWIPE_THRESHOLD && absDy > absDx) {
                            if (dy < 0) {
                                gameWorld.activateBoost()
                            }
                        } else if (absDx < TAP_THRESHOLD && absDy < TAP_THRESHOLD && dt < TAP_TIME_THRESHOLD) {
                            if (event.x < gameWorld.screenWidth / 2) {
                                gameWorld.moveLeft()
                            } else {
                                gameWorld.moveRight()
                            }
                        }
                    }
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun onPause() {
        if (gameWorld.state == GameState.PLAYING) {
            gameWorld.pause()
        }
    }

    fun onDestroy() {
        gameWorld.release()
    }

    inner class GameThread(
        private val surfaceHolder: SurfaceHolder,
        private val gameView: GameView
    ) : Thread("GameThread") {

        var running = true
        private var lastFrameTime = System.nanoTime()

        override fun run() {
            while (running) {
                val now = System.nanoTime()
                val deltaTime = ((now - lastFrameTime) / 1_000_000_000.0).toFloat()
                    .coerceIn(0f, 0.05f)
                lastFrameTime = now

                gameView.update(deltaTime)

                var canvas: Canvas? = null
                try {
                    canvas = surfaceHolder.lockCanvas()
                    if (canvas != null) {
                        synchronized(surfaceHolder) {
                            gameView.render(canvas)
                        }
                    }
                } catch (e: Exception) {
                    // Surface might be destroyed
                } finally {
                    canvas?.let {
                        try {
                            surfaceHolder.unlockCanvasAndPost(it)
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                }

                val frameTime = (System.nanoTime() - now) / 1_000_000
                val sleepTime = Constants.FRAME_PERIOD - frameTime
                if (sleepTime > 0) {
                    try {
                        sleep(sleepTime)
                    } catch (e: InterruptedException) {
                        // interrupted
                    }
                }
            }
        }
    }
}
