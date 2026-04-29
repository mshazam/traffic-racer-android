package com.trafficracer.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix

class SpriteManager(private val context: Context) {
    private val cache = mutableMapOf<String, Bitmap>()
    private val scaledCache = mutableMapOf<String, Bitmap>()

    val playerSprites = mapOf(
        "starter" to "sprites/cars/player_default.png",
        "blue_bolt" to "sprites/cars/player_sprint.png",
        "viper" to "sprites/cars/player_venom.png",
        "phantom" to "sprites/cars/player_phantom.png",
        "golden_fury" to "sprites/cars/player_blaze.png",
        "shadow" to "sprites/cars/player_muscle.png",
        "inferno" to "sprites/cars/player_turbo.png",
        "arctic" to "sprites/cars/player_thunder.png"
    )

    val trafficSprites = listOf(
        "sprites/cars/traffic_0.png",
        "sprites/cars/traffic_1.png",
        "sprites/cars/traffic_2.png",
        "sprites/cars/traffic_3.png",
        "sprites/cars/traffic_4.png",
        "sprites/cars/traffic_5.png",
        "sprites/cars/traffic_6.png",
        "sprites/cars/traffic_7.png",
        "sprites/cars/traffic_sedan.png",
        "sprites/cars/traffic_taxi.png",
        "sprites/cars/traffic_police.png",
        "sprites/cars/traffic_ambulance.png"
    )

    val trafficLargeSprites = listOf(
        "sprites/cars/traffic_bus.png",
        "sprites/cars/traffic_truck.png",
        "sprites/cars/traffic_van.png",
        "sprites/cars/traffic_suv.png",
        "sprites/cars/traffic_firetruck.png",
        "sprites/cars/traffic_transport.png"
    )

    val trafficSmallSprites = (0..3).map { "sprites/cars/traffic_small_$it.png" }

    val objectSprites = mapOf(
        "oil" to "sprites/objects/oil.png",
        "cone" to "sprites/objects/cone.png",
        "barrel" to "sprites/objects/barrel.png",
        "barrier" to "sprites/objects/barrier.png",
        "rock" to "sprites/objects/rock.png"
    )

    fun preload() {
        playerSprites.values.forEach { load(it) }
        trafficSprites.forEach { load(it) }
        trafficLargeSprites.forEach { load(it) }
        trafficSmallSprites.forEach { load(it) }
        objectSprites.values.forEach { load(it) }
    }

    fun load(assetPath: String): Bitmap? {
        cache[assetPath]?.let { return it }
        return try {
            context.assets.open(assetPath).use { stream ->
                BitmapFactory.decodeStream(stream)?.also { cache[assetPath] = it }
            }
        } catch (_: Exception) { null }
    }

    fun getScaled(assetPath: String, targetWidth: Float, targetHeight: Float): Bitmap? {
        val key = "$assetPath|${targetWidth.toInt()}x${targetHeight.toInt()}"
        scaledCache[key]?.let { if (!it.isRecycled) return it }
        val orig = load(assetPath) ?: return null
        val scaled = Bitmap.createScaledBitmap(orig, targetWidth.toInt().coerceAtLeast(1), targetHeight.toInt().coerceAtLeast(1), true)
        scaledCache[key] = scaled
        return scaled
    }

    fun getScaledRotated(assetPath: String, targetWidth: Float, targetHeight: Float, degrees: Float): Bitmap? {
        if (degrees == 0f) return getScaled(assetPath, targetWidth, targetHeight)
        val key = "$assetPath|${targetWidth.toInt()}x${targetHeight.toInt()}|r$degrees"
        scaledCache[key]?.let { if (!it.isRecycled) return it }
        val scaled = getScaled(assetPath, targetWidth, targetHeight) ?: return null
        val matrix = Matrix().apply { postRotate(degrees, scaled.width / 2f, scaled.height / 2f) }
        val rotated = Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, matrix, true)
        scaledCache[key] = rotated
        return rotated
    }

    fun getPlayerSprite(carId: String): String {
        return playerSprites[carId] ?: playerSprites.values.first()
    }

    fun getTrafficSprite(index: Int): String {
        val allTraffic = trafficSprites + trafficLargeSprites + trafficSmallSprites
        return allTraffic.getOrElse(index % allTraffic.size) { trafficSprites[0] }
    }

    fun isLargeVehicle(index: Int): Boolean {
        val offset = trafficSprites.size
        return index in offset until (offset + trafficLargeSprites.size)
    }

    fun release() {
        cache.values.forEach { if (!it.isRecycled) it.recycle() }
        scaledCache.values.forEach { if (!it.isRecycled) it.recycle() }
        cache.clear(); scaledCache.clear()
    }
}
