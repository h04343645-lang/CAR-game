package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.data.*
import com.example.engine.Particle
import com.example.engine.VehicleState
import kotlin.math.*

@Composable
fun GameCanvas(
    playerVehicle: VehicleState?,
    opponentVehicles: List<VehicleState>,
    obstacles: List<ObstacleEntity>,
    waypoints: List<Waypoint>,
    particles: List<Particle>,
    adminSettings: AdminSettings,
    trackMinX: Float,
    trackMaxX: Float,
    onTapWorld: ((Float, Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("game_canvas")
            .pointerInput(Unit) {
                if (onTapWorld != null) {
                    detectTapGestures { tapOffset ->
                        val player = playerVehicle ?: return@detectTapGestures
                        val screenCenterX = size.width / 2f
                        val screenCenterY = size.height * 0.65f
                        val worldX = (tapOffset.x - screenCenterX) + player.x
                        val worldY = (tapOffset.y - screenCenterY) + player.y
                        onTapWorld(worldX, worldY)
                    }
                }
            }
    ) {
        val player = playerVehicle ?: return@Canvas
        val screenCenterX = size.width / 2f
        val screenCenterY = size.height * 0.65f

        // Camera transform offset
        val camOffsetX = screenCenterX - player.x
        val camOffsetY = screenCenterY - player.y

        // 1. Draw Background Grid & Asphalt Track
        drawTrack(
            camOffsetX = camOffsetX,
            camOffsetY = camOffsetY,
            trackMinX = trackMinX,
            trackMaxX = trackMaxX,
            waypoints = waypoints,
            screenWidth = size.width,
            screenHeight = size.height
        )

        // 2. Draw Obstacles & Collectibles
        for (obs in obstacles) {
            val screenX = obs.x + camOffsetX
            val screenY = obs.y + camOffsetY
            // Frustum cull
            if (screenX < -100f || screenX > size.width + 100f || screenY < -100f || screenY > size.height + 100f) {
                continue
            }
            drawObstacle(obs, screenX, screenY)
        }

        // 3. Draw Particles (tire smoke, sparks, nitro exhaust)
        for (p in particles) {
            val px = p.x + camOffsetX
            val py = p.y + camOffsetY
            val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            drawCircle(
                color = Color(p.colorHex).copy(alpha = alpha),
                radius = p.size * (1f + (1f - alpha) * 0.5f),
                center = Offset(px, py)
            )
        }

        // 4. Draw Opponent AI Cars
        for (aiCar in opponentVehicles) {
            drawCarVehicle(
                vehicle = aiCar,
                camOffsetX = camOffsetX,
                camOffsetY = camOffsetY,
                showSensors = adminSettings.showSensorLasers
            )
        }

        // 5. Draw Player Car & Raycast Sensor Lasers
        drawCarVehicle(
            vehicle = player,
            camOffsetX = camOffsetX,
            camOffsetY = camOffsetY,
            showSensors = adminSettings.showSensorLasers
        )
    }
}

private fun DrawScope.drawTrack(
    camOffsetX: Float,
    camOffsetY: Float,
    trackMinX: Float,
    trackMaxX: Float,
    waypoints: List<Waypoint>,
    screenWidth: Float,
    screenHeight: Float
) {
    // Cyber dark grid
    val gridSpacing = 80f
    val gridStartX = (camOffsetX % gridSpacing)
    val gridStartY = (camOffsetY % gridSpacing)

    var gx = gridStartX
    while (gx < screenWidth) {
        drawLine(
            color = Color(0xFF131D33),
            start = Offset(gx, 0f),
            end = Offset(gx, screenHeight),
            strokeWidth = 1f
        )
        gx += gridSpacing
    }

    var gy = gridStartY
    while (gy < screenHeight) {
        drawLine(
            color = Color(0xFF131D33),
            start = Offset(0f, gy),
            end = Offset(screenWidth, gy),
            strokeWidth = 1f
        )
        gy += gridSpacing
    }

    // Asphalt Track Surface
    val trackLeftScreen = trackMinX + camOffsetX
    val trackRightScreen = trackMaxX + camOffsetX
    val trackWidth = trackMaxX - trackMinX

    drawRect(
        color = Color(0xFF151924),
        topLeft = Offset(trackLeftScreen, 0f),
        size = Size(trackWidth, screenHeight)
    )

    // Neon Track Borders (curbs)
    drawLine(
        color = Color(0xFF00F0FF),
        start = Offset(trackLeftScreen, 0f),
        end = Offset(trackLeftScreen, screenHeight),
        strokeWidth = 6f
    )
    drawLine(
        color = Color(0xFF00F0FF),
        start = Offset(trackRightScreen, 0f),
        end = Offset(trackRightScreen, screenHeight),
        strokeWidth = 6f
    )

    // Center Dashed Lane Divider
    val trackCenterScreen = (trackLeftScreen + trackRightScreen) / 2f
    val dashSpacing = 60f
    val dashOffset = (camOffsetY % dashSpacing)
    var dy = dashOffset - dashSpacing
    while (dy < screenHeight + dashSpacing) {
        drawLine(
            color = Color(0x66FFD600),
            start = Offset(trackCenterScreen, dy),
            end = Offset(trackCenterScreen, dy + 32f),
            strokeWidth = 3f
        )
        dy += dashSpacing
    }

    // Racing line Waypoint markers (circuit visual hints)
    for (i in waypoints.indices) {
        val wp = waypoints[i]
        val sx = wp.x + camOffsetX
        val sy = wp.y + camOffsetY
        if (sx in -50f..screenWidth + 50f && sy in -50f..screenHeight + 50f) {
            drawCircle(
                color = Color(0x3300F0FF),
                radius = 18f,
                center = Offset(sx, sy)
            )
            drawCircle(
                color = Color(0x8800F0FF),
                radius = 5f,
                center = Offset(sx, sy)
            )
        }
    }
}

private fun DrawScope.drawObstacle(obs: ObstacleEntity, screenX: Float, screenY: Float) {
    val center = Offset(screenX, screenY)

    when (obs.type) {
        ObstacleType.BARRIER -> {
            // High-tech red/orange barrier with hazard stripes
            drawCircle(color = Color(0xFFFF3366), radius = obs.radius, center = center)
            drawCircle(color = Color(0xFF220011), radius = obs.radius * 0.7f, center = center)
            drawCircle(color = Color(0xFFFFD600), radius = obs.radius * 0.35f, center = center)
        }
        ObstacleType.ROCK -> {
            drawCircle(color = Color(0xFF6B7280), radius = obs.radius, center = center)
            drawCircle(color = Color(0xFF374151), radius = obs.radius * 0.65f, center = center)
        }
        ObstacleType.OIL_SLICK -> {
            // Dark purple iridescent slick
            drawCircle(color = Color(0xAA7C3AED), radius = obs.radius, center = center)
            drawCircle(color = Color(0xCC06B6D4), radius = obs.radius * 0.55f, center = center)
        }
        ObstacleType.TRAFFIC_CAR -> {
            // Dynamic moving traffic vehicle
            drawRoundRect(
                color = Color(0xFFE11D48),
                topLeft = Offset(screenX - 18f, screenY - 32f),
                size = Size(36f, 64f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )
            // Windshield
            drawRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(screenX - 14f, screenY - 14f),
                size = Size(28f, 18f)
            )
            // Headlights
            drawCircle(color = Color(0xFFFDE047), radius = 4f, center = Offset(screenX - 12f, screenY - 30f))
            drawCircle(color = Color(0xFFFDE047), radius = 4f, center = Offset(screenX + 12f, screenY - 30f))
        }
        ObstacleType.NITRO_CANISTER -> {
            // Glowing cyan nitro pickup
            drawCircle(color = Color(0x5500F0FF), radius = obs.radius + 6f, center = center)
            drawCircle(color = Color(0xFF00F0FF), radius = obs.radius, center = center)
            drawCircle(color = Color.White, radius = obs.radius * 0.4f, center = center)
        }
        ObstacleType.COIN -> {
            // Rotating gold coin
            drawCircle(color = Color(0x66FFD600), radius = obs.radius + 4f, center = center)
            drawCircle(color = Color(0xFFFFD600), radius = obs.radius, center = center)
            drawCircle(color = Color(0xFFFFF59D), radius = obs.radius * 0.5f, center = center)
        }
    }
}

private fun DrawScope.drawCarVehicle(
    vehicle: VehicleState,
    camOffsetX: Float,
    camOffsetY: Float,
    showSensors: Boolean
) {
    val carScreenX = vehicle.x + camOffsetX
    val carScreenY = vehicle.y + camOffsetY
    val carCenter = Offset(carScreenX, carScreenY)

    // 1. Draw Raycast Sensor Lasers (Autonomous Raytracing visualization)
    if (showSensors && vehicle.activeRays.isNotEmpty()) {
        for (ray in vehicle.activeRays) {
            val startScreen = Offset(ray.startX + camOffsetX, ray.startY + camOffsetY)
            val endScreen = Offset(ray.hitX + camOffsetX, ray.hitY + camOffsetY)

            val rayColor = if (ray.isHit) {
                if (ray.hitType?.isHazard == true) Color(0xFFFF3366) else Color(0xFF00E676)
            } else {
                Color(0x7700F0FF)
            }

            // Sensor laser beam
            drawLine(
                color = rayColor,
                start = startScreen,
                end = endScreen,
                strokeWidth = if (ray.isHit) 3f else 1.5f
            )

            // Hit contact circle / spark
            if (ray.isHit) {
                drawCircle(
                    color = rayColor,
                    radius = 6f,
                    center = endScreen
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5f,
                    center = endScreen
                )
            }
        }
    }

    // 2. Draw Car Chassis with Heading Rotation
    rotate(degrees = vehicle.headingAngleDeg, pivot = carCenter) {
        val halfW = vehicle.carWidth / 2f
        val halfL = vehicle.carLength / 2f

        val primaryColor = Color(vehicle.model.primaryColorHex)
        val accentColor = Color(vehicle.model.accentColorHex)
        val glowColor = Color(vehicle.model.glowColorHex)

        // Underglow neon shadow
        drawRoundRect(
            color = glowColor.copy(alpha = 0.45f),
            topLeft = Offset(carScreenX - halfW - 8f, carScreenY - halfL - 6f),
            size = Size(vehicle.carWidth + 16f, vehicle.carLength + 12f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
        )

        // 4 Wheels with steering angle on front wheels
        val wheelW = 9f
        val wheelL = 20f

        // Rear wheels
        drawRoundRect(
            color = Color(0xFF1E2430),
            topLeft = Offset(carScreenX - halfW - 2f, carScreenY + halfL * 0.45f - wheelL / 2f),
            size = Size(wheelW, wheelL),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
        )
        drawRoundRect(
            color = Color(0xFF1E2430),
            topLeft = Offset(carScreenX + halfW - wheelW + 2f, carScreenY + halfL * 0.45f - wheelL / 2f),
            size = Size(wheelW, wheelL),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
        )

        // Front wheels (steered)
        val frontWheelLeftCenter = Offset(carScreenX - halfW + 3f, carScreenY - halfL * 0.5f)
        val frontWheelRightCenter = Offset(carScreenX + halfW - 3f, carScreenY - halfL * 0.5f)

        rotate(degrees = vehicle.steerAngleDeg, pivot = frontWheelLeftCenter) {
            drawRoundRect(
                color = Color(0xFF1E2430),
                topLeft = Offset(frontWheelLeftCenter.x - wheelW / 2f, frontWheelLeftCenter.y - wheelL / 2f),
                size = Size(wheelW, wheelL),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
            )
        }
        rotate(degrees = vehicle.steerAngleDeg, pivot = frontWheelRightCenter) {
            drawRoundRect(
                color = Color(0xFF1E2430),
                topLeft = Offset(frontWheelRightCenter.x - wheelW / 2f, frontWheelRightCenter.y - wheelL / 2f),
                size = Size(wheelW, wheelL),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
            )
        }

        // Main Car Body
        drawRoundRect(
            color = primaryColor,
            topLeft = Offset(carScreenX - halfW, carScreenY - halfL),
            size = Size(vehicle.carWidth, vehicle.carLength),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
        )

        // Cabin / Roof
        drawRoundRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(carScreenX - halfW * 0.7f, carScreenY - halfL * 0.4f),
            size = Size(vehicle.carWidth * 0.7f, vehicle.carLength * 0.55f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
        )

        // Windshield and rear glass
        drawRoundRect(
            color = Color(0xFF00F0FF).copy(alpha = 0.7f),
            topLeft = Offset(carScreenX - halfW * 0.6f, carScreenY - halfL * 0.35f),
            size = Size(vehicle.carWidth * 0.6f, 14f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
        )

        // Hood racing stripes / accents
        drawLine(
            color = accentColor,
            start = Offset(carScreenX - 4f, carScreenY - halfL),
            end = Offset(carScreenX - 4f, carScreenY - halfL * 0.4f),
            strokeWidth = 3f
        )
        drawLine(
            color = accentColor,
            start = Offset(carScreenX + 4f, carScreenY - halfL),
            end = Offset(carScreenX + 4f, carScreenY - halfL * 0.4f),
            strokeWidth = 3f
        )

        // Front Headlights / LiDAR array
        drawCircle(
            color = Color(0xFF00F0FF),
            radius = 4f,
            center = Offset(carScreenX - halfW * 0.7f, carScreenY - halfL + 3f)
        )
        drawCircle(
            color = Color(0xFF00F0FF),
            radius = 4f,
            center = Offset(carScreenX + halfW * 0.7f, carScreenY - halfL + 3f)
        )
        // Center LiDAR emitter
        drawCircle(
            color = Color(0xFFFF5722),
            radius = 3f,
            center = Offset(carScreenX, carScreenY - halfL + 2f)
        )

        // Rear Taillights
        drawRect(
            color = Color(0xFFFF3366),
            topLeft = Offset(carScreenX - halfW * 0.8f, carScreenY + halfL - 4f),
            size = Size(10f, 4f)
        )
        drawRect(
            color = Color(0xFFFF3366),
            topLeft = Offset(carScreenX + halfW * 0.8f - 10f, carScreenY + halfL - 4f),
            size = Size(10f, 4f)
        )

        // Rear Spoiler wing
        drawRoundRect(
            color = accentColor,
            topLeft = Offset(carScreenX - halfW * 0.95f, carScreenY + halfL * 0.75f),
            size = Size(vehicle.carWidth * 0.95f, 6f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
        )
    }
}
