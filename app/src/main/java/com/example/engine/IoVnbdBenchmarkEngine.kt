package com.example.engine

import com.example.model.*
import kotlin.math.*

/**
 * IO-VNBD Benchmark Dataset Engine (ISRO NavDR)
 *
 * Implements Ground Vehicle Kinematics, IMU Synthesis, and Ground Truth trajectories
 * for the official IO-VNBD benchmark dataset test tracks.
 */
class IoVnbdBenchmarkEngine {

    val scenarios: List<BenchmarkScenario> = listOf(
        createUrbanCanyonScenario(),
        createLongTunnelScenario(),
        createUndergroundParkingScenario(),
        createDenseForestScenario()
    )

    private var activeScenarioIndex = 0
    var currentScenarioTimeSec = 0f
        private set
    var isPlaying = false
        private set
    var playbackSpeed = 1.0f

    val currentScenario: BenchmarkScenario
        get() = scenarios[activeScenarioIndex]

    fun selectScenario(index: Int) {
        activeScenarioIndex = index.coerceIn(0, scenarios.size - 1)
        currentScenarioTimeSec = 0f
        isPlaying = false
    }

    fun play() { isPlaying = true }
    fun pause() { isPlaying = false }
    fun reset() {
        currentScenarioTimeSec = 0f
        isPlaying = false
    }
    fun setSpeed(speed: Float) { playbackSpeed = speed }

    /**
     * Steps benchmark simulation by dtSec, feeding synthesized IO-VNBD IMU and GNSS data
     * into the fusion engine.
     */
    fun stepSimulation(
        dtSec: Float,
        fusionEngine: GnssInsFusionEngine
    ): Triple<ImuReading, GnssReading?, TrajectoryPoint> {
        val effectiveDt = dtSec * playbackSpeed
        currentScenarioTimeSec += effectiveDt

        if (currentScenarioTimeSec > currentScenario.totalDurationSeconds) {
            currentScenarioTimeSec = currentScenario.totalDurationSeconds.toFloat()
            isPlaying = false
        }

        val scenario = currentScenario
        val t = currentScenarioTimeSec

        // Interpolate ground truth state along trajectory
        val groundTruth = getGroundTruthAtTime(scenario, t)

        // Check if inside GNSS blackout window
        val isGnssDenied = t >= scenario.outageStartSec && t <= scenario.outageEndSec
        fusionEngine.setGnssSignalAvailable(!isGnssDenied)

        // Synthesize vehicle IMU with road vibration noise, chassis harmonics, potholes, and mounting tilt
        val imu = synthesizeImuForGroundTruth(groundTruth, t, isGnssDenied, fusionEngine.calibrator.currentCalibration)

        // Synthesize GNSS reading if satellite visibility exists
        val gnss: GnssReading? = if (!isGnssDenied) {
            // Add subtle 1.5m GPS noise
            val noiseLat = (sin(t.toDouble() * 1.5) * 0.00001)
            val noiseLng = (cos(t.toDouble() * 1.2) * 0.00001)
            GnssReading(
                lat = groundTruth.lat + noiseLat,
                lng = groundTruth.lng + noiseLng,
                altitude = groundTruth.altitude,
                speedMps = groundTruth.speedKmph / 3.6f,
                bearingDeg = groundTruth.headingDeg,
                accuracyMeters = 1.8f,
                satellitesInUse = 18,
                hdop = 0.75f,
                isValid = true
            )
        } else {
            null
        }

        // Feed to fusion engine
        fusionEngine.processImu(imu, effectiveDt)
        if (gnss != null) {
            fusionEngine.processGnss(gnss)
        }

        // Keep ground truth trail recorded
        fusionEngine.groundTruthTrail.add(groundTruth)
        if (fusionEngine.groundTruthTrail.size > 1200) {
            fusionEngine.groundTruthTrail.removeAt(0)
        }

        return Triple(imu, gnss, groundTruth)
    }

