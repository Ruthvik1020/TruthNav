package com.example.model

enum class FusionMode(val label: String, val description: String) {
    GNSS_AIDED("GNSS + INS AIDED", "Full satellite constellation lock (NavIC/GPS/Galileo) with active Kalman bias calibration"),
    DEAD_RECKONING_AI("AI-ML DEAD RECKONING", "Zero-GNSS outage tracking with Kinematic AI Speed + NHC + Map-Matching"),
    GNSS_OUTAGE_JAMMED("GNSS OUTAGE / JAMMED", "Instant deficit handler triggered: IMU inertial propagation active"),
    CALIBRATING("CALIBRATING MOUNT", "Estimating phone pitch/roll/yaw relative to vehicle driving direction")
}

enum class MountPreset(val displayName: String, val pitch: Float, val roll: Float, val yaw: Float) {
    DASHBOARD_HOLDER("Dashboard Mobile Holder (60° upright)", 60f, 0f, 0f),
    WINDSHIELD_MOUNT("Windshield Suction Mount (45° tilt)", 45f, 0f, 0f),
    AC_VENT_CRADLE("AC Vent Magnetic Cradle (75° upright)", 75f, -5f, 0f),
    CENTER_CONSOLE("Flat Center Console (0° horizontal)", 0f, 0f, 0f),
    CUSTOM_DYNAMIC("Auto Dynamic Alignment (Adaptive DCM)", 0f, 0f, 0f)
}

enum class TrajectoryType {
    GROUND_TRUTH,
    RAW_IMU_DR,
    AI_INS_FUSION,
    MAP_MATCHED
}

data class ImuReading(
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 9.81f,
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val magX: Float = 0f,
    val magY: Float = 0f,
    val magZ: Float = 0f,
    val timestampNs: Long = System.nanoTime()
)

data class GnssReading(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val altitude: Double = 0.0,
    val speedMps: Float = 0f,
    val bearingDeg: Float = 0f,
    val accuracyMeters: Float = 2.5f,
    val satellitesInUse: Int = 18,
    val hdop: Float = 0.8f,
    val timestampMs: Long = System.currentTimeMillis(),
    val isValid: Boolean = true
)

data class CalibrationAngles(
    val pitchDeg: Float = 60f,
    val rollDeg: Float = 0f,
    val yawDeg: Float = 0f,
    val isCalibrated: Boolean = true,
    val mountPreset: MountPreset = MountPreset.DASHBOARD_HOLDER,
    val confidence: Float = 0.95f
)

data class VehicleState(
    val lat: Double = 12.9716,
    val lng: Double = 77.5946,
    val altitude: Double = 920.0,
    val speedMps: Float = 0f,
    val headingDeg: Float = 0f,
    val pitchDeg: Float = 0f,
    val rollDeg: Float = 0f,
    val accelLongitudinal: Float = 0f,
    val accelLateral: Float = 0f,
    val accelVertical: Float = 0f,
    val yawRateDegPerSec: Float = 0f,
    val fusionMode: FusionMode = FusionMode.GNSS_AIDED,
    val accuracyMeters: Float = 1.8f,
    val isZuptActive: Boolean = false,
    val isZaruActive: Boolean = false,
    val roadSnapped: Boolean = false,
    val currentRoadName: String = "Outer Ring Road (NH 44)",
    val distanceTraveledMeters: Float = 0f,
    val timestampMs: Long = System.currentTimeMillis()
) {
    val speedKmph: Float get() = speedMps * 3.6f
}

data class TrajectoryPoint(
    val lat: Double,
    val lng: Double,
    val altitude: Double = 0.0,
    val speedKmph: Float = 0f,
    val headingDeg: Float = 0f,
    val type: TrajectoryType = TrajectoryType.AI_INS_FUSION,
    val timestampMs: Long = System.currentTimeMillis()
)

data class RoadNode(
    val id: String,
    val lat: Double,
    val lng: Double
)

data class RoadSegment(
    val id: String,
    val name: String,
    val startNode: RoadNode,
    val endNode: RoadNode,
    val speedLimitKmph: Int = 60,
    val lanes: Int = 3
) {
    val bearingDeg: Float
        get() {
            val dLng = Math.toRadians(endNode.lng - startNode.lng)
            val lat1 = Math.toRadians(startNode.lat)
            val lat2 = Math.toRadians(endNode.lat)
            val y = Math.sin(dLng) * Math.cos(lat2)
            val x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLng)
            var b = Math.toDegrees(Math.atan2(y, x)).toFloat()
            if (b < 0) b += 360f
            return b
        }
}

data class DriftMetrics(
    val currentDriftMeters: Float = 0f,
    val maxDriftMeters: Float = 0f,
    val distanceTraveledMeters: Float = 0f,
    val outageDurationSeconds: Float = 0f,
    val rawDriftMeters: Float = 0f,
    val relativeDriftPercent: Float = 0f,
    val speedRmseMps: Float = 0.28f,
    val zuptEventsCount: Int = 0,
    val benchmarkPassed: Boolean = true
)

data class BenchmarkScenario(
    val id: String,
    val title: String,
    val description: String,
    val locationName: String,
    val totalDurationSeconds: Int,
    val outageStartSec: Int,
    val outageEndSec: Int,
    val totalDistanceMeters: Float,
    val maxSpeedKmph: Float,
    val roadSegments: List<RoadSegment>,
    val groundTruthPoints: List<TrajectoryPoint>
)

data class TargetDestination(
    val lat: Double,
    val lng: Double,
    val label: String = "Destination Target",
    val timestampMs: Long = System.currentTimeMillis()
)

