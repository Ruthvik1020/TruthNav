package com.example.engine

import kotlin.math.abs

/**
 * Non-Holonomic Constraints (NHC) Engine (ISRO NavDR)
 *
 * Enforces land vehicle kinematic physics:
 * - Zero lateral velocity (v_lateral = 0): A car or two-wheeler cannot slide sideways.
 * - Zero vertical velocity (v_vertical = 0): A vehicle does not fly upwards or dive into pavement.
 *
 * Returns corrected velocity vector components in vehicle navigation frame.
 */
class NonHolonomicConstraints {

    /**
     * Applies NHC corrections to raw inertial velocity components.
     * @param vLongitudinal Estimated forward velocity (m/s)
     * @param vLateral Raw integrated lateral velocity (m/s)
     * @param vVertical Raw integrated vertical velocity (m/s)
     * @return Triple(corrected_vForward, corrected_vLateral, corrected_vVertical)
     */
    fun applyConstraints(
        vLongitudinal: Float,
        vLateral: Float,
        vVertical: Float,
        isTurning: Boolean
    ): Triple<Float, Float, Float> {
        // Enforce non-negative forward speed for standard navigation
        val correctedForward = vLongitudinal.coerceAtLeast(0f)

        // Strict dampening of lateral velocity: vehicles do not slide sideways unless severe skidding
        val lateralDamping = if (isTurning) 0.15f else 0.02f
        val correctedLateral = vLateral * lateralDamping

        // Strict dampening of vertical velocity: vehicle remains on road plane
        val correctedVertical = vVertical * 0.01f

        return Triple(correctedForward, correctedLateral, correctedVertical)
    }

    /**
     * Verifies if motion is physically consistent with land vehicle kinematics
     */
    fun validateKinematicPlausibility(accelLong: Float, accelLat: Float, yawRateRad: Float): Boolean {
        // Road vehicle acceleration bounds: -10 m/s^2 (hard braking) to +6 m/s^2 (hard acceleration)
        val isAccelPlausible = accelLong in -12f..8f
        // Lateral acceleration rarely exceeds 0.8g (8 m/s^2) in normal driving
        val isLateralPlausible = abs(accelLat) < 9.0f
        return isAccelPlausible && isLateralPlausible
    }
}
