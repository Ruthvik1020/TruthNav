package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MountPreset
import com.example.ui.NavViewModel
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CalibrationScreen(
    viewModel: NavViewModel,
    modifier: Modifier = Modifier
) {
    val calibration by viewModel.calibration.collectAsState()
    val imu by viewModel.currentImu.collectAsState()
    val scrollState = rememberScrollState()

    var customPitch by remember { mutableFloatStateOf(calibration.pitchDeg) }
    var customRoll by remember { mutableFloatStateOf(calibration.rollDeg) }
    var customYaw by remember { mutableFloatStateOf(calibration.yawDeg) }
    var isAutoCalibrating by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(bottom = 64.dp)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Tune, contentDescription = "Calibration", tint = AmberAccent)
            Column {
                Text(
                    text = "In-Vehicle Alignment & Calibration",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Resolves phone Pitch, Roll & Yaw relative to Vehicle Navigation Frame",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        // 3D Mount Attitude Visualization Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "PHONE ORIENTATION IN VEHICLE FRAME",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = AmberAccent
                )

                // Phone Mount Visual Canvas
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .background(SpaceCardElevated, CircleShape)
                        .border(1.dp, SpaceBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f

                        // Vehicle reference base
                        drawLine(
                            color = SpaceBorderBright,
                            start = Offset(cx - 50f, cy + 50f),
                            end = Offset(cx + 50f, cy + 50f),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round
                        )

                        // Smartphone Tilt Line (Pitch)
                        val pitchRad = Math.toRadians(calibration.pitchDeg.toDouble())
                        val phoneLen = 65f
                        val topX = cx - (phoneLen * sin(pitchRad)).toFloat()
                        val topY = (cy + 50f) - (phoneLen * cos(pitchRad)).toFloat()

                        // Phone Body
                        drawLine(
                            color = AmberAccent,
                            start = Offset(cx, cy + 50f),
                            end = Offset(topX, topY),
                            strokeWidth = 8f,
                            cap = StrokeCap.Round
                        )

                        // Screen face indicator
                        drawLine(
                            color = Color.White,
                            start = Offset(cx, cy + 50f),
                            end = Offset(topX, topY),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round
                        )

                        // Gravity vector arrow (down)
                        drawLine(
                            color = EmeraldGps,
                            start = Offset(cx, cy - 10f),
                            end = Offset(cx, cy + 30f),
                            strokeWidth = 2.5f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Live Angles Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AnglePill("PITCH (Tilt)", "${"%.1f".format(calibration.pitchDeg)}°", AmberAccent)
                    AnglePill("ROLL (Bank)", "${"%.1f".format(calibration.rollDeg)}°", NavicBlueLight)
                    AnglePill("YAW (Heading)", "${"%.1f".format(calibration.yawDeg)}°", EmeraldGps)
                }
            }
        }

        // Mount Preset Selector Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "SELECT MOUNTING HARDWARE PRESET",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary
                )

                MountPreset.entries.forEach { preset ->
                    val isSelected = calibration.mountPreset == preset
                    Surface(
                        onClick = {
                            viewModel.setMountPreset(preset)
                            customPitch = preset.pitch
                            customRoll = preset.roll
                            customYaw = preset.yaw
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) SpaceCardElevated else SpaceSurfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) AmberAccent else SpaceBorder.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("preset_${preset.name}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = preset.displayName,
                                    color = if (isSelected) AmberAccent else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = "Preset Pitch: ${preset.pitch.toInt()}° • Roll: ${preset.roll.toInt()}°",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = AmberAccent)
                            }
                        }
                    }
                }
            }
        }

        // Manual Fine-Tuning Sliders Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "MANUAL ATTITUDE FINE-TUNING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary
                )

                // Pitch Slider
                Text(text = "Pitch Angle: ${customPitch.toInt()}° (Upright vs Flat)", color = TextPrimary, fontSize = 12.sp)
                Slider(
                    value = customPitch,
                    onValueChange = {
                        customPitch = it
                        viewModel.setManualMountAngles(customPitch, customRoll, customYaw)
                    },
                    valueRange = 0f..90f,
                    colors = SliderDefaults.colors(thumbColor = AmberAccent, activeTrackColor = AmberAccent)
                )

                // Roll Slider
                Text(text = "Roll Angle: ${customRoll.toInt()}° (Lateral Tilt)", color = TextPrimary, fontSize = 12.sp)
                Slider(
                    value = customRoll,
                    onValueChange = {
                        customRoll = it
                        viewModel.setManualMountAngles(customPitch, customRoll, customYaw)
                    },
                    valueRange = -45f..45f,
                    colors = SliderDefaults.colors(thumbColor = NavicBlueLight, activeTrackColor = NavicBlueLight)
                )

                // Yaw Slider
                Text(text = "Yaw Misalignment: ${customYaw.toInt()}° (Azimuth Offset)", color = TextPrimary, fontSize = 12.sp)
                Slider(
                    value = customYaw,
                    onValueChange = {
                        customYaw = it
                        viewModel.setManualMountAngles(customPitch, customRoll, customYaw)
                    },
                    valueRange = -45f..45f,
                    colors = SliderDefaults.colors(thumbColor = EmeraldGps, activeTrackColor = EmeraldGps)
                )
            }
        }

        // Live IMU Gravity Vector
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceCard)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "LIVE SENSOR LEVELING READINGS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Ax: ${"%.2f".format(imu.accelX)} m/s²", color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Text(text = "Ay: ${"%.2f".format(imu.accelY)} m/s²", color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Text(text = "Az: ${"%.2f".format(imu.accelZ)} m/s²", color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Gx: ${"%.2f".format(imu.gyroX)} rad/s", color = TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Text(text = "Gy: ${"%.2f".format(imu.gyroY)} rad/s", color = TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Text(text = "Gz: ${"%.2f".format(imu.gyroZ)} rad/s", color = TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun AnglePill(label: String, value: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SpaceCardElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
            Text(text = value, fontSize = 15.sp, color = color, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}
