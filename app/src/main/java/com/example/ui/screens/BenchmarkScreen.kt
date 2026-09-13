package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BenchmarkScenario
import com.example.ui.NavViewModel
import com.example.ui.components.DigitalTwinLabCard
import com.example.ui.theme.*

@Composable
fun BenchmarkScreen(
    viewModel: NavViewModel,
    modifier: Modifier = Modifier
) {
    val benchmarkTime by viewModel.benchmarkTimeSec.collectAsState()
    val isPlaying by viewModel.isBenchmarkPlaying.collectAsState()
    val selectedIndex by viewModel.selectedScenarioIndex.collectAsState()
    val driftMetrics by viewModel.driftMetrics.collectAsState()
    val digitalTwinMetrics by viewModel.digitalTwinMetrics.collectAsState()
    val navigationIntegrity by viewModel.navigationIntegrity.collectAsState()
    val mapOptions by viewModel.mapOptions.collectAsState()
    val savedBenchmarks by viewModel.savedBenchmarks.collectAsState()

    val currentScenario = viewModel.benchmarkEngine.scenarios[selectedIndex]
    val scrollState = rememberScrollState()

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
            Icon(Icons.Default.Speed, contentDescription = "Benchmark", tint = AmberAccent)
            Column {
                Text(
                    text = "IO-VNBD Benchmark Suite",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "ISRO Evaluation Dataset • Vehicle Kinematics & GNSS Outage Verification",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        // Active Scenario Banner & Playback Controls Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = currentScenario.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberAccent
                        )
                        Text(
                            text = "${currentScenario.locationName} • ${currentScenario.totalDistanceMeters.toInt()}m Track",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    // Play / Pause Button
                    FilledIconButton(
                        onClick = { viewModel.toggleBenchmarkPlayback() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = AmberAccent,
                            contentColor = SpaceDark
                        ),
                        modifier = Modifier.size(48.dp).testTag("benchmark_play_pause_button")
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play"
                        )
                    }
                }

                // Timeline Progress Bar
                val progress = (benchmarkTime / currentScenario.totalDurationSeconds).coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = AmberAccent,
                        trackColor = SpaceCardElevated
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Time: ${"%.1f".format(benchmarkTime)}s / ${currentScenario.totalDurationSeconds}s",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Outage: ${currentScenario.outageStartSec}s - ${currentScenario.outageEndSec}s",
                            fontSize = 10.sp,
                            color = AmberAccentBright,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Playback Speed & Reset Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SpeedChip("1x", 1.0f, viewModel)
                        SpeedChip("2x", 2.0f, viewModel)
                        SpeedChip("5x", 5.0f, viewModel)
                    }

                    OutlinedButton(
                        onClick = { viewModel.resetBenchmark() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("benchmark_reset_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("RESET", fontSize = 11.sp)
                    }
                }
            }
        }

        // Real-time Navigation Integrity & Digital Twin Research Laboratory Card
        DigitalTwinLabCard(
            integrityReport = navigationIntegrity,
            digitalTwinMetrics = digitalTwinMetrics,
            mapOptions = mapOptions,
            onToggleMapOption = viewModel::updateMapOptions,
            onSelectNavState = viewModel::setSimulatedNavState
        )

        // ISRO Performance Scorecard Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceCardElevated),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (driftMetrics.benchmarkPassed) EmeraldGps.copy(alpha = 0.5f) else RedJam.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PERFORMANCE BENCHMARK SCORECARD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (driftMetrics.benchmarkPassed) EmeraldGps.copy(alpha = 0.2f) else RedJam.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (driftMetrics.benchmarkPassed) "ISRO COMPLIANT (PASS)" else "DRIFT EXCEEDED (FAIL)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (driftMetrics.benchmarkPassed) EmeraldGps else RedJam,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Metric Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ScorecardMetric(
                        label = "AI-ML Fusion Drift",
                        value = "${"%.1f".format(driftMetrics.currentDriftMeters)}m",
                        sub = "Max: ${"%.1f".format(driftMetrics.maxDriftMeters)}m",
                        color = EmeraldGps
                    )
                    ScorecardMetric(
                        label = "Relative Drift %",
                        value = "${"%.1f".format(driftMetrics.relativeDriftPercent)}%",
                        sub = "Target: < 10.0%",
                        color = if (driftMetrics.relativeDriftPercent < 10f) CyanAccent else RedJam
                    )
                    ScorecardMetric(
                        label = "Raw IMU Drift",
                        value = "${"%.1f".format(driftMetrics.rawDriftMeters)}m",
                        sub = "Unfiltered Error",
                        color = PureDrPath
                    )
                }

                HorizontalDivider(color = SpaceBorder.copy(alpha = 0.5f))

                Text(
                    text = "Benchmark Standard: Positional drift < 10% distance traveled (< 5m over 50m blackout, < 100m over 1km at 60 km/h in tunnels).",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }

        // Scenario Selector Cards
        Text(
            text = "SELECT IO-VNBD DATASET SCENARIO",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = TextSecondary
        )

        viewModel.benchmarkEngine.scenarios.forEachIndexed { index, scenario ->
            val isSelected = selectedIndex == index
            Card(
                onClick = { viewModel.loadScenario(index) },
                modifier = Modifier.fillMaxWidth().testTag("scenario_card_$index"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) SpaceCardElevated else SpaceCard
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) AmberAccent else SpaceBorder.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = scenario.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) AmberAccent else TextPrimary
                        )
                        Text(
                            text = scenario.description,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Duration: ${scenario.totalDurationSeconds}s • Blackout: ${scenario.outageEndSec - scenario.outageStartSec}s • Max Speed: ${scenario.maxSpeedKmph.toInt()} km/h",
                            fontSize = 10.sp,
                            color = NavicBlueLight,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (isSelected) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = AmberAccent)
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedChip(label: String, speed: Float, viewModel: NavViewModel) {
    Surface(
        onClick = { viewModel.setPlaybackSpeed(speed) },
        shape = RoundedCornerShape(6.dp),
        color = if (viewModel.benchmarkEngine.playbackSpeed == speed) AmberAccent else SpaceCardElevated
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (viewModel.benchmarkEngine.playbackSpeed == speed) SpaceDark else TextPrimary,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ScorecardMetric(label: String, value: String, sub: String, color: Color) {
    Column {
        Text(text = label, fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
        Text(text = value, fontSize = 16.sp, color = color, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(text = sub, fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
    }
}
