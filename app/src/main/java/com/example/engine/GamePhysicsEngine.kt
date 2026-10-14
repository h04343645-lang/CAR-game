package com.example.engine

import com.example.data.*
import kotlin.math.*

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float, // 1.0 down to 0.0
    val maxLife: Float,
    val colorHex: Long,
    val size: Float
)

data class VehicleState(
    val id: String,
    val isPlayer: Boolean,
    var x: Float,
    var y: Float,
    var speed: Float = 0f, // in pixels/sec
    var headingAngleDeg: Float = 0f, // 0 = straight up (-Y direction)
    var steerAngleDeg: Float = 0f,
    var throttle: Float = 0f, // 0..1
    var brake: Float = 0f, // 0..1
    var nitroActive: Boolean = false,
    var nitroFuel: Float = 100f,
    var health: Float = 100f,
    var isDestroyed: Boolean = false,
    var currentWaypointIdx: Int = 0,
    var lapsCompleted: Int = 0,
    var carWidth: Float = 42f,
    var carLength: Float = 76f,
    var activeRays: List<RaycastResult> = emptyList(),
    var aiDecision: String = "Cruising",
    val model: CarModel
)

class GamePhysicsEngine {

    val particles = mutableListOf<Particle>()

    fun updateVehicle(
        vehicle: VehicleState,
        dt: Float,
        adminSettings: AdminSettings,
        waypoints: List<Waypoint>,
        obstacles: MutableList<ObstacleEntity>,
        trackMinX: Float,
        trackMaxX: Float,
        onCollision: (ObstacleType) -> Unit,
        onCollectCoin: () -> Unit,
        onCollectNitro: () -> Unit
    ) {
        if (vehicle.isDestroyed) return

        val speedMultiplier = if (vehicle.isPlayer) adminSettings.speedMultiplier else 1.0f
        val accelMultiplier = if (vehicle.isPlayer) adminSettings.accelMultiplier else 1.0f
        val maxSpeed = (vehicle.model.baseTopSpeed * 2.2f) * speedMultiplier
        val baseAccel = (vehicle.model.baseAcceleration * 180f) * accelMultiplier
        val handling = vehicle.model.baseHandling

        // 1. Calculate Raycasts
        val sensorRange = vehicle.model.baseSensorRange * adminSettings.sensorRangeMultiplier
        val rays = RaycastSensorEngine.castRays(
            carX = vehicle.x,
            carY = vehicle.y,
            carAngleDeg = vehicle.headingAngleDeg,
            carWidth = vehicle.carWidth,
            carLength = vehicle.carLength,
            baseSensorRange = sensorRange,
            rayCount = adminSettings.sensorRayCount,
            spreadAngleDeg = adminSettings.sensorSpreadAngle,
            obstacles = obstacles,
            trackMinX = trackMinX,
            trackMaxX = trackMaxX
        )
        vehicle.activeRays = rays

        // 2. Autonomous AI Logic (if not player or if AI Copilot assist is active)
        if (!vehicle.isPlayer) {
            runAiLogic(vehicle, waypoints, rays, adminSettings)
        } else if (adminSettings.autoAvoidAssistInPlayerMode && vehicle.throttle > 0.1f) {
            // Player Copilot assist: gently correct steering if imminent collision
            val (aiSteer, aiBrake) = RaycastSensorEngine.computeAvoidance(
                rays,
                adminSettings.steerAvoidanceSensitivity * 0.7f
            )
            if (abs(aiSteer) > 8f) {
                vehicle.steerAngleDeg = (vehicle.steerAngleDeg * 0.4f) + (aiSteer * 0.6f)
                if (aiBrake > 0.4f) vehicle.brake = max(vehicle.brake, aiBrake * 0.5f)
            }
        }

        // 3. Nitro Handling
        var nitroBoost = 1.0f
        if (vehicle.nitroActive && (vehicle.nitroFuel > 0f || adminSettings.infiniteNitro)) {
            nitroBoost = 1.65f
            if (!adminSettings.infiniteNitro) {
                vehicle.nitroFuel = max(0f, vehicle.nitroFuel - dt * 25f)
            }
            // Spawn nitro flame particles behind bumper
            val headingRad = Math.toRadians(vehicle.headingAngleDeg.toDouble()).toFloat()
            val backX = vehicle.x - sin(headingRad) * (vehicle.carLength * 0.55f)
            val backY = vehicle.y + cos(headingRad) * (vehicle.carLength * 0.55f)
            particles.add(
                Particle(
                    x = backX + (sin(backY) * 6f),
                    y = backY,
                    vx = -sin(headingRad) * 60f + (kotlin.random.Random.nextFloat() - 0.5f) * 20f,
                    vy = cos(headingRad) * 60f + (kotlin.random.Random.nextFloat() - 0.5f) * 20f,
                    life = 0.35f,
                    maxLife = 0.35f,
                    colorHex = 0xFF00F0FF,
                    size = 7f
                )
            )
        } else if (!vehicle.nitroActive && vehicle.nitroFuel < 100f) {
            vehicle.nitroFuel = min(100f, vehicle.nitroFuel + dt * 4f)
        }

        // 4. Acceleration and Braking
        val effectiveMaxSpeed = maxSpeed * nitroBoost
        if (vehicle.throttle > 0f) {
            val forwardForce = vehicle.throttle * baseAccel * (1f - (vehicle.speed / effectiveMaxSpeed).coerceIn(0f, 1f))
            vehicle.speed = min(effectiveMaxSpeed, vehicle.speed + forwardForce * dt)
        } else if (vehicle.brake > 0f) {
            val brakeForce = vehicle.brake * baseAccel * 1.8f
            vehicle.speed = max(-80f, vehicle.speed - brakeForce * dt)
        } else {
            // Natural rolling resistance & aerodynamic drag
            val drag = vehicle.speed * 0.85f * dt
            if (abs(vehicle.speed) > 2f) {
                vehicle.speed -= sign(vehicle.speed) * max(drag, 15f * dt)
            } else {
                vehicle.speed = 0f
            }
        }

        // 5. Steering & Heading update
        if (abs(vehicle.speed) > 5f) {
            val turnSpeedFactor = (vehicle.speed / 160f).coerceIn(-1.5f, 1.5f)
            val deltaHeading = vehicle.steerAngleDeg * handling * turnSpeedFactor * dt * 2.8f
            vehicle.headingAngleDeg = (vehicle.headingAngleDeg + deltaHeading) % 360f

            // Tire skid particles when sharp steering at high speed
            if (abs(vehicle.steerAngleDeg) > 20f && abs(vehicle.speed) > 150f) {
                val headingRad = Math.toRadians(vehicle.headingAngleDeg.toDouble()).toFloat()
                val backX = vehicle.x - sin(headingRad) * (vehicle.carLength * 0.4f)
                val backY = vehicle.y + cos(headingRad) * (vehicle.carLength * 0.4f)
                particles.add(
                    Particle(
                        x = backX,
                        y = backY,
                        vx = (kotlin.random.Random.nextFloat() - 0.5f) * 15f,
                        vy = (kotlin.random.Random.nextFloat() - 0.5f) * 15f,
                        life = 0.4f,
                        maxLife = 0.4f,
                        colorHex = 0x88AAAAAA,
                        size = 5f
                    )
                )
            }
        }

        // 6. Velocity vector and position integration
        val rad = Math.toRadians(vehicle.headingAngleDeg.toDouble()).toFloat()
        val moveX = sin(rad) * vehicle.speed * dt
        val moveY = -cos(rad) * vehicle.speed * dt
        vehicle.x += moveX
        vehicle.y += moveY

        // 7. Track boundary collision
        if (!adminSettings.noWallCollisions || !vehicle.isPlayer) {
            val halfW = vehicle.carWidth * 0.6f
            if (vehicle.x - halfW < trackMinX) {
                vehicle.x = trackMinX + halfW
                vehicle.speed *= 0.7f
                spawnCrashSparks(vehicle.x, vehicle.y)
            } else if (vehicle.x + halfW > trackMaxX) {
                vehicle.x = trackMaxX - halfW
                vehicle.speed *= 0.7f
                spawnCrashSparks(vehicle.x, vehicle.y)
            }
        }

        // 8. Obstacle & Collectible Collisions
        val carHitRadius = vehicle.carWidth * 0.7f
        val iterator = obstacles.iterator()
        while (iterator.hasNext()) {
            val obs = iterator.next()
            if (obs.isHit) continue

            val dx = vehicle.x - obs.x
            val dy = vehicle.y - obs.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < carHitRadius + obs.radius) {
                // Collided!
                when (obs.type) {
                    ObstacleType.COIN -> {
                        obs.isHit = true
                        onCollectCoin()
                        iterator.remove()
                    }
                    ObstacleType.NITRO_CANISTER -> {
                        obs.isHit = true
                        vehicle.nitroFuel = 100f
                        onCollectNitro()
                        iterator.remove()
                    }
                    ObstacleType.OIL_SLICK -> {
                        // Loss of traction, spin out slightly
                        vehicle.steerAngleDeg += if (vehicle.steerAngleDeg >= 0) 30f else -30f
                        vehicle.speed *= 0.8f
                        onCollision(obs.type)
                    }
                    ObstacleType.BARRIER, ObstacleType.ROCK, ObstacleType.TRAFFIC_CAR -> {
                        obs.isHit = true
                        spawnCrashSparks(vehicle.x, vehicle.y)
                        onCollision(obs.type)

                        if (!adminSettings.godMode || !vehicle.isPlayer) {
                            vehicle.health = max(0f, vehicle.health - 25f)
                            vehicle.speed *= 0.4f
                            if (vehicle.health <= 0f) {
                                vehicle.isDestroyed = true
                            }
                        } else {
                            // God Mode bounce back hazard
                            obs.vx = sin(rad) * 400f
                            obs.vy = -cos(rad) * 400f
                        }
                    }
                }
            }
        }

