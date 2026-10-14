package com.example.engine

import com.example.data.ObstacleEntity
import com.example.data.ObstacleType
import com.example.data.TrackType
import com.example.data.Waypoint
import kotlin.math.*
import kotlin.random.Random

object TrackManager {

    /**
     * Generates closed-loop circuit waypoints for the autonomous AI car to navigate.
     */
    fun createCircuitWaypoints(trackType: TrackType): List<Waypoint> {
        return when (trackType) {
            TrackType.CYBER_CIRCUIT -> {
                // Sleek Grand Prix circuit with 16 waypoints
                listOf(
                    Waypoint(0f, 0f, 260f),
                    Waypoint(0f, -400f, 280f),
                    Waypoint(50f, -800f, 260f),
                    Waypoint(200f, -1100f, 200f), // Turn 1
                    Waypoint(450f, -1150f, 180f),
                    Waypoint(700f, -950f, 210f),
                    Waypoint(800f, -600f, 250f),
                    Waypoint(750f, -200f, 260f),
                    Waypoint(550f, 150f, 220f),
                    Waypoint(300f, 350f, 200f),  // Chicane
                    Waypoint(350f, 650f, 220f),
                    Waypoint(250f, 950f, 240f),
                    Waypoint(50f, 1200f, 210f),  // Hairpin
                    Waypoint(-250f, 1100f, 190f),
                    Waypoint(-350f, 650f, 240f),
                    Waypoint(-250f, 250f, 250f)
                )
            }
            TrackType.NEON_HIGHWAY -> {
                // Long highway sprint with sweeping curves
                val pts = mutableListOf<Waypoint>()
                for (i in 0..20) {
                    val y = -i * 300f
                    val x = sin(i * 0.45f) * 120f
                    pts.add(Waypoint(x, y, 300f))
                }
                pts
            }
            TrackType.TEST_SANDBOX -> {
                // Figure-8 testing arena for sensor raycasting
                val pts = mutableListOf<Waypoint>()
                val count = 24
                for (i in 0 until count) {
                    val t = (i.toFloat() / count) * 2f * Math.PI.toFloat()
                    val x = sin(t) * 350f
                    val y = sin(2f * t) * 500f
                    pts.add(Waypoint(x, y, 220f))
                }
                pts
            }
        }
    }

    /**
     * Spawns obstacles, road hazards, traffic vehicles, and collectibles.
     */
    fun generateInitialObstacles(
        trackType: TrackType,
        densityMultiplier: Float
    ): MutableList<ObstacleEntity> {
        val list = mutableListOf<ObstacleEntity>()
        var idCounter = 1L
        val random = Random(42)

        when (trackType) {
            TrackType.CYBER_CIRCUIT -> {
                val waypoints = createCircuitWaypoints(trackType)
                for (i in 1 until waypoints.size) {
                    val wp = waypoints[i]
                    // Chance to spawn barrier, oil, or traffic
                    val roll = random.nextFloat()
                    val lateralOffset = (random.nextFloat() - 0.5f) * 180f
                    if (roll < 0.35f * densityMultiplier) {
                        list.add(
                            ObstacleEntity(
                                id = idCounter++,
                                x = wp.x + lateralOffset,
                                y = wp.y + (random.nextFloat() - 0.5f) * 80f,
                                radius = ObstacleType.BARRIER.radius,
                                type = ObstacleType.BARRIER
                            )
                        )
                    } else if (roll < 0.55f * densityMultiplier) {
                        list.add(
                            ObstacleEntity(
                                id = idCounter++,
                                x = wp.x + lateralOffset,
                                y = wp.y,
                                radius = ObstacleType.OIL_SLICK.radius,
                                type = ObstacleType.OIL_SLICK
                            )
                        )
                    } else if (roll < 0.75f * densityMultiplier) {
                        list.add(
                            ObstacleEntity(
                                id = idCounter++,
                                x = wp.x + lateralOffset,
                                y = wp.y,
                                radius = ObstacleType.TRAFFIC_CAR.radius,
                                type = ObstacleType.TRAFFIC_CAR,
                                vy = -40f - random.nextFloat() * 40f
                            )
                        )
                    }

                    // Add collectibles (coins and nitro)
                    if (random.nextFloat() < 0.6f) {
                        list.add(
                            ObstacleEntity(
                                id = idCounter++,
                                x = wp.x + (random.nextFloat() - 0.5f) * 120f,
                                y = wp.y + 40f,
                                radius = ObstacleType.COIN.radius,
                                type = ObstacleType.COIN
                            )
                        )
                    }
                    if (random.nextFloat() < 0.25f) {
                        list.add(
                            ObstacleEntity(
                                id = idCounter++,
                                x = wp.x + (random.nextFloat() - 0.5f) * 100f,
                                y = wp.y + 80f,
                                radius = ObstacleType.NITRO_CANISTER.radius,
                                type = ObstacleType.NITRO_CANISTER
                            )
                        )
                    }
                }
            }
            TrackType.NEON_HIGHWAY -> {
                // Highway lanes
                val laneOffsets = listOf(-150f, -50f, 50f, 150f)
                for (step in 1..35) {
                    val y = -step * 140f
                    val lane = laneOffsets[random.nextInt(laneOffsets.size)]
                    val roll = random.nextFloat()
                    if (roll < 0.4f * densityMultiplier) {
                        list.add(
                            ObstacleEntity(
                                id = idCounter++,
                                x = lane + (random.nextFloat() - 0.5f) * 20f,
                                y = y,
                                radius = ObstacleType.TRAFFIC_CAR.radius,
                                type = ObstacleType.TRAFFIC_CAR,
                                vy = -50f - random.nextFloat() * 30f
                            )
                        )
                    } else if (roll < 0.65f * densityMultiplier) {
                        list.add(
                            ObstacleEntity(
                                id = idCounter++,
                                x = lane,
                                y = y,
                                radius = ObstacleType.BARRIER.radius,
                                type = ObstacleType.BARRIER
                            )
                        )
                    }

                    // Coins
                    if (random.nextFloat() < 0.5f) {
                        val coinLane = laneOffsets[random.nextInt(laneOffsets.size)]
                        list.add(
                            ObstacleEntity(
                                id = idCounter++,
                                x = coinLane,
                                y = y + 50f,
                                radius = ObstacleType.COIN.radius,
                                type = ObstacleType.COIN
                            )
                        )
                    }
                }
            }
            TrackType.TEST_SANDBOX -> {
                // Obstacle slalom course in the lab arena
                val xOffsets = listOf(-220f, -110f, 0f, 110f, 220f)
                for (row in 0..15) {
                    val y = -row * 120f
                    val pickX = xOffsets[(row * 2 + 1) % xOffsets.size]
                    list.add(
                        ObstacleEntity(
                            id = idCounter++,
                            x = pickX,
                            y = y,
                            radius = ObstacleType.BARRIER.radius,
                            type = ObstacleType.BARRIER
                        )
                    )
                    // Also spawn a few moving drones
                    if (row % 3 == 0) {
                        list.add(
                            ObstacleEntity(
                                id = idCounter++,
                                x = -pickX,
                                y = y,
                                radius = ObstacleType.TRAFFIC_CAR.radius,
                                type = ObstacleType.TRAFFIC_CAR,
                                vx = if (pickX > 0) -25f else 25f
                            )
                        )
                    }
                }
            }
        }
        return list
    }
}
