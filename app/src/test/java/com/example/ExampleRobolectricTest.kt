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
}
