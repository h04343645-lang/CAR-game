package com.example.engine

import com.example.data.ObstacleEntity
import com.example.data.RaycastResult
import kotlin.math.*

object RaycastSensorEngine {

    /**
     * Casts rays from the car's front bumper and checks for intersections
     * with obstacles and track boundaries.
     */
    fun castRays(
        carX: Float,
        carY: Float,
        carAngleDeg: Float,
        carWidth: Float,
        carLength: Float,
        baseSensorRange: Float,
        rayCount: Int,
        spreadAngleDeg: Float,
        obstacles: List<ObstacleEntity>,
        trackMinX: Float,
        trackMaxX: Float
    ): List<RaycastResult> {
        val carHeadingRad = Math.toRadians(carAngleDeg.toDouble()).toFloat()
        val forwardX = sin(carHeadingRad)
        val forwardY = -cos(carHeadingRad)
        val rightX = cos(carHeadingRad)
        val rightY = sin(carHeadingRad)

        // Front bumper center origin
        val bumperX = carX + forwardX * (carLength * 0.5f)
        val bumperY = carY + forwardY * (carLength * 0.5f)

        val results = mutableListOf<RaycastResult>()

        // Generate ray angles based on rayCount (e.g. 3, 5, 7)
        val angles = when (rayCount) {
            3 -> listOf(-spreadAngleDeg * 0.7f, 0f, spreadAngleDeg * 0.7f)
            7 -> listOf(
                -spreadAngleDeg * 1.3f, -spreadAngleDeg * 0.8f, -spreadAngleDeg * 0.35f,
                0f,
                spreadAngleDeg * 0.35f, spreadAngleDeg * 0.8f, spreadAngleDeg * 1.3f
            )
            else -> listOf(
                -spreadAngleDeg, -spreadAngleDeg * 0.5f,
                0f,
                spreadAngleDeg * 0.5f, spreadAngleDeg
            )
        }

        val labels = angles.map { angle ->
            when {
                angle < -15f -> "Left Angled"
                angle < -0.1f -> "Left Center"
                angle > 15f -> "Right Angled"
                angle > 0.1f -> "Right Center"
                else -> "Front Center"
            }
        }

        for (i in angles.indices) {
            val angleOffsetDeg = angles[i]
            val rayAngleRad = Math.toRadians((carAngleDeg + angleOffsetDeg).toDouble()).toFloat()
            val rayDirX = sin(rayAngleRad)
            val rayDirY = -cos(rayAngleRad)

            // Ray start offset (distribute along bumper width)
            val lateralOffsetFactor = when {
                angleOffsetDeg < -1f -> -0.4f
                angleOffsetDeg > 1f -> 0.4f
                else -> 0f
            }
            val startX = bumperX + rightX * (carWidth * lateralOffsetFactor)
            val startY = bumperY + rightY * (carWidth * lateralOffsetFactor)

            val maxRange = baseSensorRange
            val defaultEndX = startX + rayDirX * maxRange
            val defaultEndY = startY + rayDirY * maxRange

            var closestHitDist = maxRange
            var hitX = defaultEndX
            var hitY = defaultEndY
            var isHit = false
            var hitType: com.example.data.ObstacleType? = null

            // 1. Check intersection with obstacles (circle raycast)
            for (obs in obstacles) {
                if (obs.isHit) continue
                val hit = intersectRayCircle(
                    startX, startY, rayDirX, rayDirY,
                    obs.x, obs.y, obs.radius + 6f
                )
                if (hit != null && hit > 0f && hit < closestHitDist) {
                    closestHitDist = hit
                    hitX = startX + rayDirX * hit
                    hitY = startY + rayDirY * hit
                    isHit = true
                    hitType = obs.type
                }
            }

            // 2. Check intersection with track walls
            if (rayDirX < -0.001f) {
                val distToLeftWall = (trackMinX - startX) / rayDirX
                if (distToLeftWall in 0f..closestHitDist) {
                    closestHitDist = distToLeftWall
                    hitX = trackMinX
                    hitY = startY + rayDirY * distToLeftWall
                    isHit = true
                    hitType = com.example.data.ObstacleType.BARRIER
                }
            } else if (rayDirX > 0.001f) {
                val distToRightWall = (trackMaxX - startX) / rayDirX
                if (distToRightWall in 0f..closestHitDist) {
                    closestHitDist = distToRightWall
                    hitX = trackMaxX
                    hitY = startY + rayDirY * distToRightWall
                    isHit = true
                    hitType = com.example.data.ObstacleType.BARRIER
                }
            }

            results.add(
                RaycastResult(
                    index = i,
                    label = labels[i],
                    angleDegrees = angleOffsetDeg,
                    startX = startX,
                    startY = startY,
                    endX = defaultEndX,
                    endY = defaultEndY,
                    hitX = hitX,
                    hitY = hitY,
                    hitDistance = closestHitDist,
                    maxRange = maxRange,
                    isHit = isHit,
                    hitType = hitType
                )
            )
        }

        return results
    }

