package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiNavState
import com.example.model.DigitalTwinMetrics
import com.example.model.GnssSpoofingReport
import com.example.model.MultiHypothesisReport
import com.example.model.NavigationIntegrityReport
import com.example.model.RoadLayerState
import com.example.model.UndergroundTopologyReport
import com.example.ui.MapDisplayOptions
import com.example.ui.theme.*

@Composable
fun DigitalTwinLabCard(
    integrityReport: NavigationIntegrityReport,
    digitalTwinMetrics: DigitalTwinMetrics,
    mapOptions: MapDisplayOptions,
    onToggleMapOption: ((MapDisplayOptions) -> MapDisplayOptions) -> Unit,
    onSelectNavState: (AiNavState?) -> Unit,
    gnssSpoofingReport: GnssSpoofingReport = GnssSpoofingReport(),
    onToggleSpoofingSimulation: (Boolean) -> Unit = {},
    roadLayerState: RoadLayerState = RoadLayerState(),
    onSelectLayerOverride: (String?) -> Unit = {},
    multiHypothesisReport: MultiHypothesisReport = MultiHypothesisReport(),
    onSelectMultiHypothesisScenario: (String) -> Unit = {},
    undergroundTopologyReport: UndergroundTopologyReport = UndergroundTopologyReport(),
    onToggleUndergroundDescent: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Integrity HUD, 1: Digital Twin Benchmark, 2: State Machine Lab

    val stateColor = when (integrityReport.state) {
        AiNavState.STATE_0_FULL_GNSS -> EmeraldGps
        AiNavState.STATE_1_GNSS_DEGRADED -> AmberAccent
        AiNavState.STATE_2_MULTIPATH_SUSPECTED -> OrangeFlame
        AiNavState.STATE_3_SPOOFING_SUSPECTED -> RedJam
        AiNavState.STATE_4_GNSS_OUTAGE -> RedJam
        AiNavState.STATE_5_IMU_DEGRADING -> RedJamGlow
        AiNavState.STATE_6_MAP_RECOVERY -> CyanAccent
        AiNavState.STATE_7_VISUAL_RECOVERY -> VisualInsPath
        AiNavState.STATE_8_GNSS_REACQUISITION -> CyanAccent
        AiNavState.STATE_9_TRAJECTORY_RECONCILIATION -> EmeraldGpsGlow
    }

    // Radar pulse on current state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "statePulse"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCard.copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SpaceBorderBright.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .testTag("digital_twin_lab_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Title + State Tag + Expand toggle
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
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(stateColor.copy(alpha = pulseAlpha))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "NAVIGATION DIGITAL TWIN & INTEGRITY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextSecondary,
                            letterSpacing = 1.2.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${integrityReport.state.stateCode}: ${integrityReport.state.title}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = stateColor
                            )
                            if (integrityReport.isEvaluatorManualOverride) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = AmberAccent.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, AmberAccent)
                                ) {
                                    Text(
                                        text = "MANUAL DEMO",
                                        color = AmberAccent,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Quick error badge
                    Surface(
                        color = SpaceDarkSurface,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorder)
                    ) {
                        Text(
                            text = "±${"%.1f".format(integrityReport.estimatedErrorMeters)} m",
                            color = CyanAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // Compact summary bar when collapsed
            if (!isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SensorMiniStatus("GNSS", integrityReport.gnssTrust, if (integrityReport.gnssTrust == "TRUSTED") EmeraldGps else RedJam)
                        SensorMiniStatus("IMU", "${integrityReport.imuConfidencePercent}%", CyanAccent)
                        SensorMiniStatus("MAP", "${integrityReport.mapConfidencePercent}%", AmberAccent)
                        SensorMiniStatus("VIO", integrityReport.cameraConfidenceStatus, VisualInsPath)
                    }

                    Text(
                        text = "Tap for Lab View",
                        fontSize = 10.sp,
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Expanded view with Tab Navigation
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    // Tab row
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = SpaceDarkSurface,
                        contentColor = CyanAccent,
                        edgePadding = 4.dp,
                        indicator = {},
                        divider = {},
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, SpaceBorder, RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("INTEGRITY HUD", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("DIGITAL TWIN (4 TRAJ)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("STATE MACHINE LAB", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = { Text("SPOOFING DETECTOR", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 4,
                            onClick = { selectedTab = 4 },
                            text = { Text("3D ROAD LAYERS", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 5,
                            onClick = { selectedTab = 5 },
                            text = { Text("MULTI-HYPOTHESIS (HMM/PF)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 6,
                            onClick = { selectedTab = 6 },
                            text = { Text("UNDERGROUND TOPOLOGY", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    when (selectedTab) {
                        0 -> NavigationIntegrityDashboard(integrityReport)
                        1 -> DigitalTwinComparisonDashboard(digitalTwinMetrics, mapOptions, onToggleMapOption)
                        2 -> StateMachineLabDashboard(integrityReport.state, onSelectNavState)
                        3 -> GnssSpoofingDetectorCard(
                            report = gnssSpoofingReport,
                            onToggleSpoofingSimulation = onToggleSpoofingSimulation
                        )
                        4 -> RoadLayer3DCard(
                            roadLayerState = roadLayerState,
                            onSelectLayerOverride = onSelectLayerOverride
                        )
                        5 -> MultiHypothesisLabCard(
                            report = multiHypothesisReport,
                            onSelectScenario = onSelectMultiHypothesisScenario
                        )
                        6 -> UndergroundTopologyLabCard(
                            report = undergroundTopologyReport,
                            onToggleDescent = onToggleUndergroundDescent
                        )
                    }
                }
            }
        }
    }
}

/**
 * 1. Navigation Integrity Dashboard (Matches the user's requested ASCII layout)
 */
@Composable
private fun NavigationIntegrityDashboard(report: NavigationIntegrityReport) {
    Surface(
        color = SpaceDark,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NAVIGATION INTEGRITY",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = CyanAccent,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "AEROSPACE GRADE",
                    fontSize = 9.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }

            HorizontalDivider(color = SpaceBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

            IntegrityTelemetryRow(label = "Mode", value = report.mode, valueColor = AmberAccent)
            IntegrityTelemetryRow(
                label = "GNSS",
                value = report.gnssTrust,
                valueColor = if (report.gnssTrust == "TRUSTED") EmeraldGps else RedJam
            )
            IntegrityTelemetryRow(
                label = "IMU",
                value = "${report.imuConfidencePercent}%",
                valueColor = if (report.imuConfidencePercent >= 85) EmeraldGps else AmberAccent
            )
            IntegrityTelemetryRow(
                label = "MAP",
                value = "${report.mapConfidencePercent}%",
                valueColor = if (report.mapConfidencePercent >= 90) CyanAccent else AmberAccent
            )
            IntegrityTelemetryRow(
                label = "CAMERA",
                value = report.cameraConfidenceStatus,
                valueColor = if (report.cameraConfidenceStatus.startsWith("ACTIVE")) VisualInsPath else TextSecondary
            )
            IntegrityTelemetryRow(
                label = "BAROMETER",
                value = "${report.barometerConfidencePercent}%",
                valueColor = EmeraldGps
            )

            HorizontalDivider(color = SpaceBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Estimated error:",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "±${"%.1f".format(report.estimatedErrorMeters)} m",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = CyanAccent,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = report.state.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                fontSize = 10.5.sp,
                lineHeight = 14.sp
            )
        }
    }
}

/**
 * 2. Digital Twin Research Laboratory Comparison (Physics vs AI vs Map vs Visual vs Fusion)
 */
@Composable
private fun DigitalTwinComparisonDashboard(
    metrics: DigitalTwinMetrics,
    mapOptions: MapDisplayOptions,
    onToggleMapOption: ((MapDisplayOptions) -> MapDisplayOptions) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "LIVE RESEARCH LABORATORY: 4 SIMULTANEOUS TRAJECTORIES",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = CyanAccent,
            letterSpacing = 0.8.sp
        )
        Text(
            text = "Continual error distance benchmark against Ground Truth GNSS reference:",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            fontSize = 10.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Trajectory A: Pure INS (Physics only)
        TrajectoryComparisonRow(
            label = "Trajectory A (Pure INS / Physics)",
            errorMeters = metrics.pureInsErrorMeters,
            maxScale = (metrics.pureInsErrorMeters * 1.2f).coerceAtLeast(30f),
            color = PureDrPath,
            description = "Uncorrected double integration (quadratic drift)",
            isVisible = mapOptions.showRawDr,
            onToggleVisibility = { onToggleMapOption { it.copy(showRawDr = !it.showRawDr) } }
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Trajectory B: AI-INS (AI corrected)
        TrajectoryComparisonRow(
            label = "Trajectory B (AI-INS Corrected)",
            errorMeters = metrics.aiInsErrorMeters,
            maxScale = (metrics.pureInsErrorMeters * 1.2f).coerceAtLeast(30f),
            color = AmberAccent,
            description = "AI Speed regression + NHC + ZUPT",
            isVisible = mapOptions.showAiFusion,
            onToggleVisibility = { onToggleMapOption { it.copy(showAiFusion = !it.showAiFusion) } }
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Trajectory C: Map-INS (Map constrained)
        TrajectoryComparisonRow(
            label = "Trajectory C (Map-INS Constrained)",
            errorMeters = metrics.mapInsErrorMeters,
            maxScale = (metrics.pureInsErrorMeters * 1.2f).coerceAtLeast(30f),
            color = MapMatchedPath,
            description = "Road vector network corridor snapping",
            isVisible = mapOptions.showMapMatched,
            onToggleVisibility = { onToggleMapOption { it.copy(showMapMatched = !it.showMapMatched) } }
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Trajectory D: Visual-INS (Optical Flow)
        TrajectoryComparisonRow(
            label = "Trajectory D (Visual-INS VIO)",
            errorMeters = metrics.visualInsErrorMeters,
            maxScale = (metrics.pureInsErrorMeters * 1.2f).coerceAtLeast(30f),
            color = VisualInsPath,
            description = "Optical flow velocity feature tracking",
            isVisible = mapOptions.showVisualIns,
            onToggleVisibility = { onToggleMapOption { it.copy(showVisualIns = !it.showVisualIns) } }
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Master Fusion (NavSense-X)
        TrajectoryComparisonRow(
            label = "Master NavSense-X Fusion",
            errorMeters = metrics.fusionEstimateErrorMeters,
            maxScale = (metrics.pureInsErrorMeters * 1.2f).coerceAtLeast(30f),
            color = EmeraldGps,
            description = "Optimal multi-hypothesis EKF fusion",
            isVisible = true,
            onToggleVisibility = {}
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Stat Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = SpaceDarkSurface,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("AI Error Reduction", fontSize = 9.sp, color = TextMuted)
                    Text(
                        text = "${"%.1f".format(metrics.aiInsReductionPercent)}%",
                        color = AmberAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Surface(
                color = SpaceDarkSurface,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Fusion Error Reduction", fontSize = 9.sp, color = TextMuted)
                    Text(
                        text = "${"%.1f".format(metrics.fusionReductionPercent)}%",
                        color = EmeraldGps,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Surface(
                color = SpaceDarkSurface,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Physics Drift Rate", fontSize = 9.sp, color = TextMuted)
                    Text(
                        text = "${"%.2f".format(metrics.pureInsDriftRateMps)} m/s",
                        color = PureDrPath,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * 3. AI Navigation State Machine Laboratory (Interactive Evaluator Controls)
 */
@Composable
private fun StateMachineLabDashboard(
    currentState: AiNavState,
    onSelectNavState: (AiNavState?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EVALUATOR STATE MACHINE TRIGGER",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = CyanAccent,
                letterSpacing = 0.8.sp
            )

            // Button to return to autonomous state detection
            TextButton(
                onClick = { onSelectNavState(null) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("RESTORE AUTO", color = AmberAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Text(
            text = "Tap any of the 10 states below to demonstrate real-time fault handling during judging:",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            fontSize = 10.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal chips row for quick state switching
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AiNavState.values().forEach { state ->
                val isSelected = currentState == state
                val chipColor = when (state) {
                    AiNavState.STATE_0_FULL_GNSS -> EmeraldGps
                    AiNavState.STATE_1_GNSS_DEGRADED -> AmberAccent
                    AiNavState.STATE_2_MULTIPATH_SUSPECTED -> OrangeFlame
                    AiNavState.STATE_3_SPOOFING_SUSPECTED -> RedJam
                    AiNavState.STATE_4_GNSS_OUTAGE -> RedJam
                    AiNavState.STATE_5_IMU_DEGRADING -> RedJamGlow
                    AiNavState.STATE_6_MAP_RECOVERY -> CyanAccent
                    AiNavState.STATE_7_VISUAL_RECOVERY -> VisualInsPath
                    AiNavState.STATE_8_GNSS_REACQUISITION -> CyanAccent
                    AiNavState.STATE_9_TRAJECTORY_RECONCILIATION -> EmeraldGpsGlow
                }

                Surface(
                    onClick = { onSelectNavState(state) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) chipColor.copy(alpha = 0.25f) else SpaceDarkSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        if (isSelected) 1.5.dp else 0.8.dp,
                        if (isSelected) chipColor else SpaceBorder
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.stateCode,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) chipColor else TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = state.title,
                            fontSize = 9.sp,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Current state detail card
        Surface(
            color = SpaceDarkSurface,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ACTIVE ARCHITECTURE RESPONSE: ",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Text(
                        text = currentState.integrityMode,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberAccent
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentState.description,
                    fontSize = 11.sp,
                    color = TextPrimary,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun TrajectoryComparisonRow(
    label: String,
    errorMeters: Float,
    maxScale: Float,
    color: Color,
    description: String,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit
) {
    Surface(
        color = SpaceDarkSurface,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, SpaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${"%.1f".format(errorMeters)} m",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onToggleVisibility,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = if (isVisible) Icons.Default.Check else Icons.Default.Clear,
                            contentDescription = "Toggle trajectory visibility",
                            tint = if (isVisible) color else TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Error Bar
            val progress = (errorMeters / maxScale).coerceIn(0.01f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(SpaceDark)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 9.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun IntegrityTelemetryRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun SensorMiniStatus(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            fontSize = 10.sp,
            color = TextMuted,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            fontFamily = FontFamily.Monospace
        )
    }
}