        // 9. Update Waypoint progression
        if (waypoints.isNotEmpty()) {
            val targetWp = waypoints[vehicle.currentWaypointIdx]
            val dwx = targetWp.x - vehicle.x
            val dwy = targetWp.y - vehicle.y
            val distToWp = sqrt(dwx * dwx + dwy * dwy)
            if (distToWp < 140f) {
                vehicle.currentWaypointIdx = (vehicle.currentWaypointIdx + 1) % waypoints.size
                if (vehicle.currentWaypointIdx == 0) {
                    vehicle.lapsCompleted++
                }
            }
        }
    }

    private fun runAiLogic(
        vehicle: VehicleState,
        waypoints: List<Waypoint>,
        rays: List<RaycastResult>,
        adminSettings: AdminSettings
    ) {
        if (waypoints.isEmpty()) return

        // 1. Calculate ideal steering toward next waypoint
        val targetWp = waypoints[vehicle.currentWaypointIdx]
        val dx = targetWp.x - vehicle.x
        val dy = targetWp.y - vehicle.y

        // Angle to waypoint: 0 deg is -Y direction
        var desiredAngleDeg = Math.toDegrees(atan2(dx.toDouble(), -dy.toDouble())).toFloat()
        if (desiredAngleDeg < 0) desiredAngleDeg += 360f

        var headingDiff = desiredAngleDeg - vehicle.headingAngleDeg
        while (headingDiff > 180f) headingDiff -= 360f
        while (headingDiff < -180f) headingDiff += 360f

        // Proportional steering to waypoint
        val waypointSteer = (headingDiff * 0.8f).coerceIn(-35f, 35f)

        // 2. Compute Raycast Avoidance Steering & Braking
        val (avoidSteer, avoidBrake) = RaycastSensorEngine.computeAvoidance(
            rays,
            adminSettings.steerAvoidanceSensitivity
        )

        // 3. Blend avoidance with path steering
        if (abs(avoidSteer) > 5f) {
            // Prioritize obstacle avoidance
            vehicle.steerAngleDeg = avoidSteer
            vehicle.aiDecision = if (avoidSteer < 0) "Avoid Hazard (Steer Left)" else "Avoid Hazard (Steer Right)"
        } else {
            vehicle.steerAngleDeg = waypointSteer
            vehicle.aiDecision = "Navigating to WP #${vehicle.currentWaypointIdx + 1}"
        }

        // 4. Throttle & Brake
        val targetSpeed = targetWp.targetSpeed * (if (abs(headingDiff) > 30f) 0.65f else 1.0f)
        if (avoidBrake > 0.2f) {
            vehicle.throttle = 0.1f
            vehicle.brake = avoidBrake
            vehicle.aiDecision += " [Braking]"
        } else if (vehicle.speed < targetSpeed) {
            vehicle.throttle = 1.0f
            vehicle.brake = 0f
            // Trigger nitro on clear straights
            if (abs(headingDiff) < 8f && vehicle.speed > 160f && vehicle.nitroFuel > 60f) {
                vehicle.nitroActive = true
                vehicle.aiDecision += " [Nitro Engaged]"
            } else {
                vehicle.nitroActive = false
            }
        } else {
            vehicle.throttle = 0.3f
            vehicle.brake = 0f
            vehicle.nitroActive = false
        }
    }

    fun updateObstacles(obstacles: List<ObstacleEntity>, dt: Float) {
        for (obs in obstacles) {
            obs.x += obs.vx * dt
            obs.y += obs.vy * dt
            // Decelerate launched obstacles
            obs.vx *= (1f - dt * 1.5f)
            obs.vy *= (1f - dt * 1.5f)
        }
    }

    fun updateParticles(dt: Float) {
        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.life -= dt
            if (p.life <= 0f) {
                it.remove()
            }
        }
    }

    private fun spawnCrashSparks(x: Float, y: Float) {
        val rand = kotlin.random.Random
        for (i in 0..12) {
            val angle = rand.nextFloat() * 2f * Math.PI.toFloat()
            val spd = 60f + rand.nextFloat() * 120f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * spd,
                    vy = sin(angle) * spd,
                    life = 0.35f,
                    maxLife = 0.35f,
                    colorHex = if (rand.nextBoolean()) 0xFFFFD600 else 0xFFFF5722,
                    size = 5f
                )
            )
        }
    }
}
