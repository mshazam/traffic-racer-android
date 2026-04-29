package com.trafficracer.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

class MenuScreen(private val game: TrafficRacerGame) : ScreenAdapter() {

    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var layout: GlyphLayout

    // Button positions (calculated on render)
    private var playBtnY = 0f
    private var settingsBtnY = 0f
    private var garageBtnY = 0f
    private var btnW = 0f
    private var btnH = 0f

    override fun show() {
        shapeRenderer = ShapeRenderer()
        layout = GlyphLayout()
        Gdx.input.inputProcessor = null
    }

    override fun render(delta: Float) {
        val sw = Gdx.graphics.width.toFloat()
        val sh = Gdx.graphics.height.toFloat()

        Gdx.gl.glClearColor(0.05f, 0.1f, 0.18f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        btnW = sw * 0.6f
        btnH = sh * 0.07f
        val cx = sw / 2f
        playBtnY = sh * 0.55f
        garageBtnY = sh * 0.44f
        settingsBtnY = sh * 0.33f

        // Draw buttons
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Background gradient effect
        shapeRenderer.setColor(0.08f, 0.12f, 0.22f, 1f)
        shapeRenderer.rect(0f, 0f, sw, sh)

        // Play button
        drawButton(cx, playBtnY, btnW, btnH, Color(0.2f, 0.6f, 0.3f, 1f))

        // Garage button
        drawButton(cx, garageBtnY, btnW, btnH, Color(0.2f, 0.4f, 0.7f, 1f))

        // Settings button
        drawButton(cx, settingsBtnY, btnW, btnH, Color(0.4f, 0.3f, 0.6f, 1f))

        shapeRenderer.end()

        // Text
        game.batch.begin()

        // Title
        game.fontLarge.color = Color(0.3f, 0.7f, 1f, 1f)
        layout.setText(game.fontLarge, "TRAFFIC RACER")
        game.fontLarge.draw(game.batch, layout, (sw - layout.width) / 2f, sh * 0.85f)

        game.font.color = Color(0.5f, 0.7f, 0.9f, 1f)
        layout.setText(game.font, "3D EDITION")
        game.font.draw(game.batch, layout, (sw - layout.width) / 2f, sh * 0.77f)

        // High score
        game.font.color = Color.GOLD
        layout.setText(game.font, "High Score: ${game.prefs.highScore}")
        game.font.draw(game.batch, layout, (sw - layout.width) / 2f, sh * 0.68f)

        game.font.color = Color.WHITE
        layout.setText(game.font, "Coins: ${game.prefs.totalCoins}")
        game.font.draw(game.batch, layout, (sw - layout.width) / 2f, sh * 0.63f)

        // Button labels
        game.font.color = Color.WHITE
        drawButtonText("PLAY", cx, playBtnY, sw)
        drawButtonText("GARAGE", cx, garageBtnY, sw)
        drawButtonText("SETTINGS", cx, settingsBtnY, sw)

        game.batch.end()

        // Touch handling
        if (Gdx.input.justTouched()) {
            val tx = Gdx.input.x.toFloat()
            val ty = (sh - Gdx.input.y.toFloat()) // flip Y

            if (isInButton(tx, ty, cx, playBtnY)) {
                game.setScreen(GameScreen(game))
            } else if (isInButton(tx, ty, cx, garageBtnY)) {
                game.setScreen(GarageScreen(game))
            } else if (isInButton(tx, ty, cx, settingsBtnY)) {
                game.setScreen(SettingsScreen(game))
            }
        }
    }

    private fun drawButton(cx: Float, cy: Float, w: Float, h: Float, color: Color) {
        shapeRenderer.setColor(color)
        shapeRenderer.rect(cx - w / 2f, cy - h / 2f, w, h)
        // Border
        shapeRenderer.setColor(color.r + 0.2f, color.g + 0.2f, color.b + 0.2f, 1f)
        shapeRenderer.rectLine(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy - h / 2f, 2f)
        shapeRenderer.rectLine(cx - w / 2f, cy + h / 2f, cx + w / 2f, cy + h / 2f, 2f)
        shapeRenderer.rectLine(cx - w / 2f, cy - h / 2f, cx - w / 2f, cy + h / 2f, 2f)
        shapeRenderer.rectLine(cx + w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f, 2f)
    }

    private fun drawButtonText(text: String, cx: Float, cy: Float, sw: Float) {
        layout.setText(game.font, text)
        game.font.draw(game.batch, layout, cx - layout.width / 2f, cy + layout.height / 2f)
    }

    private fun isInButton(tx: Float, ty: Float, cx: Float, cy: Float): Boolean {
        return tx > cx - btnW / 2f && tx < cx + btnW / 2f &&
                ty > cy - btnH / 2f && ty < cy + btnH / 2f
    }

    override fun dispose() {
        shapeRenderer.dispose()
    }
}
