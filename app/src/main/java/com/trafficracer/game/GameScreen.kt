package com.trafficracer.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.assets.loaders.ModelLoader
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import kotlin.math.abs
import kotlin.math.sin

class GameScreen(private val game: TrafficRacerGame) : ScreenAdapter() {

    private lateinit var camera: PerspectiveCamera
    private lateinit var modelBatch: ModelBatch
    private lateinit var environment: Environment
    private lateinit var shapeRenderer: ShapeRenderer
    private val world = GameWorld(game.prefs)

    // Models
    private val carModels = mutableMapOf<String, Model>()
    private var roadModel: Model? = null
    private var roadInstances = mutableListOf<ModelInstance>()

    // Instances
    private var playerInstance: ModelInstance? = null
    private val trafficInstances = mutableListOf<ModelInstance>()

    // HUD
    private lateinit var layout: GlyphLayout
    private var paused = false

    // Touch input tracking
    private val touchActions = mutableMapOf<Int, String>() // pointer -> action
    private var tiltX = 0f

    // Environment colors
    private data class EnvColors(
        val skyTop: Color, val skyBottom: Color,
        val roadColor: Color, val grassColor: Color,
        val fogColor: Color, val lightDir: Vector3, val lightColor: Color
    )
    private val envConfigs = listOf(
        EnvColors(Color(0.4f, 0.6f, 0.9f, 1f), Color(0.7f, 0.85f, 1f, 1f),
            Color(0.25f, 0.25f, 0.27f, 1f), Color(0.2f, 0.6f, 0.15f, 1f),
            Color(0.7f, 0.85f, 1f, 1f), Vector3(-0.5f, -0.8f, -0.3f), Color.WHITE),
        EnvColors(Color(0.9f, 0.6f, 0.3f, 1f), Color(1f, 0.9f, 0.6f, 1f),
            Color(0.4f, 0.35f, 0.25f, 1f), Color(0.8f, 0.7f, 0.4f, 1f),
            Color(1f, 0.9f, 0.6f, 1f), Vector3(-0.3f, -0.9f, -0.2f), Color(1f, 0.95f, 0.8f, 1f)),
        EnvColors(Color(0.75f, 0.8f, 0.9f, 1f), Color(0.9f, 0.92f, 0.95f, 1f),
            Color(0.5f, 0.5f, 0.52f, 1f), Color(0.85f, 0.9f, 0.95f, 1f),
            Color(0.9f, 0.92f, 0.95f, 1f), Vector3(-0.5f, -0.7f, -0.4f), Color(0.9f, 0.9f, 1f, 1f)),
        EnvColors(Color(0.05f, 0.05f, 0.15f, 1f), Color(0.1f, 0.1f, 0.2f, 1f),
            Color(0.15f, 0.15f, 0.17f, 1f), Color(0.05f, 0.15f, 0.05f, 1f),
            Color(0.1f, 0.1f, 0.2f, 1f), Vector3(-0.3f, -0.6f, -0.5f), Color(0.5f, 0.5f, 0.7f, 1f)),
        EnvColors(Color(0.3f, 0.5f, 0.3f, 1f), Color(0.5f, 0.7f, 0.4f, 1f),
            Color(0.2f, 0.2f, 0.22f, 1f), Color(0.15f, 0.45f, 0.1f, 1f),
            Color(0.5f, 0.7f, 0.4f, 1f), Vector3(-0.5f, -0.8f, -0.2f), Color(0.9f, 1f, 0.8f, 1f))
    )

    override fun show() {
        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()

        camera = PerspectiveCamera(67f, w, h).apply {
            position.set(0f, Constants.CAM_HEIGHT, -Constants.CAM_DISTANCE)
            lookAt(0f, 1f, Constants.CAM_LOOK_AHEAD)
            near = 0.5f
            far = 300f
            update()
        }

        modelBatch = ModelBatch()
        shapeRenderer = ShapeRenderer()
        layout = GlyphLayout()

        environment = Environment().apply {
            set(ColorAttribute(ColorAttribute.AmbientLight, 0.5f, 0.5f, 0.5f, 1f))
            add(DirectionalLight().set(Color.WHITE, -0.5f, -0.8f, -0.3f))
        }

        loadModels()
        buildRoad()
        world.reset()
        updatePlayerInstance()

        setupInput()
    }

