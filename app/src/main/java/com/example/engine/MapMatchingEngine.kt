package com.example.engine

import com.example.model.RoadNode
import com.example.model.RoadSegment
import kotlin.math.*

/**
 * Map-Matching Engine (ISRO NavDR)
 *
 * Implements Geometric and Topological Map Matching:
 * 1. Offline vector road network overlay.
 * 2. Point-to-Polyline orthogonal projection.
 * 3. Heading alignment weighting & corridor snap constraint during GNSS dropouts.
 */
class MapMatchingEngine {

    private val roadSegments = mutableListOf<RoadSegment>()

    fun setRoadNetwork(segments: List<RoadSegment>) {
        roadSegments.clear()
        roadSegments.addAll(segments)
    }

    data class MatchResult(
        val snappedLat: Double,
        val snappedLng: Double,
        val matchedRoadName: String,
        val orthogonalDistanceMeters: Float,
        val headingDeltaDeg: Float,
        val isSnapped: Boolean
    )

    /**
     * Matches raw dead reckoning position (lat, lng, heading) to the nearest road network segment
     */
    fun matchPosition(
        lat: Double,
        lng: Double,
        headingDeg: Float,
        maxSnapDistanceMeters: Float = 25f
    ): MatchResult {
        if (roadSegments.isEmpty()) {
            return MatchResult(lat, lng, "Off-Grid Highway", 0f, 0f, false)
        }

        var closestSegment: RoadSegment? = null
        var minDistance = Float.MAX_VALUE
        var bestSnappedLat = lat
        var bestSnappedLng = lng
        var bestHeadingDelta = 0f

        for (segment in roadSegments) {
            val (projLat, projLng, dist) = projectPointToSegment(
                lat, lng,
                segment.startNode.lat, segment.startNode.lng,
                segment.endNode.lat, segment.endNode.lng
            )

            val segBearing = segment.bearingDeg
            val diffBearing = angleDifference(headingDeg, segBearing)

            // Weight distance and heading alignment
            val headingPenalty = if (diffBearing > 45f && diffBearing < 135f) 20f else 0f
            val totalCost = dist + headingPenalty

            if (totalCost < minDistance) {
                minDistance = totalCost
                closestSegment = segment
                bestSnappedLat = projLat
                bestSnappedLng = projLng
                bestHeadingDelta = diffBearing
            }
        }

        val isSnapped = minDistance <= maxSnapDistanceMeters && closestSegment != null

        return MatchResult(
            snappedLat = if (isSnapped) bestSnappedLat else lat,
            snappedLng = if (isSnapped) bestSnappedLng else lng,
            matchedRoadName = closestSegment?.name ?: "Unknown Route",
            orthogonalDistanceMeters = minDistance,
            headingDeltaDeg = bestHeadingDelta,
            isSnapped = isSnapped
        )
    }

    private fun projectPointToSegment(
        pLat: Double, pLng: Double,
        sLat: Double, sLng: Double,
        eLat: Double, eLng: Double
    ): Triple<Double, Double, Float> {
        // Approximate planar projection for local meters
        val latMid = (sLat + eLat) / 2.0
        val cosLat = cos(Math.toRadians(latMid))
        val metersPerDegLat = 111132.954
        val metersPerDegLng = 111132.954 * cosLat

        val px = (pLng - sLng) * metersPerDegLng
        val py = (pLat - sLat) * metersPerDegLat

        val dx = (eLng - sLng) * metersPerDegLng
        val dy = (eLat - sLat) * metersPerDegLat

        val segLengthSq = dx * dx + dy * dy
        if (segLengthSq < 1e-6) {
            val dist = sqrt(px * px + py * py).toFloat()
            return Triple(sLat, sLng, dist)
        }

        // Projection fraction t along segment
        val t = ((px * dx + py * dy) / segLengthSq).coerceIn(0.0, 1.0)

        val projLng = sLng + t * (eLng - sLng)
        val projLat = sLat + t * (eLat - sLat)

        val projX = (pLng - projLng) * metersPerDegLng
        val projY = (pLat - projLat) * metersPerDegLat
        val dist = sqrt(projX * projX + projY * projY).toFloat()

        return Triple(projLat, projLng, dist)
    }

    private fun angleDifference(a1: Float, a2: Float): Float {
        var diff = (a1 - a2) % 360f
        if (diff < -180f) diff += 360f
        if (diff > 180f) diff -= 360f
        return abs(diff)
    }
}
