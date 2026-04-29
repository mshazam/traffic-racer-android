package com.trafficracer.game

data class PlayerCarDef(
    val id: String,
    val name: String,
    val modelFile: String,
    val baseSpeed: Float,
    val handling: Float,
    val price: Int,
    val color: FloatArray = floatArrayOf(1f, 1f, 1f)
) {
    companion object {
        val ALL = listOf(
            PlayerCarDef("sedan-sports", "Sport Sedan", "sedan-sports.obj", 1.0f, 1.0f, 0),
            PlayerCarDef("race", "Racer", "race.obj", 1.2f, 1.1f, 500),
            PlayerCarDef("race-future", "Future", "race-future.obj", 1.4f, 1.2f, 1500),
            PlayerCarDef("hatchback-sports", "Hatchback", "hatchback-sports.obj", 0.9f, 1.3f, 300),
            PlayerCarDef("suv-luxury", "Luxury SUV", "suv-luxury.obj", 0.8f, 0.8f, 800)
        )
    }
}

data class TrafficCarDef(
    val modelFile: String,
    val isLarge: Boolean = false,
    val speedFactor: Float = 1f
) {
    companion object {
        val ALL = listOf(
            TrafficCarDef("sedan.obj"),
            TrafficCarDef("taxi.obj"),
            TrafficCarDef("van.obj"),
            TrafficCarDef("suv.obj"),
            TrafficCarDef("police.obj"),
            TrafficCarDef("delivery.obj", isLarge = true, speedFactor = 0.7f),
            TrafficCarDef("ambulance.obj", isLarge = true, speedFactor = 0.8f),
            TrafficCarDef("truck.obj", isLarge = true, speedFactor = 0.6f),
            TrafficCarDef("truck-flat.obj", isLarge = true, speedFactor = 0.6f),
            TrafficCarDef("firetruck.obj", isLarge = true, speedFactor = 0.75f)
        )
    }
}
