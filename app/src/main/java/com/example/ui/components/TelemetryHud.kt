package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DriftMetrics
import com.example.model.FusionMode
import com.example.model.ImuReading
import com.example.model.VehicleState
import com.example.ui.theme.*

@Composable
fun TelemetryHud(
    vehicleState: VehicleState,
    driftMetrics: DriftMetrics,
    imu: ImuReading,
    isManualJamming: Boolean,
    onToggleJamming: () -> Unit,
    modifier: Modifier = Modifier
) {
    val modeColor by animateColorAsState(
        targetValue = when (vehicleState.fusionMode) {
            FusionMode.GNSS_AIDED -> EmeraldGps
            FusionMode.DEAD_RECKONING_AI -> AmberDr
            FusionMode.GNSS_OUTAGE_JAMMED -> RedJam
            FusionMode.CALIBRATING -> AmberAccent
        },
        label = "modeColor"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ==========================================
        // SENSOR CHASSIS TELEMETRY CARD
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Header with live sensor status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Sensors,
                            contentDescription = null,
                            tint = AmberAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "LIVE HARDWARE IMU & KINEMATICS",
                            color = AmberAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SpaceCardElevated
                    ) {
                        Text(
                            text = if (vehicleState.isZuptActive) "ZUPT ACTIVE" else "DYNAMIC MOTION",
                            color = if (vehicleState.isZuptActive) EmeraldGps else NavicBlueLight,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // Accelerometer Axis Gauges (Ax, Ay, Az)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SensorGaugePill(
                        axis = "Ax (Long)",
                        value = "${if (imu.accelX >= 0) "+" else ""}${"%.2f".format(imu.accelX)}",
                        unit = "m/s²",
                        modifier = Modifier.weight(1f)
                    )
                    SensorGaugePill(
                        axis = "Ay (Lat)",
                        value = "${if (imu.accelY >= 0) "+" else ""}${"%.2f".format(imu.accelY)}",
                        unit = "m/s²",
                        modifier = Modifier.weight(1f)
                    )
                    SensorGaugePill(
                        axis = "Az (Down)",
                        value = "${if (imu.accelZ >= 0) "+" else ""}${"%.2f".format(imu.accelZ)}",
                        unit = "m/s²",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Gyroscope Axis Gauges (Gx, Gy, Gz)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SensorGaugePill(
                        axis = "Gx (Roll)",
                        value = "${if (imu.gyroX >= 0) "+" else ""}${"%.2f".format(imu.gyroX)}",
                        unit = "rad/s",
                        modifier = Modifier.weight(1f)
                    )
                    SensorGaugePill(
                        axis = "Gy (Pitch)",
                        value = "${if (imu.gyroY >= 0) "+" else ""}${"%.2f".format(imu.gyroY)}",
                        unit = "rad/s",
                        modifier = Modifier.weight(1f)
                    )
                    SensorGaugePill(
                        axis = "Gz (Yaw)",
                        value = "${if (imu.gyroZ >= 0) "+" else ""}${"%.2f".format(imu.gyroZ)}",
                        unit = "rad/s",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Bottom Status Badges Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(
                        label = "ZUPT ZERO-VEL",
                        isActive = vehicleState.isZuptActive,
                        activeColor = EmeraldGps
                    )
                    StatusBadge(
                        label = "ZARU ZERO-YAW",
                        isActive = vehicleState.isZaruActive,
                        activeColor = AmberAccent
                    )
                    StatusBadge(
                        label = "NHC 3D KINEMATICS",
                        isActive = true,
                        activeColor = NavicBlueLight
                    )
                    StatusBadge(
                        label = if (vehicleState.roadSnapped) "MAP SNAPPED" else "FREE DR",
                        isActive = vehicleState.roadSnapped,
                        activeColor = MapMatchedPath
                    )
                }
            }
        }

        // ==========================================
        // MISSION TACTICAL CONTROL BAR (Jamming Simulator)
        // ==========================================
        Button(
            onClick = onToggleJamming,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isManualJamming) RedJam else AmberAccent,
                contentColor = SpaceDark
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("toggle_jamming_button")
        ) {
            Icon(
                if (isManualJamming) Icons.Default.SignalCellularOff else Icons.Default.GpsOff,
                contentDescription = "Simulate Blackout",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isManualJamming) "RESTORE SATELLITE GNSS" else "SIMULATE TUNNEL BLACKOUT / JAMMING",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun SensorGaugePill(
    axis: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SpaceCardElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorder.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = axis,
                fontSize = 8.5.sp,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = unit,
                    fontSize = 8.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(
    label: String,
    isActive: Boolean,
    activeColor: Color
) {
    Surface(
        shape = RoundedCornerShape(5.dp),
        color = if (isActive) activeColor.copy(alpha = 0.15f) else SpaceSurfaceVariant.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) activeColor.copy(alpha = 0.7f) else SpaceBorder.copy(alpha = 0.3f)
        )
    ) {
        Text(
            text = label,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) activeColor else TextMuted,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}