    private fun loadModels() {
        val loader = ObjLoader()
        val allFiles = mutableSetOf<String>()
        PlayerCarDef.ALL.forEach { allFiles.add(it.modelFile) }
        TrafficCarDef.ALL.forEach { allFiles.add(it.modelFile) }

        for (file in allFiles) {
            try {
                val model = loader.loadModel(Gdx.files.internal("models/$file"))
                carModels[file] = model
            } catch (e: Exception) {
                Gdx.app.error("GameScreen", "Failed to load model: $file", e)
            }
        }
    }

    private fun buildRoad() {
        val builder = ModelBuilder()
        val roadMat = Material(ColorAttribute.createDiffuse(Color.DARK_GRAY))
        val attr = (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()

        // Create road segments
        roadInstances.clear()
        val segLen = Constants.ROAD_SEGMENT_LENGTH
        val halfW = Constants.ROAD_WIDTH / 2f + 1f // slightly wider for shoulders

        roadModel = builder.createBox(halfW * 2f, 0.1f, segLen, roadMat, attr)

        for (i in -2..10) {
            val instance = ModelInstance(roadModel!!)
            instance.transform.setToTranslation(0f, -0.05f, i * segLen)
            roadInstances.add(instance)
        }
    }

    private fun updatePlayerInstance() {
        val model = carModels[world.selectedCarDef.modelFile]
        if (model != null) {
            playerInstance = ModelInstance(model)
        }
    }

    private fun setupInput() {
        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                if (world.gameOver) {
                    game.setScreen(MenuScreen(game))
                    return true
                }
                if (paused) {
                    paused = false
                    return true
                }
                classifyTouch(screenX.toFloat(), screenY.toFloat(), pointer, true)
                return true
            }

            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val action = touchActions.remove(pointer)
                when (action) {
                    "brake" -> world.isBraking = false
                    "gas" -> world.isAccelerating = false
                    "steer" -> world.steerInput = 0f
                    "left" -> world.steerInput = 0f
                    "right" -> world.steerInput = 0f
                    "nos" -> world.deactivateNOS()
                }
                return true
            }

