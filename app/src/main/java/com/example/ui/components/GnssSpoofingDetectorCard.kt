package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GnssSpoofingReport
import com.example.ui.theme.*

@Composable
fun GnssSpoofingDetectorCard(
    report: GnssSpoofingReport,
    onToggleSpoofingSimulation: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }

    val isAlert = report.isSpoofingDetected
    val pulseTransition = rememberInfiniteTransition(label = "spoofPulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alertAlpha"
    )

    val cardBorderColor by animateColorAsState(
        targetValue = if (isAlert) RedJam.copy(alpha = pulseAlpha) else Color(0xFF243447),
        label = "cardBorderColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("gnss_spoofing_detector_card")
            .border(1.5.dp, cardBorderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceDarkSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isAlert) RedJam.copy(alpha = 0.25f) else EmeraldGps.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAlert) Icons.Default.GppBad else Icons.Default.GppGood,
                            contentDescription = "Spoofing Security Status",
                            tint = if (isAlert) RedJam else EmeraldGps,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "GNSS SPOOFING DETECTOR",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAlert) RedJam else CyanAccent,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = if (isAlert) "INTEGRITY ${report.gnssIntegrityPercent}% (REJECTED)" else "INTEGRITY 100% (TRUSTED)",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isAlert) RedJam else TextPrimary,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Killer Demo Attack Button
                    FilterChip(
                        selected = report.isSpoofingSimulationActive,
                        onClick = { onToggleSpoofingSimulation(!report.isSpoofingSimulationActive) },
                        label = {
                            Text(
                                text = if (report.isSpoofingSimulationActive) "ATTACK ON" else "SIMULATE ATTACK",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RedJam,
                            selectedLabelColor = TextPrimary,
                            containerColor = SpaceCardElevated,
                            labelColor = TextMuted
                        ),
                        modifier = Modifier.testTag("simulate_spoofing_chip")
                    )
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand/Collapse",
                            tint = TextMuted
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // Alert Banner if Spoofed
                    if (isAlert) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(RedJam.copy(alpha = 0.2f))
                                .border(1.dp, RedJam, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Warning",
                                        tint = RedJam,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "⚠ GNSS INCONSISTENCY DETECTED",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = RedJam,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Text(
                                    text = "GNSS rejected (Integrity: ${report.gnssIntegrityPercent}%). Vehicle position locked to INS + MAP. Zero jump observed.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Comparison: Normal App vs NavSense-X
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Unprotected App Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = SpaceCardElevated)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "NORMAL NAVIGATION",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🚗 ", fontSize = 14.sp)
                                    Text(
                                        text = if (isAlert) "JUMPS TO WRONG ROAD" else "Tracks GNSS blindly",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isAlert) OrangeFlame else TextMuted,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = if (isAlert) "Jump: +27.4m | Error: 38m" else "Zero verification",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // NavSense-X Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isAlert) RedJam.copy(alpha = 0.15f) else Color(0xFF102A24))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "NAVSENSE-X FUSION",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldGps,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🛡️ ", fontSize = 14.sp)
                                    Text(
                                        text = if (isAlert) "GNSS REJECTED" else "INS + MAP Active",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldGps,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = if (isAlert) "Retained True Road (<4m)" else "Multi-sensor cross check",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 5-Metric Security Inspection Grid
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SpaceCardElevated)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "AUTONOMOUS KINEMATIC & RF CROSS-CHECK",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Metric 1: Speed disagreement
                        MetricRow(
                            label = "Speed Cross-Check",
                            leftValue = "GNSS: ${"%.1f".format(report.gnssSpeedKmph)} km/h",
                            rightValue = "IMU: ${"%.1f".format(report.imuSpeedKmph)} km/h",
                            delta = "Δ: ${"%.1f".format(report.speedDeltaKmph)} km/h",
                            isWarning = report.speedDeltaKmph > 15f
                        )

                        HorizontalDivider(color = SpaceDarkSurface, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                        // Metric 2: Heading disagreement
                        MetricRow(
                            label = "Heading Alignment",
                            leftValue = "Bearing Disagreement",
                            rightValue = "${"%.1f".format(report.headingDisagreementDeg)}°",
                            delta = if (report.headingDisagreementDeg > 20f) "DIVERGENT" else "ALIGNED",
                            isWarning = report.headingDisagreementDeg > 20f
                        )

                        HorizontalDivider(color = SpaceDarkSurface, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                        // Metric 3: Spatial Position Jump
                        MetricRow(
                            label = "Spatial Discontinuity",
                            leftValue = "Position Step Delta",
                            rightValue = "${"%.1f".format(report.positionJumpMeters)} m",
                            delta = if (report.positionJumpMeters > 18f) "ANOMALOUS JUMP" else "CONTINUOUS",
                            isWarning = report.positionJumpMeters > 18f
                        )

                        HorizontalDivider(color = SpaceDarkSurface, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                        // Metric 4: C/N0 & AGC Status
                        MetricRow(
                            label = "Signal & AGC Checks",
                            leftValue = "C/N0: ${if (report.cn0PatternAbnormal) "ABNORMAL (Uniform)" else "NOMINAL (Scattered)"}",
                            rightValue = "AGC: ${if (report.agcAbnormal) "ABNORMAL (-14dB)" else "NOMINAL (-2dB)"}",
                            delta = if (report.cn0PatternAbnormal || report.agcAbnormal) "RF ANOMALY" else "CLEAN RF",
                            isWarning = report.cn0PatternAbnormal || report.agcAbnormal
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Final Action Footer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SECURITY PROTOCOL:",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = report.actionTaken,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAlert) RedJam else EmeraldGps,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    leftValue: String,
    rightValue: String,
    delta: String,
    isWarning: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.3f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            )
            Text(
                text = "$leftValue • $rightValue",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                fontSize = 10.sp
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (isWarning) RedJam.copy(alpha = 0.2f) else EmeraldGps.copy(alpha = 0.15f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = delta,
                style = MaterialTheme.typography.labelSmall,
                color = if (isWarning) RedJam else EmeraldGps,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
