package com.trafficracer.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

class SettingsScreen(private val game: TrafficRacerGame) : ScreenAdapter() {

    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var layout: GlyphLayout

    private val schemes = arrayOf("NFS Style", "Simple (Tilt)", "Arcade (Buttons)")
    private val schemeDescs = arrayOf(
        "Steering wheel + Gas/Brake + NOS",
        "Tilt to steer + Auto gas + Tap brake",
        "Left/Right buttons + Auto gas + Tap brake"
    )

    override fun show() {
        shapeRenderer = ShapeRenderer()
        layout = GlyphLayout()
    }

    override fun render(delta: Float) {
        val sw = Gdx.graphics.width.toFloat()
        val sh = Gdx.graphics.height.toFloat()

        Gdx.gl.glClearColor(0.05f, 0.08f, 0.15f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        val cardW = sw * 0.88f
        val cardH = sh * 0.08f
        val startY = sh * 0.75f
        val cx = sw / 2f

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Control scheme cards
        for (i in schemes.indices) {
            val cy = startY - i * (cardH + sh * 0.015f)
            val selected = game.prefs.controlScheme == i
            val color = if (selected) Color(0.15f, 0.3f, 0.6f, 1f) else Color(0.15f, 0.15f, 0.2f, 1f)
            shapeRenderer.setColor(color)
            shapeRenderer.rect(cx - cardW / 2f, cy - cardH / 2f, cardW, cardH)
            if (selected) {
                shapeRenderer.setColor(0.2f, 0.5f, 1f, 1f)
                shapeRenderer.rectLine(cx - cardW / 2f, cy - cardH / 2f, cx + cardW / 2f, cy - cardH / 2f, 2f)
                shapeRenderer.rectLine(cx - cardW / 2f, cy + cardH / 2f, cx + cardW / 2f, cy + cardH / 2f, 2f)
                shapeRenderer.rectLine(cx - cardW / 2f, cy - cardH / 2f, cx - cardW / 2f, cy + cardH / 2f, 2f)
                shapeRenderer.rectLine(cx + cardW / 2f, cy - cardH / 2f, cx + cardW / 2f, cy + cardH / 2f, 2f)
            }
        }

        // Perspective toggle (below control cards)
        val toggleY = startY - schemes.size * (cardH + sh * 0.015f) - sh * 0.04f
        val toggleW = cardW
        val toggleH = sh * 0.05f

        // Orientation toggle
        val orientY = toggleY - toggleH - sh * 0.02f
        val isPortrait = game.prefs.orientationPortrait
        val orientColor = if (isPortrait) Color(0.15f, 0.3f, 0.6f, 1f) else Color(0.15f, 0.15f, 0.2f, 1f)
        shapeRenderer.setColor(orientColor)
        shapeRenderer.rect(cx - toggleW / 2f, orientY - toggleH / 2f, toggleW, toggleH)

        // Back button
        val backY = sh * 0.08f
        val backW = sw * 0.4f
        val backH = sh * 0.055f
        shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 0.8f)
        shapeRenderer.rect(cx - backW / 2f, backY - backH / 2f, backW, backH)

        shapeRenderer.end()

        // Text
        game.batch.begin()

        // Title
        game.fontLarge.color = Color(0.3f, 0.7f, 1f, 1f)
        layout.setText(game.fontLarge, "SETTINGS")
        game.fontLarge.draw(game.batch, layout, (sw - layout.width) / 2f, sh * 0.95f)

        // Section: Controls
        game.font.color = Color(1f, 0.5f, 0f, 1f)
        layout.setText(game.font, "CONTROL SCHEME")
        game.font.draw(game.batch, layout, (sw - layout.width) / 2f, sh * 0.88f)

        // Scheme cards
        for (i in schemes.indices) {
            val cy = startY - i * (cardH + sh * 0.015f)
            val selected = game.prefs.controlScheme == i
            game.font.color = if (selected) Color.WHITE else Color(0.7f, 0.7f, 0.7f, 1f)
            game.font.draw(game.batch, schemes[i], cx - cardW / 2f + sw * 0.08f, cy + cardH * 0.15f)
            game.font.color = Color(0.5f, 0.5f, 0.6f, 1f)
            game.font.data.setScale(1.2f)
            game.font.draw(game.batch, schemeDescs[i], cx - cardW / 2f + sw * 0.08f, cy - cardH * 0.15f)
            game.font.data.setScale(1.5f)

            // Radio indicator
            if (selected) {
                game.font.color = Color(0.2f, 0.6f, 1f, 1f)
                game.font.draw(game.batch, "●", cx - cardW / 2f + sw * 0.03f, cy + game.font.capHeight / 2f)
            } else {
                game.font.color = Color(0.5f, 0.5f, 0.5f, 1f)
                game.font.draw(game.batch, "○", cx - cardW / 2f + sw * 0.03f, cy + game.font.capHeight / 2f)
            }
        }

        // Section: Display
        game.font.color = Color(1f, 0.5f, 0f, 1f)
        val displayLabelY = toggleY + toggleH / 2f + sh * 0.04f
        layout.setText(game.font, "DISPLAY")
        game.font.draw(game.batch, layout, (sw - layout.width) / 2f, displayLabelY)

        // Orientation
        game.font.color = Color.WHITE
        val orientLabel = if (isPortrait) "Orientation: Portrait" else "Orientation: Landscape"
        game.font.draw(game.batch, orientLabel, cx - toggleW / 2f + sw * 0.04f, orientY + game.font.capHeight / 2f)

        // Toggle indicator
        game.font.color = if (isPortrait) Color(0.2f, 0.6f, 1f, 1f) else Color.GRAY
        game.font.draw(game.batch, if (isPortrait) "[ON]" else "[OFF]", cx + toggleW / 2f - sw * 0.12f, orientY + game.font.capHeight / 2f)

        // Back
        game.font.color = Color.WHITE
        layout.setText(game.font, "BACK")
        game.font.draw(game.batch, layout, cx - layout.width / 2f, backY + layout.height / 2f)

        game.batch.end()

        // Touch
        if (Gdx.input.justTouched()) {
            val tx = Gdx.input.x.toFloat()
            val ty = sh - Gdx.input.y.toFloat()

            // Control scheme cards
            for (i in schemes.indices) {
                val cy = startY - i * (cardH + sh * 0.015f)
                if (tx > cx - cardW / 2f && tx < cx + cardW / 2f &&
                    ty > cy - cardH / 2f && ty < cy + cardH / 2f) {
                    game.prefs.controlScheme = i
                    return
                }
            }

            // Orientation toggle
            if (tx > cx - toggleW / 2f && tx < cx + toggleW / 2f &&
                ty > orientY - toggleH / 2f && ty < orientY + toggleH / 2f) {
                val newVal = !game.prefs.orientationPortrait
                game.prefs.orientationPortrait = newVal
                game.onOrientationChanged(newVal)
                return
            }

            // Back
            if (tx > cx - backW / 2f && tx < cx + backW / 2f &&
                ty > backY - backH / 2f && ty < backY + backH / 2f) {
                game.setScreen(MenuScreen(game))
            }
        }
    }

    override fun dispose() {
        shapeRenderer.dispose()
    }
}
