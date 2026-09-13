package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DriftMetrics
import com.example.model.FusionMode
import com.example.model.VehicleState
import com.example.ui.theme.*

/**
 * Visual Mode Toggle & Outage Status Component
 * Provides high-visibility indication and interactive switching between
 * 'GNSS Active' mode (Satellite Lock) and 'Dead Reckoning' mode (Signal Blackout / Inertial AI).
 */
@Composable
fun GnssModeToggleBar(
    vehicleState: VehicleState,
    driftMetrics: DriftMetrics,
    isBlackoutActive: Boolean,
    onToggleMode: (isBlackout: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isGnssActive = !isBlackoutActive && vehicleState.fusionMode == FusionMode.GNSS_AIDED

    // Pulsing animation for blackout warning
    val infiniteTransition = rememberInfiniteTransition(label = "blackout_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val borderGlowColor by animateColorAsState(
        targetValue = if (isBlackoutActive) RedJam.copy(alpha = pulseAlpha)
        else if (isGnssActive) EmeraldGps.copy(alpha = 0.8f)
        else AmberAccent.copy(alpha = 0.8f),
        label = "border_glow"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("mode_toggle_container"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCard),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderGlowColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Title & Quick Status
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
                        imageVector = if (isGnssActive) Icons.Default.SatelliteAlt else Icons.Default.GpsOff,
                        contentDescription = null,
                        tint = if (isGnssActive) EmeraldGps else RedJam,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "POSITIONING SYSTEM MODE",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                // Status Indicator Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isBlackoutActive) RedJam.copy(alpha = 0.2f)
                    else if (isGnssActive) EmeraldGps.copy(alpha = 0.15f)
                    else AmberAccent.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isBlackoutActive) RedJam.copy(alpha = pulseAlpha)
                        else if (isGnssActive) EmeraldGps
                        else AmberAccent
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (isBlackoutActive) RedJam else if (isGnssActive) EmeraldGps else AmberAccent,
                                    CircleShape
                                )
                        )
                        Text(
                            text = if (isBlackoutActive) "BLACKOUT ACTIVE"
                            else if (isGnssActive) "GNSS LOCKED"
                            else "AI-DR FUSION",
                            color = if (isBlackoutActive) RedJam else if (isGnssActive) EmeraldGps else AmberAccent,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // ==========================================
            // INTERACTIVE DUAL-SEGMENT TOGGLE SELECTOR
            // ==========================================
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SpaceDarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Segment 1: GNSS Active Mode
                    SegmentItem(
                        title = "GNSS Active",
                        subtitle = "NavIC / GPS L1+L5",
                        icon = Icons.Default.Public,
                        isSelected = isGnssActive,
                        selectedContainerColor = EmeraldGps.copy(alpha = 0.18f),
                        selectedBorderColor = EmeraldGps,
                        selectedContentColor = EmeraldGps,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gnss_active_tab"),
                        onClick = { onToggleMode(false) }
                    )

                    // Segment 2: Dead Reckoning Mode (Blackout)
                    SegmentItem(
                        title = "Dead Reckoning",
                        subtitle = "Inertial AI Engine",
                        icon = Icons.Default.Sensors,
                        isSelected = isBlackoutActive,
                        selectedContainerColor = RedJam.copy(alpha = 0.22f),
                        selectedBorderColor = RedJam,
                        selectedContentColor = if (isBlackoutActive) RedJam else AmberAccent,
                        isWarning = isBlackoutActive,
                        pulseAlpha = pulseAlpha,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dead_reckoning_tab"),
                        onClick = { onToggleMode(true) }
                    )
                }
            }

            // ==========================================
            // REAL-TIME CONTEXTUAL FEEDBACK BANNER
            // ==========================================
            AnimatedContent(
                targetState = isBlackoutActive,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                },
                label = "feedback_banner"
            ) { blackout ->
                if (blackout) {
                    // Signal Blackout Warning Feedback
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = RedJam.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RedJam.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth().testTag("blackout_feedback_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(RedJam.copy(alpha = 0.25f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Signal Outage Warning",
                                    tint = RedJam,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "SIGNAL BLACKOUT IN EFFECT",
                                        color = RedJam,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "T+${"%.1f".format(driftMetrics.outageDurationSeconds)}s",
                                        color = AmberAccentBright,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = "Zero GNSS • Propagating via IMU Neural Velocity & Kinematic NHC • Drift: ${"%.2f".format(driftMetrics.currentDriftMeters)}m (${"%.1f".format(driftMetrics.relativeDriftPercent)}%)",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                } else {
                    // GNSS Nominal Operation Feedback
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EmeraldGps.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGps.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth().testTag("gnss_active_feedback_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(EmeraldGps.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "GNSS Locked",
                                    tint = EmeraldGps,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "GNSS CARRIER LOCK NOMINAL",
                                        color = EmeraldGps,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "±${"%.1f".format(vehicleState.accuracyMeters)}m",
                                        color = EmeraldGps,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = "18 NavIC / GPS satellites in view • Continuous IMU Kalman bias calibration active",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    selectedContainerColor: Color,
    selectedBorderColor: Color,
    selectedContentColor: Color,
    modifier: Modifier = Modifier,
    isWarning: Boolean = false,
    pulseAlpha: Float = 1f,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) selectedContainerColor else Color.Transparent,
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (isWarning) selectedBorderColor.copy(alpha = pulseAlpha) else selectedBorderColor
            )
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, Color.Transparent)
        },
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        if (isSelected) selectedContentColor.copy(alpha = 0.2f) else SpaceCardElevated,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) selectedContentColor else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    color = if (isSelected) TextPrimary else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = subtitle,
                    color = if (isSelected) selectedContentColor else TextMuted,
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