            override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                val action = touchActions[pointer]
                if (action == "steer") {
                    val sw = Gdx.graphics.width.toFloat()
                    val cx = sw / 2f
                    val normalized = ((screenX - cx) / (sw * 0.3f)).coerceIn(-1f, 1f)
                    world.steerInput = normalized
                }
                return true
            }

            override fun keyDown(keycode: Int): Boolean {
                if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
                    if (world.gameOver) game.setScreen(MenuScreen(game))
                    else paused = !paused
                    return true
                }
                return false
            }
        }
    }

    private fun classifyTouch(x: Float, y: Float, pointer: Int, isDown: Boolean) {
        val sw = Gdx.graphics.width.toFloat()
        val sh = Gdx.graphics.height.toFloat()

        // Pause button area (top right)
        if (x > sw * 0.85f && y < sh * 0.08f) {
            paused = !paused
            return
        }

        when (world.controlScheme) {
            0 -> { // NFS
                val controlsTop = sh * 0.75f
                if (y > controlsTop) {
                    when {
                        x < sw * 0.2f -> { touchActions[pointer] = "brake"; world.isBraking = true }
                        x > sw * 0.8f -> { touchActions[pointer] = "gas"; world.isAccelerating = true }
                        y > sh * 0.9f && x > sw * 0.35f && x < sw * 0.65f -> { touchActions[pointer] = "nos"; world.activateNOS() }
                        else -> { touchActions[pointer] = "steer"; val cx = sw / 2f; world.steerInput = ((x - cx) / (sw * 0.3f)).coerceIn(-1f, 1f) }
                    }
                }
            }
            1 -> { // Simple - tilt steering, tap = brake
                val controlsTop = sh * 0.75f
                if (y > controlsTop) {
                    if (y > sh * 0.9f && x > sw * 0.35f && x < sw * 0.65f) {
                        touchActions[pointer] = "nos"; world.activateNOS()
                    } else {
                        touchActions[pointer] = "brake"; world.isBraking = true
                    }
                }
            }
            2 -> { // Arcade - buttons
                val controlsTop = sh * 0.75f
                if (y > controlsTop) {
                    when {
                        x < sw * 0.25f -> { touchActions[pointer] = "left"; world.steerInput = -1f }
                        x > sw * 0.75f -> { touchActions[pointer] = "right"; world.steerInput = 1f }
                        y > sh * 0.9f && x > sw * 0.35f && x < sw * 0.65f -> { touchActions[pointer] = "nos"; world.activateNOS() }
                        else -> { touchActions[pointer] = "brake"; world.isBraking = true }
                    }
                }
            }
        }
    }

    override fun render(delta: Float) {
        val dt = if (paused) 0f else delta.coerceAtMost(0.05f)

        // Tilt input for simple scheme
        if (world.controlScheme == 1 && !paused && !world.gameOver) {
            tiltX = -Gdx.input.accelerometerX
            world.steerInput = (tiltX / 4f).coerceIn(-1f, 1f)
            world.isAccelerating = true
        }
        if (world.controlScheme == 2 && !paused && !world.gameOver) {
            world.isAccelerating = true
        }

        world.update(dt)
        updateCamera(dt)

        // Environment
        val env = envConfigs[world.envIndex % envConfigs.size]

        // Clear with sky color
        Gdx.gl.glClearColor(env.skyTop.r, env.skyTop.g, env.skyTop.b, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)

        // Update environment lighting
        environment.clear()
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
        environment.add(DirectionalLight().set(env.lightColor, env.lightDir))

        // Screen shake
        if (world.shakeTimer > 0) {
            val intensity = world.shakeIntensity * (world.shakeTimer / 0.5f)
            camera.position.x += (MathUtils.random() - 0.5f) * intensity * 0.05f
            camera.position.y += (MathUtils.random() - 0.5f) * intensity * 0.03f
            camera.update()
        }

        // 3D rendering
        modelBatch.begin(camera)

        // Road
        renderRoad(env)

        // Grass planes
        renderGrass(env)

        // Player car
        renderPlayer()

        // Traffic
        renderTraffic()

        // Coins
        renderCoins()

        modelBatch.end()

        // HUD (2D overlay)
        renderHUD(env)
    }

    private fun updateCamera(dt: Float) {
        val targetX = world.playerX * 0.7f
        val targetZ = -Constants.CAM_DISTANCE
        val targetY = Constants.CAM_HEIGHT

        camera.position.x = MathUtils.lerp(camera.position.x, targetX, dt * 5f)
        camera.position.y = MathUtils.lerp(camera.position.y, targetY, dt * 3f)
        camera.position.z = MathUtils.lerp(camera.position.z, targetZ, dt * 5f)

        val lookTarget = Vector3(world.playerX * 0.3f, 1f, Constants.CAM_LOOK_AHEAD)
        camera.lookAt(lookTarget)
        camera.up.set(0f, 1f, 0f)
        camera.update()
    }

    private val tmpVec = Vector3()

    private fun renderRoad(env: EnvColors) {
        // Move road segments to stay around the camera
        val camZ = 0f // player is always at z=0 in world coords
        for (inst in roadInstances) {
            val segZ = inst.transform.getTranslation(tmpVec).z
            // Cycle segments
            val totalLen = roadInstances.size * Constants.ROAD_SEGMENT_LENGTH
            if (segZ < -Constants.ROAD_SEGMENT_LENGTH * 2) {
                inst.transform.setToTranslation(0f, -0.05f, segZ + totalLen)
            }

            // Color road based on environment
            for (mat in inst.materials) {
                mat.set(ColorAttribute.createDiffuse(env.roadColor))
            }
        }
        for (inst in roadInstances) modelBatch.render(inst, environment)
    }

    private var grassModel: Model? = null
    private var grassInstances: MutableList<ModelInstance>? = null

    private fun renderGrass(env: EnvColors) {
        if (grassModel == null) {
            val builder = ModelBuilder()
            val grassMat = Material(ColorAttribute.createDiffuse(env.grassColor))
            val attr = (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
            grassModel = builder.createBox(60f, 0.05f, Constants.ROAD_SEGMENT_LENGTH * 13f, grassMat, attr)
            grassInstances = mutableListOf(
                ModelInstance(grassModel!!).apply { transform.setToTranslation(-37f, -0.1f, 200f) },
                ModelInstance(grassModel!!).apply { transform.setToTranslation(37f, -0.1f, 200f) }
            )
        }
        for (inst in grassInstances!!) {
            for (mat in inst.materials) {
                mat.set(ColorAttribute.createDiffuse(env.grassColor))
            }
            modelBatch.render(inst, environment)
        }
    }

    private fun renderPlayer() {
        val inst = playerInstance ?: return
        inst.transform.setToTranslation(world.playerX, 0f, 0f)
        inst.transform.rotate(Vector3.Y, 180f) // face forward (away from camera)

        // Flicker when invincible
        if (world.isInvincible && (System.currentTimeMillis() % 200 < 100)) return
        modelBatch.render(inst, environment)
    }

    private fun renderTraffic() {
        // Reuse/create instances as needed
        while (trafficInstances.size < world.trafficCars.size) {
            trafficInstances.add(ModelInstance(carModels.values.first()))
        }

        for (i in world.trafficCars.indices) {
            val car = world.trafficCars[i]
            val def = TrafficCarDef.ALL[car.defIndex]
            val model = carModels[def.modelFile] ?: continue
            val inst = trafficInstances[i]

            // Swap model if different
            if (inst.model != model) {
                trafficInstances[i] = ModelInstance(model)
            }
            val drawInst = trafficInstances[i]
            drawInst.transform.setToTranslation(car.x, 0f, car.z)
            drawInst.transform.rotate(Vector3.Y, 180f)
            modelBatch.render(drawInst, environment)
        }
    }

    private var coinModel: Model? = null
    private val coinInstances = mutableListOf<ModelInstance>()

    private fun renderCoins() {
        if (coinModel == null) {
            val builder = ModelBuilder()
            val coinMat = Material(ColorAttribute.createDiffuse(Color.GOLD))
            val attr = (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
            coinModel = builder.createCylinder(0.6f, 0.15f, 0.6f, 12, coinMat, attr)
        }

        while (coinInstances.size < world.roadCoins.size) {
            coinInstances.add(ModelInstance(coinModel!!))
        }

        for (i in world.roadCoins.indices) {
            val coin = world.roadCoins[i]
            if (coin.collected) continue
            val inst = coinInstances[i]
            inst.transform.setToTranslation(coin.x, 0.5f, coin.z)
            inst.transform.rotate(Vector3.Y, (System.currentTimeMillis() % 3600) * 0.1f)
            modelBatch.render(inst, environment)
        }
    }

    private fun renderHUD(env: EnvColors) {
        val sw = Gdx.graphics.width.toFloat()
        val sh = Gdx.graphics.height.toFloat()

        // HUD background
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Top bar
        shapeRenderer.setColor(0f, 0f, 0f, 0.5f)
        shapeRenderer.rect(0f, sh - sh * 0.08f, sw, sh * 0.08f)

        // Speed bar (bottom left)
        val speedRatio = world.playerSpeed / (Constants.MAX_SPEED * world.selectedCarDef.baseSpeed)
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.7f)
        shapeRenderer.rect(sw * 0.02f, sh * 0.02f, sw * 0.25f, sh * 0.025f)
        shapeRenderer.setColor(0f, 0.9f, 0.4f, 1f)
        shapeRenderer.rect(sw * 0.02f, sh * 0.02f, sw * 0.25f * speedRatio, sh * 0.025f)

        // NOS bar
        val nosRatio = world.nosAmount / 100f
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.7f)
        shapeRenderer.rect(sw * 0.02f, sh * 0.055f, sw * 0.25f, sh * 0.02f)
        val nosColor = if (world.nosActive) Color(0f, 0.9f, 1f, 1f) else Color(0.2f, 0.5f, 1f, 1f)
        shapeRenderer.setColor(nosColor)
        shapeRenderer.rect(sw * 0.02f, sh * 0.055f, sw * 0.25f * nosRatio, sh * 0.02f)

        // Controls area background
        shapeRenderer.setColor(0f, 0f, 0f, 0.3f)
        shapeRenderer.rect(0f, 0f, sw, sh * 0.25f)

        // Control scheme specific
        renderControlsShape(sw, sh)

        shapeRenderer.end()

        // Text overlay
        game.batch.begin()
        game.font.color = Color.WHITE

        // Score
        game.font.draw(game.batch, "Score: ${world.score}", sw * 0.02f, sh - sh * 0.01f)
        game.font.draw(game.batch, "Coins: ${world.coins}", sw * 0.35f, sh - sh * 0.01f)

        // Speed text
        val kmh = (world.playerSpeed * 3).toInt()
        game.font.draw(game.batch, "${kmh} km/h", sw * 0.02f, sh * 0.09f)
        game.font.draw(game.batch, "NOS", sw * 0.02f, sh * 0.12f)

        // Lives
        game.font.draw(game.batch, "Lives: ${"❤".repeat(world.lives)}", sw * 0.7f, sh - sh * 0.01f)

        // Pause button
        game.font.draw(game.batch, "||", sw * 0.93f, sh - sh * 0.01f)

        // Combo
        if (world.comboCount > 0) {
            game.fontLarge.color = Color.YELLOW
            layout.setText(game.fontLarge, "COMBO x${world.comboCount}")
            game.fontLarge.draw(game.batch, layout, (sw - layout.width) / 2f, sh * 0.65f)
        }

        // Floating texts
        for (ft in world.floatingTexts) {
            val alpha = (ft.timer / 2f).coerceIn(0f, 1f)
            game.fontLarge.color = Color(1f, 0.1f, 0.1f, alpha)
            layout.setText(game.fontLarge, ft.text)
            game.fontLarge.draw(game.batch, layout, (sw - layout.width) / 2f, sh * 0.5f + (2f - ft.timer) * 30f)
        }

        // Game over
        if (world.gameOver) {
            game.fontLarge.color = Color.RED
            layout.setText(game.fontLarge, "GAME OVER")
            game.fontLarge.draw(game.batch, layout, (sw - layout.width) / 2f, sh * 0.6f)
            game.font.color = Color.WHITE
            layout.setText(game.font, "Score: ${world.score}  |  High: ${game.prefs.highScore}")
            game.font.draw(game.batch, layout, (sw - layout.width) / 2f, sh * 0.45f)
            layout.setText(game.font, "Tap to continue")
            game.font.draw(game.batch, layout, (sw - layout.width) / 2f, sh * 0.35f)
        }

        // Paused
        if (paused) {
            game.fontLarge.color = Color.WHITE
            layout.setText(game.fontLarge, "PAUSED")
            game.fontLarge.draw(game.batch, layout, (sw - layout.width) / 2f, sh * 0.55f)
            game.font.color = Color.WHITE
            layout.setText(game.font, "Tap to resume")
            game.font.draw(game.batch, layout, (sw - layout.width) / 2f, sh * 0.42f)
        }

        // Environment name (brief)
        game.font.color = Color(1f, 1f, 1f, 0.3f)
        game.font.draw(game.batch, world.environments[world.envIndex], sw * 0.85f, sh * 0.09f)

        game.batch.end()
    }

    private fun renderControlsShape(sw: Float, sh: Float) {
        when (world.controlScheme) {
            0 -> { // NFS
                // Brake pedal
                val brakeColor = if (world.isBraking) Color(1f, 0.1f, 0.1f, 0.8f) else Color(0.5f, 0.2f, 0.2f, 0.5f)
                shapeRenderer.setColor(brakeColor)
                shapeRenderer.rect(sw * 0.02f, sh * 0.03f, sw * 0.15f, sh * 0.18f)

                // Gas pedal
                val gasColor = if (world.isAccelerating) Color(0f, 0.9f, 0.3f, 0.8f) else Color(0.2f, 0.5f, 0.2f, 0.5f)
                shapeRenderer.setColor(gasColor)
                shapeRenderer.rect(sw * 0.83f, sh * 0.03f, sw * 0.15f, sh * 0.18f)

                // Steer zone
                shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 0.3f)
                shapeRenderer.rect(sw * 0.2f, sh * 0.03f, sw * 0.6f, sh * 0.15f)

                // NOS button
                val nosColor = if (world.nosActive) Color(0f, 0.9f, 1f, 0.8f) else Color(0.2f, 0.4f, 0.6f, 0.5f)
                shapeRenderer.setColor(nosColor)
                shapeRenderer.circle(sw * 0.5f, sh * 0.04f, sw * 0.06f)
            }
            1 -> { // Simple - tilt
                // Brake zone (whole bottom)
                val brakeColor = if (world.isBraking) Color(1f, 0.1f, 0.1f, 0.6f) else Color(0.3f, 0.3f, 0.3f, 0.3f)
                shapeRenderer.setColor(brakeColor)
                shapeRenderer.rect(0f, sh * 0.03f, sw, sh * 0.18f)

                // Tilt indicator
                val tiltPos = sw * 0.5f + world.steerInput * sw * 0.3f
                shapeRenderer.setColor(0.3f, 0.6f, 1f, 0.8f)
                shapeRenderer.circle(tiltPos, sh * 0.13f, sw * 0.03f)

                // NOS
                val nosColor = if (world.nosActive) Color(0f, 0.9f, 1f, 0.8f) else Color(0.2f, 0.4f, 0.6f, 0.5f)
                shapeRenderer.setColor(nosColor)
                shapeRenderer.circle(sw * 0.5f, sh * 0.04f, sw * 0.06f)
            }
            2 -> { // Arcade
                // Left button
                val leftColor = if (world.steerInput < -0.1f) Color(0.3f, 0.6f, 1f, 0.8f) else Color(0.2f, 0.3f, 0.5f, 0.5f)
                shapeRenderer.setColor(leftColor)
                shapeRenderer.rect(sw * 0.02f, sh * 0.03f, sw * 0.2f, sh * 0.18f)

                // Right button
                val rightColor = if (world.steerInput > 0.1f) Color(0.3f, 0.6f, 1f, 0.8f) else Color(0.2f, 0.3f, 0.5f, 0.5f)
                shapeRenderer.setColor(rightColor)
                shapeRenderer.rect(sw * 0.78f, sh * 0.03f, sw * 0.2f, sh * 0.18f)

                // Brake zone
                val brakeColor = if (world.isBraking) Color(1f, 0.1f, 0.1f, 0.6f) else Color(0.3f, 0.3f, 0.3f, 0.3f)
                shapeRenderer.setColor(brakeColor)
                shapeRenderer.rect(sw * 0.25f, sh * 0.03f, sw * 0.5f, sh * 0.15f)

                // NOS
                val nosColor = if (world.nosActive) Color(0f, 0.9f, 1f, 0.8f) else Color(0.2f, 0.4f, 0.6f, 0.5f)
                shapeRenderer.setColor(nosColor)
                shapeRenderer.circle(sw * 0.5f, sh * 0.04f, sw * 0.06f)
            }
        }
    }

    override fun resize(width: Int, height: Int) {
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()
    }

    override fun dispose() {
        modelBatch.dispose()
        shapeRenderer.dispose()
        carModels.values.forEach { it.dispose() }
        roadModel?.dispose()
        grassModel?.dispose()
        coinModel?.dispose()
    }
}
