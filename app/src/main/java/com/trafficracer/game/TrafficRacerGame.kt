package com.trafficracer.game

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch

class TrafficRacerGame(val onOrientationChanged: (Boolean) -> Unit) : Game() {
    lateinit var batch: SpriteBatch
    lateinit var font: BitmapFont
    lateinit var fontLarge: BitmapFont
    lateinit var prefs: GamePrefs

    override fun create() {
        batch = SpriteBatch()
        font = BitmapFont().apply { data.setScale(1.5f) }
        fontLarge = BitmapFont().apply { data.setScale(3f) }
        prefs = GamePrefs()
        setScreen(MenuScreen(this))
    }

    override fun dispose() {
        batch.dispose()
        font.dispose()
        fontLarge.dispose()
        screen?.dispose()
    }
}