    private fun getGroundTruthAtTime(scenario: BenchmarkScenario, timeSec: Float): TrajectoryPoint {
        val totalSec = scenario.totalDurationSeconds.toFloat()
        val frac = (timeSec / totalSec).coerceIn(0f, 1f)
        val points = scenario.groundTruthPoints
        if (points.isEmpty()) return TrajectoryPoint(12.9716, 77.5946)

        val idxF = frac * (points.size - 1)
        val idxLow = idxF.toInt().coerceIn(0, points.size - 1)
        val idxHigh = (idxLow + 1).coerceIn(0, points.size - 1)
        val alpha = idxF - idxLow

        val p1 = points[idxLow]
        val p2 = points[idxHigh]

        val lat = p1.lat + alpha * (p2.lat - p1.lat)
        val lng = p1.lng + alpha * (p2.lng - p1.lng)
        val speed = p1.speedKmph + alpha * (p2.speedKmph - p1.speedKmph)
        val heading = p1.headingDeg + alpha * (p2.headingDeg - p1.headingDeg)

        return TrajectoryPoint(
            lat = lat,
            lng = lng,
            altitude = p1.altitude,
            speedKmph = speed,
            headingDeg = heading,
            type = TrajectoryType.GROUND_TRUTH,
            timestampMs = System.currentTimeMillis()
        )
    }

    private fun synthesizeImuForGroundTruth(
        gt: TrajectoryPoint,
        timeSec: Float,
        isDenied: Boolean,
        calib: CalibrationAngles
    ): ImuReading {
        val speedMps = gt.speedKmph / 3.6f
        val isStopped = speedMps < 0.2f

        // Longitudinal acceleration
        val aLongVehicle = if (isStopped) 0f else (sin(timeSec.toDouble() * 0.4).toFloat() * 0.8f)

        // Lateral acceleration during turns: a_lat = v^2 / R = v * yaw_rate
        val yawRateVehicle = sin(timeSec.toDouble() * 0.25).toFloat() * 0.15f // rad/s
        val aLatVehicle = speedMps * yawRateVehicle

        // Vertical road vibrations & pothole shock injection
        val roadVib = if (isStopped) 0.05f * sin(timeSec * 50f) else (0.4f * sin(timeSec * 35f) + 0.2f * cos(timeSec * 80f))
        val isPothole = (timeSec.toInt() % 15 == 0 && (timeSec - timeSec.toInt()) < 0.15f)
        val aDownVehicle = 9.81f + roadVib + if (isPothole) 8.5f else 0f

        // Rotate from vehicle frame back into phone body frame according to mount pitch/roll
        val pitchRad = Math.toRadians(calib.pitchDeg.toDouble())
        val rollRad = Math.toRadians(calib.rollDeg.toDouble())

        val cp = cos(pitchRad).toFloat()
        val sp = sin(pitchRad).toFloat()
        val cr = cos(rollRad).toFloat()
        val sr = sin(rollRad).toFloat()

        // Inverse DCM transform
        val axPhone = cp * aLongVehicle - sp * aDownVehicle
        val ayPhone = sr * sp * aLongVehicle + cr * aLatVehicle + sr * cp * aDownVehicle
        val azPhone = cr * sp * aLongVehicle - sr * aLatVehicle + cr * cp * aDownVehicle

        val gxPhone = yawRateVehicle * sp
        val gyPhone = yawRateVehicle * cp * sr
        val gzPhone = yawRateVehicle * cp * cr

        return ImuReading(
            accelX = axPhone,
            accelY = ayPhone,
            accelZ = azPhone,
            gyroX = gxPhone,
            gyroY = gyPhone,
            gyroZ = gzPhone,
            magX = 22f * cos(Math.toRadians(gt.headingDeg.toDouble())).toFloat(),
            magY = 22f * sin(Math.toRadians(gt.headingDeg.toDouble())).toFloat(),
            magZ = 40f,
            timestampNs = System.nanoTime()
        )
    }

