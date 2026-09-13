package com.example.engine

import com.example.model.CalibrationAngles
import com.example.model.ImuReading
import com.example.model.MountPreset
import kotlin.math.*

/**
 * In-Vehicle Alignment & Calibration Engine (ISRO NavDR)
 * Determines phone's pitch, roll, and yaw relative to the vehicle coordinate frame
 * (X = Forward, Y = Right, Z = Downwards/Chassis).
 *
 * Implements:
 * 1. Gravity Vector Leveling (pitch/roll determination during static/quasi-static state)
 * 2. Dynamic Acceleration Alignment (longitudinal vehicle axis determination from braking/acceleration)
 * 3. Direction Cosine Matrix (DCM) rotation transformation R_b_to_v
 */
class VehicleAlignmentCalibrator {

    var currentCalibration = CalibrationAngles(
        pitchDeg = 60f,
        rollDeg = 0f,
        yawDeg = 0f,
        isCalibrated = true,
        mountPreset = MountPreset.DASHBOARD_HOLDER
    )
        private set

    // Low-pass filtered gravity components
    private var gravX = 0f
    private var gravY = 0f
    private var gravZ = 9.81f

    // Dynamic horizontal acceleration components for forward axis estimation
    private var dynAccX = 0f
    private var dynAccY = 0f
    private var dynSamplesCount = 0

    // Rotation Matrix R_b^v (3x3)
    private var rMat = Array(3) { FloatArray(3) }

    init {
        updateRotationMatrix(currentCalibration.pitchDeg, currentCalibration.rollDeg, currentCalibration.yawDeg)
    }

    /**
     * Updates calibration preset or manual angles
     */
    fun setPreset(preset: MountPreset) {
        currentCalibration = currentCalibration.copy(
            pitchDeg = preset.pitch,
            rollDeg = preset.roll,
            yawDeg = preset.yaw,
            mountPreset = preset,
            isCalibrated = true
        )
        updateRotationMatrix(preset.pitch, preset.roll, preset.yaw)
    }

    fun setManualAngles(pitch: Float, roll: Float, yaw: Float) {
        currentCalibration = currentCalibration.copy(
            pitchDeg = pitch,
            rollDeg = roll,
            yawDeg = yaw,
            mountPreset = MountPreset.CUSTOM_DYNAMIC,
            isCalibrated = true
        )
        updateRotationMatrix(pitch, roll, yaw)
    }

    /**
     * Automatic In-Vehicle Calibration:
     * Analyzes streaming IMU frames to level gravity and align forward driving axis.
     */
    fun processCalibrationSample(imu: ImuReading, isMoving: Boolean) {
        val alpha = 0.05f
        gravX = (1 - alpha) * gravX + alpha * imu.accelX
        gravY = (1 - alpha) * gravY + alpha * imu.accelY
        gravZ = (1 - alpha) * gravZ + alpha * imu.accelZ

        val gNorm = sqrt(gravX * gravX + gravY * gravY + gravZ * gravZ)
        if (gNorm in 8.5f..11.5f && !isMoving) {
            // Leveling from gravity:
            // Pitch (tilt up/down): theta = asin(-gravX / gNorm)
            // Roll (tilt left/right): phi = atan2(gravY, gravZ)
            val pitch = Math.toDegrees(asin((-gravX / gNorm).coerceIn(-1f, 1f).toDouble())).toFloat()
            val roll = Math.toDegrees(atan2(gravY.toDouble(), gravZ.toDouble())).toFloat()

            currentCalibration = currentCalibration.copy(
                pitchDeg = pitch,
                rollDeg = roll,
                confidence = 0.96f
            )
            updateRotationMatrix(pitch, roll, currentCalibration.yawDeg)
        } else if (isMoving) {
            // Forward Axis Alignment using dynamic horizontal acceleration
            dynSamplesCount++
            val hAccX = imu.accelX - gravX
            val hAccY = imu.accelY - gravY
            dynAccX += hAccX
            dynAccY += hAccY

            if (dynSamplesCount > 50) {
                val yawEst = Math.toDegrees(atan2(dynAccY.toDouble(), dynAccX.toDouble())).toFloat()
                if (!yawEst.isNaN()) {
                    currentCalibration = currentCalibration.copy(
                        yawDeg = yawEst,
                        confidence = 0.98f
                    )
                    updateRotationMatrix(currentCalibration.pitchDeg, currentCalibration.rollDeg, yawEst)
                }
                dynSamplesCount = 0
                dynAccX = 0f
                dynAccY = 0f
            }
        }
    }

    /**
     * Rotates body frame acceleration [ax, ay, az] into Vehicle Frame [a_forward, a_right, a_down]
     * Subtracts gravity in vehicle frame.
     */
    fun transformToVehicleFrame(imu: ImuReading): Triple<Float, Float, Float> {
        val ax = imu.accelX
        val ay = imu.accelY
        val az = imu.accelZ

        // Matrix multiplication: a_v = R * a_b
        val aLongitudinal = rMat[0][0] * ax + rMat[0][1] * ay + rMat[0][2] * az
        val aLateral = rMat[1][0] * ax + rMat[1][1] * ay + rMat[1][2] * az
        val aDown = rMat[2][0] * ax + rMat[2][1] * ay + rMat[2][2] * az - 9.81f

        return Triple(aLongitudinal, aLateral, aDown)
    }

    /**
     * Rotates body frame angular rates [gx, gy, gz] into Vehicle Frame [roll_rate, pitch_rate, yaw_rate]
     */
    fun transformAngularRates(imu: ImuReading): Triple<Float, Float, Float> {
        val gx = imu.gyroX
        val gy = imu.gyroY
        val gz = imu.gyroZ

        val rollRate = rMat[0][0] * gx + rMat[0][1] * gy + rMat[0][2] * gz
        val pitchRate = rMat[1][0] * gx + rMat[1][1] * gy + rMat[1][2] * gz
        val yawRate = rMat[2][0] * gx + rMat[2][1] * gy + rMat[2][2] * gz

        return Triple(rollRate, pitchRate, yawRate)
    }

    private fun updateRotationMatrix(pitchDeg: Float, rollDeg: Float, yawDeg: Float) {
        val p = Math.toRadians(pitchDeg.toDouble())
        val r = Math.toRadians(rollDeg.toDouble())
        val y = Math.toRadians(yawDeg.toDouble())

        val cp = cos(p).toFloat()
        val sp = sin(p).toFloat()
        val cr = cos(r).toFloat()
        val sr = sin(r).toFloat()
        val cy = cos(y).toFloat()
        val sy = sin(y).toFloat()

        // 3-2-1 Euler transformation matrix: R_z(yaw) * R_y(pitch) * R_x(roll)
        rMat[0][0] = cy * cp
        rMat[0][1] = cy * sp * sr - sy * cr
        rMat[0][2] = cy * sp * cr + sy * sr

        rMat[1][0] = sy * cp
        rMat[1][1] = sy * sp * sr + cy * cr
        rMat[1][2] = sy * sp * cr - cy * sr

        rMat[2][0] = -sp
        rMat[2][1] = cp * sr
        rMat[2][2] = cp * cr
    }
}
