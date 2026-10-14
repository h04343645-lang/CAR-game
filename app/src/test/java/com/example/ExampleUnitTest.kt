package com.example

import com.example.data.ObstacleEntity
import com.example.data.ObstacleType
import com.example.engine.RaycastSensorEngine
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testRaycastSensorClearTrack() {
        val rays = RaycastSensorEngine.castRays(
            carX = 0f,
            carY = 0f,
            carAngleDeg = 0f,
            carWidth = 40f,
            carLength = 70f,
            baseSensorRange = 200f,
            rayCount = 5,
            spreadAngleDeg = 36f,
            obstacles = emptyList(),
            trackMinX = -250f,
            trackMaxX = 250f
        )

        assertEquals(5, rays.size)
        // Center ray should not be hit on clear straight
        val centerRay = rays.find { kotlin.math.abs(it.angleDegrees) < 0.1f }
        assertNotNull(centerRay)
        assertFalse(centerRay!!.isHit)
    }

    @Test
    fun testRaycastSensorObstacleDetectionAndAvoidance() {
        val obstacles = listOf(
            ObstacleEntity(
                id = 1,
                x = 15f,
                y = -80f, // Directly ahead slightly right
                radius = 25f,
                type = ObstacleType.BARRIER
            )
        )

        val rays = RaycastSensorEngine.castRays(
            carX = 0f,
            carY = 0f,
            carAngleDeg = 0f,
            carWidth = 40f,
            carLength = 70f,
            baseSensorRange = 200f,
            rayCount = 5,
            spreadAngleDeg = 36f,
            obstacles = obstacles,
            trackMinX = -250f,
            trackMaxX = 250f
        )

        // At least one ray should register the hazard
        val hitRays = rays.filter { it.isHit }
        assertTrue("Obstacle should be detected by LiDAR rays", hitRays.isNotEmpty())

        val (steer, brake) = RaycastSensorEngine.computeAvoidance(rays, 1.5f)
        // Obstacle is on the right -> AI must steer left (negative angle) or apply brake
        assertTrue("AI must steer or brake to avoid right-side hazard", steer != 0f || brake > 0f)
    }
}
