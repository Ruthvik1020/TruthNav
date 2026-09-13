package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.*
import com.example.model.ImuReading
import com.example.model.MountPreset
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NavDR", appName)
    }

    @Test
    fun `test vehicle alignment calibrator rotation`() {
        val calibrator = VehicleAlignmentCalibrator()
        calibrator.setPreset(MountPreset.DASHBOARD_HOLDER)
        assertEquals(60f, calibrator.currentCalibration.pitchDeg)

        val imu = ImuReading(accelX = 0f, accelY = 0f, accelZ = 9.81f)
        val (aLong, aLat, aDown) = calibrator.transformToVehicleFrame(imu)
        assertNotNull(aLong)
        assertNotNull(aLat)
        assertNotNull(aDown)
    }

    @Test
    fun `test ai speed filter zupt detection`() {
        val filter = AiSpeedVibrationFilter()
        // Simulate stationary vehicle with zero dynamic variance
        var isZuptActive = false
        for (i in 0 until 30) {
            val (zupt, _, _) = filter.analyzeVibrationsAndMotion(
                aLong = 0.01f,
                aLat = 0.01f,
                aDown = 9.81f,
                yawRateRad = 0.001f
            )
            isZuptActive = zupt
        }
        assertTrue("ZUPT should be active during stationary state", isZuptActive)
    }

    @Test
    fun `test dead reckoning fusion engine drift performance`() {
        val fusion = GnssInsFusionEngine()
        fusion.initPosition(12.9716, 77.5946, 90f)

        // Step 100 IMU cycles
        for (i in 0 until 100) {
            fusion.processImu(
                ImuReading(accelX = 0.5f, accelY = 0f, accelZ = 9.81f),
                dtSec = 0.1f
            )
        }

        val state = fusion.currentState
        assertTrue("Distance traveled should be positive", state.distanceTraveledMeters > 0f)
        assertTrue("Heading should be tracked", state.headingDeg >= 0f)
    }

    @Test
    fun `test digital twin simultaneous multi-trajectory generation`() {
        val fusion = GnssInsFusionEngine()
        fusion.initPosition(12.9716, 77.5946, 90f)

        // Step 50 cycles of acceleration
        for (i in 0 until 50) {
            fusion.processImu(
                ImuReading(accelX = 1.0f, accelY = 0.05f, accelZ = 9.81f, gyroZ = 0.02f),
                dtSec = 0.1f
            )
        }

        // Verify that all digital twin trajectories are simultaneously maintained
        assertTrue("Trajectory A (Physics Pure IMU) must have points", fusion.trajectoryAPhysicsTrail.isNotEmpty())
        assertTrue("Trajectory B (AI Corrected) must have points", fusion.trajectoryBAiTrail.isNotEmpty())
        assertTrue("Trajectory C (Map Constrained) must have points", fusion.trajectoryCMapTrail.isNotEmpty())
        assertTrue("Trajectory D (Visual Corrected) must have points", fusion.trajectoryDVisualTrail.isNotEmpty())
        assertTrue("Master Fusion Trail must have points", fusion.masterFusionTrail.isNotEmpty())

        val metrics = fusion.getDigitalTwinMetrics(12.9716, 77.5950, "Test Run")
        assertTrue("Pure INS error must be non-negative", metrics.pureInsErrorMeters >= 0f)
        assertTrue("AI INS error must be non-negative", metrics.aiInsErrorMeters >= 0f)
    }

    @Test
    fun `test ai navigation state machine transitions and manual evaluator override`() {
        val fusion = GnssInsFusionEngine()
        fusion.initPosition(12.9716, 77.5946, 90f)

        val initialReport = fusion.getNavigationIntegrityReport()
        assertNotNull(initialReport.state)
        assertEquals("0.0 m or baseline error initially", true, initialReport.estimatedErrorMeters >= 0f)

        // Test manual evaluator state machine trigger
        fusion.setManualNavState(com.example.model.AiNavState.STATE_4_GNSS_OUTAGE)
        val outageReport = fusion.getNavigationIntegrityReport()
        assertEquals(com.example.model.AiNavState.STATE_4_GNSS_OUTAGE, outageReport.state)
        assertEquals("UNTRUSTED", outageReport.gnssTrust)
        assertTrue(outageReport.isEvaluatorManualOverride)

        // Restore autonomous state machine
        fusion.setManualNavState(null)
        val restoredReport = fusion.getNavigationIntegrityReport()
        assertFalse(restoredReport.isEvaluatorManualOverride)
    }
}
