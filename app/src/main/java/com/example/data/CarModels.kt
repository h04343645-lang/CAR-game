package com.example.data

import androidx.compose.ui.graphics.Color

enum class GameMode(val displayName: String, val description: String) {
    PLAYER_DRIVE("Player Drive", "Manual high-speed driving with AI safety assist"),
    AI_SIMULATION("Autonomous AI", "Self-driving car using Raycast sensor navigation"),
    TRAFFIC_DODGE("Traffic Dodge", "High-speed highway obstacle dodging"),
    AI_RACE_BATTLE("Multi-AI Battle", "2 to 4 autonomous cars racing together")
}

enum class TrackType(val displayName: String, val width: Float, val length: Float) {
    CYBER_CIRCUIT("Cyber Circuit", 700f, 2200f),
    NEON_HIGHWAY("Neon Highway", 480f, 4000f),
    TEST_SANDBOX("AI Sensor Lab", 800f, 1500f)
}

enum class ObstacleType(val displayName: String, val radius: Float, val isHazard: Boolean) {
    BARRIER("Road Block", 28f, true),
    ROCK("Concrete Block", 22f, true),
    OIL_SLICK("Oil Spill", 32f, true),
    TRAFFIC_CAR("Traffic Vehicle", 30f, true),
    NITRO_CANISTER("Nitro Boost", 18f, false),
    COIN("Energy Credit", 16f, false)
}

data class ObstacleEntity(
    val id: Long,
    var x: Float,
    var y: Float,
    var radius: Float,
    val type: ObstacleType,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var isHit: Boolean = false,
    var rotation: Float = 0f
)

data class Waypoint(
    val x: Float,
    val y: Float,
    val targetSpeed: Float = 220f
)

data class RaycastResult(
    val index: Int,
    val label: String,
    val angleDegrees: Float,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val hitX: Float,
    val hitY: Float,
    val hitDistance: Float,
    val maxRange: Float,
    val isHit: Boolean,
    val hitType: ObstacleType? = null
)

data class CarModel(
    val id: String,
    val name: String,
    val description: String,
    val baseTopSpeed: Float, // km/h
    val baseAcceleration: Float,
    val baseHandling: Float,
    val baseSensorRange: Float,
    val price: Int,
    val isUnlocked: Boolean,
    val primaryColorHex: Long,
    val accentColorHex: Long,
    val glowColorHex: Long
)

data class CarUpgrade(
    val engineLevel: Int = 1,
    val handlingLevel: Int = 1,
    val sensorLevel: Int = 1,
    val nitroLevel: Int = 1
)

data class AdminSettings(
    val godMode: Boolean = false,
    val infiniteNitro: Boolean = false,
    val speedMultiplier: Float = 1.0f,
    val accelMultiplier: Float = 1.0f,
    val sensorRangeMultiplier: Float = 1.0f,
    val steerAvoidanceSensitivity: Float = 1.5f,
    val sensorRayCount: Int = 5, // 3, 5, 7 rays
    val sensorSpreadAngle: Float = 36f, // degrees
    val showSensorLasers: Boolean = true,
    val showDebugHud: Boolean = true,
    val obstacleDensity: Float = 1.0f,
    val trafficSpeed: Float = 70f,
    val simulationSpeed: Float = 1.0f, // 0.5x to 2.0x
    val noWallCollisions: Boolean = false,
    val autoAvoidAssistInPlayerMode: Boolean = true,
    val adEngineRemoved: Boolean = true // Explicitly verifies ads are 100% removed
)

data class AiTelemetry(
    val speedKmh: Float = 0f,
    val steeringAngleDeg: Float = 0f,
    val throttlePct: Float = 0f,
    val brakePct: Float = 0f,
    val closestObstacleDist: Float = 999f,
    val aiDecision: String = "Cruising",
    val currentWaypointIndex: Int = 0,
    val totalLaps: Int = 0,
    val nitroLevelPct: Float = 100f,
    val healthPct: Float = 100f,
    val score: Int = 0,
    val fps: Int = 60
)
