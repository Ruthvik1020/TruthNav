package com.example

import com.example.engine.BarometricAltitudeEngine
import com.example.engine.GnssSpoofingDetector
import com.example.model.GnssReading
import com.example.model.ImuReading
import com.example.model.VehicleState
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testNominalGnssIntegrity() {
        val detector = GnssSpoofingDetector()
        val imu = ImuReading(accelX = 0f, accelY = 0f, accelZ = 9.8f, gyroX = 0f, gyroY = 0f, gyroZ = 0f)
        val gnss = GnssReading(lat = 12.9716, lng = 77.5946, altitude = 920.0, speedMps = 12.0f, bearingDeg = 90f)
        val vehicle = VehicleState(lat = 12.9716, lng = 77.5946, speedMps = 12.0f, headingDeg = 90f)

        detector.setSimulatedSpoofingAttack(false)
        val report = detector.evaluateGnssIntegrity(
            gnss = gnss,
            currentState = vehicle,
            imuReading = imu,
            imuForwardSpeedMps = 12.0f
        )

        assertFalse("Spoofing should NOT be detected for nominal signals", report.isSpoofingDetected)
        assertEquals("Integrity should be 100% for consistent signals", 100, report.gnssIntegrityPercent)
        assertTrue(report.actionTaken.contains("TRUSTED") || report.actionTaken.contains("NOMINAL"))
    }

    @Test
    fun testKillerSpoofingAttackDetection() {
        val detector = GnssSpoofingDetector()
        val imu = ImuReading(accelX = 0f, accelY = 0f, accelZ = 9.8f, gyroX = 0f, gyroY = 0f, gyroZ = 0f)
        // Vehicle traveling at 12 m/s (~43 km/h) heading 90 deg
        val vehicle = VehicleState(lat = 12.9716, lng = 77.5946, speedMps = 11.94f, headingDeg = 90f)
        // Spoofed GNSS: jumps 27m, claims 18.88 m/s (68 km/h) and 121 deg heading (31 deg divergence)
        val gnss = GnssReading(lat = 12.97184, lng = 77.5946, altitude = 920.0, speedMps = 18.88f, bearingDeg = 121f)

        detector.setSimulatedSpoofingAttack(true)
        val report = detector.evaluateGnssIntegrity(
            gnss = gnss,
            currentState = vehicle,
            imuReading = imu,
            imuForwardSpeedMps = 11.94f
        )

        assertTrue("Spoofing MUST be detected when simulation/anomaly is triggered", report.isSpoofingDetected)
        assertTrue("Integrity must drop below 20%", report.gnssIntegrityPercent <= 20)
        assertTrue("Speed delta must reflect discrepancy", report.speedDeltaKmph >= 15f)
        assertTrue("Heading disagreement must reflect > 20 deg", report.headingDisagreementDeg >= 20f)
        assertTrue("Action taken must indicate GNSS rejection and INS+MAP retention", report.actionTaken.contains("REJECTED"))
    }

    @Test
    fun test3DBarometricAltitudeLayerClassification() {
        val engine = BarometricAltitudeEngine()
        engine.calibrateBaseline(1013.25f)

        // Baseline surface
        val surfaceState = engine.update(
            dtSec = 0.1f,
            forwardSpeedMps = 11f,
            pitchDeg = 0f,
            accelDown = 9.81f,
            rawPressureHpa = 1013.25f
        )
        assertEquals("L0", surfaceState.levelCode)

        // Simulated layer override test (e.g. Flyover L2)
        engine.setSimulatedLayer("L2")
        var flyoverState = surfaceState
        repeat(25) {
            flyoverState = engine.update(
                dtSec = 0.1f,
                forwardSpeedMps = 11f,
                pitchDeg = 2.5f,
                accelDown = 9.81f
            )
        }
        assertEquals("L2", flyoverState.levelCode)
        assertTrue("Altitude should be around 12m", flyoverState.relativeAltitudeMeters > 9f)

        // Simulated tunnel override test (B1)
        engine.setSimulatedLayer("B1")
        var tunnelState = flyoverState
        repeat(30) {
            tunnelState = engine.update(
                dtSec = 0.1f,
                forwardSpeedMps = 11f,
                pitchDeg = -2.0f,
                accelDown = 9.81f
            )
        }
        assertEquals("B1", tunnelState.levelCode)
        assertTrue("Altitude should be negative", tunnelState.relativeAltitudeMeters < -6f)
    }
}