    /**
     * Computes the steering avoidance angle and recommended brake factor
     * based on the raycast sensor results (matching Unity CarEngine.cs).
     */
    fun computeAvoidance(
        rays: List<RaycastResult>,
        avoidanceSensitivity: Float
    ): Pair<Float, Float> {
        var steerDeflection = 0f
        var brakeFactor = 0f

        val centerRay = rays.find { abs(it.angleDegrees) < 0.1f }
        val leftRays = rays.filter { it.angleDegrees < -0.1f }
        val rightRays = rays.filter { it.angleDegrees > 0.1f }

        var leftWeight = 0f
        for (r in leftRays) {
            if (r.isHit && (r.hitType?.isHazard == true)) {
                val urgency = (1f - (r.hitDistance / r.maxRange)).coerceIn(0f, 1f)
                leftWeight += urgency * urgency
            }
        }

        var rightWeight = 0f
        for (r in rightRays) {
            if (r.isHit && (r.hitType?.isHazard == true)) {
                val urgency = (1f - (r.hitDistance / r.maxRange)).coerceIn(0f, 1f)
                rightWeight += urgency * urgency
            }
        }

        // Steer away from detected hazards:
        // Detected on left -> steer right (positive angle)
        // Detected on right -> steer left (negative angle)
        steerDeflection += (leftWeight - rightWeight) * 35f * avoidanceSensitivity

        // If center ray detects an imminent hazard
        if (centerRay != null && centerRay.isHit && centerRay.hitType?.isHazard == true) {
            val urgency = (1f - (centerRay.hitDistance / centerRay.maxRange)).coerceIn(0f, 1f)
            // Apply braking
            brakeFactor = urgency * urgency * 0.9f

            // Bias steering to whichever side has clearer rays
            if (abs(steerDeflection) < 4f) {
                steerDeflection = if (leftWeight <= rightWeight) -28f else 28f
            }
        }

        return Pair(steerDeflection.coerceIn(-45f, 45f), brakeFactor.coerceIn(0f, 1f))
    }

    private fun intersectRayCircle(
        rx: Float, ry: Float,
        dx: Float, dy: Float,
        cx: Float, cy: Float,
        radius: Float
    ): Float? {
        val fx = rx - cx
        val fy = ry - cy

        val a = dx * dx + dy * dy
        val b = 2f * (fx * dx + fy * dy)
        val c = (fx * fx + fy * fy) - radius * radius

        val discriminant = b * b - 4f * a * c
        if (discriminant < 0) return null

        val sqrtDisc = sqrt(discriminant)
        val t1 = (-b - sqrtDisc) / (2f * a)
        val t2 = (-b + sqrtDisc) / (2f * a)

        return when {
            t1 > 0f -> t1
            t2 > 0f -> t2
            else -> null
        }
    }
}
