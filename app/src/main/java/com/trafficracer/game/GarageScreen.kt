package com.trafficracer.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

class GarageScreen(private val game: TrafficRacerGame) : ScreenAdapter() {

    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var layout: GlyphLayout
    private var selectedIndex = 0

    override fun show() {
        shapeRenderer = ShapeRenderer()
        layout = GlyphLayout()
        // Find current car index
        selectedIndex = PlayerCarDef.ALL.indexOfFirst { it.id == game.prefs.selectedCar }.coerceAtLeast(0)
    }

    override fun render(delta: Float) {
        val sw = Gdx.graphics.width.toFloat()
        val sh = Gdx.graphics.height.toFloat()
        val cx = sw / 2f
        val car = PlayerCarDef.ALL[selectedIndex]
        val unlocked = game.prefs.isCarUnlocked(car.id)
        val equipped = game.prefs.selectedCar == car.id

        Gdx.gl.glClearColor(0.05f, 0.08f, 0.15f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Car display area
        shapeRenderer.setColor(0.1f, 0.12f, 0.2f, 1f)
        shapeRenderer.rect(sw * 0.1f, sh * 0.55f, sw * 0.8f, sh * 0.25f)

        // Stats bars
        val barX = sw * 0.15f
        val barW = sw * 0.7f
        val barH = sh * 0.02f

        // Speed bar
        val speedY = sh * 0.45f
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1f)
        shapeRenderer.rect(barX, speedY, barW, barH)
        shapeRenderer.setColor(Color(0f, 0.8f, 0.4f, 1f))
        shapeRenderer.rect(barX, speedY, barW * (car.baseSpeed / 1.5f), barH)

        // Handling bar
        val handlingY = sh * 0.40f
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1f)
        shapeRenderer.rect(barX, handlingY, barW, barH)
        shapeRenderer.setColor(Color(0.3f, 0.6f, 1f, 1f))
        shapeRenderer.rect(barX, handlingY, barW * (car.handling / 1.5f), barH)

        // Arrow buttons
        if (selectedIndex > 0) {
            shapeRenderer.setColor(0.3f, 0.3f, 0.4f, 0.8f)
            shapeRenderer.rect(sw * 0.02f, sh * 0.6f, sw * 0.08f, sh * 0.1f)
        }
        if (selectedIndex < PlayerCarDef.ALL.size - 1) {
            shapeRenderer.setColor(0.3f, 0.3f, 0.4f, 0.8f)
            shapeRenderer.rect(sw * 0.9f, sh * 0.6f, sw * 0.08f, sh * 0.1f)
        }

        // Action button
        val actionY = sh * 0.25f
        val actionW = sw * 0.5f
        val actionH = sh * 0.06f
        val actionColor = when {
            equipped -> Color(0.3f, 0.3f, 0.3f, 1f)
            unlocked -> Color(0.2f, 0.6f, 0.3f, 1f)
            game.prefs.totalCoins >= car.price -> Color(1f, 0.7f, 0f, 1f)
            else -> Color(0.5f, 0.3f, 0.3f, 1f)
        }
        shapeRenderer.setColor(actionColor)
        shapeRenderer.rect(cx - actionW / 2f, actionY - actionH / 2f, actionW, actionH)

        // Back button
        val backY = sh * 0.12f
        val backW = sw * 0.4f
        val backH = sh * 0.055f
        shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 0.8f)
        shapeRenderer.rect(cx - backW / 2f, backY - backH / 2f, backW, backH)

        shapeRenderer.end()

        // Text
        game.batch.begin()

        // Title
        game.fontLarge.color = Color(0.3f, 0.7f, 1f, 1f)
        layout.setText(game.fontLarge, "GARAGE")
        game.fontLarge.draw(game.batch, layout, (sw - layout.width) / 2f, sh * 0.95f)

        // Coins
        game.font.color = Color.GOLD
        layout.setText(game.font, "Coins: ${game.prefs.totalCoins}")
        game.font.draw(game.batch, layout, (sw - layout.width) / 2f, sh * 0.87f)

        // Car name
        game.fontLarge.color = Color.WHITE
        layout.setText(game.fontLarge, car.name)
        game.fontLarge.draw(game.batch, layout, (sw - layout.width) / 2f, sh * 0.54f)

        // Car model file as placeholder for 3D preview
        game.font.color = Color(0.5f, 0.5f, 0.6f, 1f)
        layout.setText(game.font, "[3D Model: ${car.modelFile}]")
        game.font.draw(game.batch, layout, (sw - layout.width) / 2f, sh * 0.66f)

        // Stats labels
        game.font.color = Color.WHITE
        game.font.draw(game.batch, "Speed", barX, speedY + barH + sh * 0.03f)
        game.font.draw(game.batch, "Handling", barX, handlingY + barH + sh * 0.03f)

        // Arrows
        game.fontLarge.color = Color.WHITE
        if (selectedIndex > 0) game.fontLarge.draw(game.batch, "<", sw * 0.03f, sh * 0.67f)
        if (selectedIndex < PlayerCarDef.ALL.size - 1) game.fontLarge.draw(game.batch, ">", sw * 0.91f, sh * 0.67f)

        // Action button text
        game.font.color = Color.WHITE
        val actionText = when {
            equipped -> "EQUIPPED"
            unlocked -> "EQUIP"
            game.prefs.totalCoins >= car.price -> "BUY (${car.price})"
            else -> "NEED ${car.price} COINS"
        }
        layout.setText(game.font, actionText)
        game.font.draw(game.batch, layout, cx - layout.width / 2f, actionY + layout.height / 2f)

        // Lock indicator
        if (!unlocked) {
            game.font.color = Color.RED
            layout.setText(game.font, "LOCKED")
            game.font.draw(game.batch, layout, (sw - layout.width) / 2f, sh * 0.35f)
        }

        // Back
        game.font.color = Color.WHITE
        layout.setText(game.font, "BACK")
        game.font.draw(game.batch, layout, cx - layout.width / 2f, backY + layout.height / 2f)

        game.batch.end()

        // Touch
        if (Gdx.input.justTouched()) {
            val tx = Gdx.input.x.toFloat()
            val ty = sh - Gdx.input.y.toFloat()

            // Left arrow
            if (tx < sw * 0.15f && ty > sh * 0.55f && ty < sh * 0.75f && selectedIndex > 0) {
                selectedIndex--
                return
            }
            // Right arrow
            if (tx > sw * 0.85f && ty > sh * 0.55f && ty < sh * 0.75f && selectedIndex < PlayerCarDef.ALL.size - 1) {
                selectedIndex++
                return
            }

            // Action button
            if (tx > cx - actionW / 2f && tx < cx + actionW / 2f &&
                ty > actionY - actionH / 2f && ty < actionY + actionH / 2f) {
                when {
                    equipped -> {} // already equipped
                    unlocked -> { game.prefs.selectedCar = car.id }
                    game.prefs.totalCoins >= car.price -> {
                        game.prefs.totalCoins -= car.price
                        game.prefs.unlockCar(car.id)
                        game.prefs.selectedCar = car.id
                    }
                }
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