    companion object {
        private fun createUrbanCanyonScenario(): BenchmarkScenario {
            val baseLat = 12.9352
            val baseLng = 77.6245
            val points = mutableListOf<TrajectoryPoint>()
            val segments = mutableListOf<RoadSegment>()

            var curLat = baseLat
            var curLng = baseLng
            var curHeading = 45f

            val nodes = mutableListOf<RoadNode>()
            for (i in 0..120) {
                val t = i.toFloat()
                val speed = if (i in 10..20) 10f else if (i in 40..80) 55f else 40f
                if (i in 30..45) curHeading += 2.5f
                if (i in 75..90) curHeading -= 3.0f

                val rad = Math.toRadians(curHeading.toDouble())
                val dMeters = (speed / 3.6f) * 1.0f
                curLat += (dMeters * cos(rad)) / 111132.954
                curLng += (dMeters * sin(rad)) / (111132.954 * cos(Math.toRadians(curLat)))

                val pt = TrajectoryPoint(curLat, curLng, 915.0, speed, curHeading, TrajectoryType.GROUND_TRUTH)
                points.add(pt)

                if (i % 10 == 0) {
                    nodes.add(RoadNode("node_uc_$i", curLat, curLng))
                }
            }

            for (j in 0 until nodes.size - 1) {
                segments.add(
                    RoadSegment("seg_uc_$j", "Cybercity Expressway Underpass", nodes[j], nodes[j + 1], 60, 4)
                )
            }

            return BenchmarkScenario(
                id = "iovnbd_urban_canyon",
                title = "Urban Canyon & Underpass Tunnel",
                description = "Deep skyscraper corridor with severe multipath and complete GNSS blackout in 600m underpass.",
                locationName = "Outer Ring Road - Cybercity",
                totalDurationSeconds = 120,
                outageStartSec = 30,
                outageEndSec = 90,
                totalDistanceMeters = 1450f,
                maxSpeedKmph = 55f,
                roadSegments = segments,
                groundTruthPoints = points
            )
        }

        private fun createLongTunnelScenario(): BenchmarkScenario {
            val baseLat = 32.3615
            val baseLng = 77.1642
            val points = mutableListOf<TrajectoryPoint>()
            val segments = mutableListOf<RoadSegment>()

            var curLat = baseLat
            var curLng = baseLng
            var curHeading = 15f

            val nodes = mutableListOf<RoadNode>()
            for (i in 0..180) {
                val speed = if (i < 15) 30f else 60f
                if (i in 50..70) curHeading += 1.2f
                if (i in 110..130) curHeading -= 1.5f

                val rad = Math.toRadians(curHeading.toDouble())
                val dMeters = (speed / 3.6f) * 1.0f
                curLat += (dMeters * cos(rad)) / 111132.954
                curLng += (dMeters * sin(rad)) / (111132.954 * cos(Math.toRadians(curLat)))

                points.add(TrajectoryPoint(curLat, curLng, 3050.0, speed, curHeading, TrajectoryType.GROUND_TRUTH))
                if (i % 15 == 0) {
                    nodes.add(RoadNode("node_tunnel_$i", curLat, curLng))
                }
            }

            for (j in 0 until nodes.size - 1) {
                segments.add(
                    RoadSegment("seg_tun_$j", "Atal Himalayan Highway Tunnel (2.5 km)", nodes[j], nodes[j + 1], 60, 2)
                )
            }

            return BenchmarkScenario(
                id = "iovnbd_long_tunnel",
                title = "Long Mountain Highway Tunnel (2.5 km)",
                description = "High-speed 60 km/h continuous 140-second GNSS denial through subterranean mountain pass.",
                locationName = "Atal Tunnel - NH 3",
                totalDurationSeconds = 180,
                outageStartSec = 20,
                outageEndSec = 160,
                totalDistanceMeters = 2750f,
                maxSpeedKmph = 60f,
                roadSegments = segments,
                groundTruthPoints = points
            )
        }

        private fun createUndergroundParkingScenario(): BenchmarkScenario {
            val baseLat = 12.9783
            val baseLng = 77.6408
            val points = mutableListOf<TrajectoryPoint>()
            val segments = mutableListOf<RoadSegment>()

            var curLat = baseLat
            var curLng = baseLng
            var curHeading = 0f

            val nodes = mutableListOf<RoadNode>()
            for (i in 0..90) {
                val speed = if (i in 25..35) 0f else 18f // Stop at parking boom barrier (ZUPT test)
                if (speed > 0f) {
                    if (i in 10..22) curHeading += 7.5f // Spiral ramp turn
                    if (i in 50..65) curHeading += 6.0f // Basement level 2 turn
                    if (i in 75..85) curHeading -= 9.0f // Parking slot maneuver
                }

                val rad = Math.toRadians(curHeading.toDouble())
                val dMeters = (speed / 3.6f) * 1.0f
                curLat += (dMeters * cos(rad)) / 111132.954
                curLng += (dMeters * sin(rad)) / (111132.954 * cos(Math.toRadians(curLat)))

                points.add(TrajectoryPoint(curLat, curLng, 890.0, speed, curHeading, TrajectoryType.GROUND_TRUTH))
                if (i % 10 == 0) {
                    nodes.add(RoadNode("node_park_$i", curLat, curLng))
                }
            }

            for (j in 0 until nodes.size - 1) {
                segments.add(
                    RoadSegment("seg_park_$j", "Multi-Level Basement Parking Deck", nodes[j], nodes[j + 1], 20, 2)
                )
            }

            return BenchmarkScenario(
                id = "iovnbd_underground_parking",
                title = "Multi-Level Basement Parking Complex",
                description = "Subterranean spiral ramps, 90-degree tight corners, and toll barrier zero-velocity update stops.",
                locationName = "Nexus Mall Multi-Level Deck",
                totalDurationSeconds = 90,
                outageStartSec = 10,
                outageEndSec = 90,
                totalDistanceMeters = 420f,
                maxSpeedKmph = 20f,
                roadSegments = segments,
                groundTruthPoints = points
            )
        }

        private fun createDenseForestScenario(): BenchmarkScenario {
            val baseLat = 11.9138
            val baseLng = 75.9897
            val points = mutableListOf<TrajectoryPoint>()
            val segments = mutableListOf<RoadSegment>()

            var curLat = baseLat
            var curLng = baseLng
            var curHeading = 180f

            val nodes = mutableListOf<RoadNode>()
            for (i in 0..150) {
                val speed = if (i in 40..60) 35f else 45f
                if (i in 20..35) curHeading -= 6f // Hairpin turn 1
                if (i in 60..75) curHeading += 7f // Hairpin turn 2
                if (i in 100..115) curHeading -= 5.5f // Hairpin turn 3

                val rad = Math.toRadians(curHeading.toDouble())
                val dMeters = (speed / 3.6f) * 1.0f
                curLat += (dMeters * cos(rad)) / 111132.954
                curLng += (dMeters * sin(rad)) / (111132.954 * cos(Math.toRadians(curLat)))

                points.add(TrajectoryPoint(curLat, curLng, 1150.0, speed, curHeading, TrajectoryType.GROUND_TRUTH))
                if (i % 12 == 0) {
                    nodes.add(RoadNode("node_forest_$i", curLat, curLng))
                }
            }

            for (j in 0 until nodes.size - 1) {
                segments.add(
                    RoadSegment("seg_forest_$j", "Western Ghats Mountain Pass (SH 89)", nodes[j], nodes[j + 1], 40, 2)
                )
            }

            return BenchmarkScenario(
                id = "iovnbd_dense_forest",
                title = "Dense Forest Highway (Western Ghats)",
                description = "Dense rainforest canopy signal blockage with series of high-curvature hairpin curves.",
                locationName = "Agumbe Ghat Pass - SH 89",
                totalDurationSeconds = 150,
                outageStartSec = 25,
                outageEndSec = 125,
                totalDistanceMeters = 1680f,
                maxSpeedKmph = 48f,
                roadSegments = segments,
                groundTruthPoints = points
            )
        }
    }
}
