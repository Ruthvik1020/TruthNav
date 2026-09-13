package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RoadLayerState
import com.example.ui.theme.*

@Composable
fun RoadLayer3DCard(
    roadLayerState: RoadLayerState,
    onSelectLayerOverride: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }

    val levelBadgeColor = when (roadLayerState.levelCode) {
        "L2" -> Color(0xFF64B5F6) // Sky blue for flyover
        "L1" -> CyanAccent        // Cyan for ramp
        "L0" -> EmeraldGps        // Emerald for surface
        "L-1" -> AmberAccent      // Amber for underpass
        "B1" -> OrangeFlame       // Orange for tunnel
        "B2" -> VisualInsPath     // Violet for deep underground B2
        else -> CyanAccent
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("road_layer_3d_card")
            .border(1.dp, Color(0xFF243447), RoundedCornerShape(16.dp)),
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
                            .background(levelBadgeColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "3D Road Layer",
                            tint = levelBadgeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "3D ROAD LAYER POSITIONING",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "LEVEL: ${roadLayerState.levelCode}",
                                style = MaterialTheme.typography.titleMedium,
                                color = levelBadgeColor,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${roadLayerState.detectedStructure})",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(levelBadgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${roadLayerState.confidencePercent}% CONF",
                            style = MaterialTheme.typography.labelSmall,
                            color = levelBadgeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
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
                    // 4 Core User Specification Metrics in 2x2 Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Level
                        LayerMetricBox(
                            label = "LEVEL",
                            value = roadLayerState.levelCode,
                            subtext = roadLayerState.layerName.take(18),
                            accentColor = levelBadgeColor,
                            modifier = Modifier.weight(1f)
                        )
                        // Altitude
                        val altSign = if (roadLayerState.relativeAltitudeMeters >= 0) "+" else ""
                        LayerMetricBox(
                            label = "ALTITUDE",
                            value = "$altSign${"%.1f".format(roadLayerState.relativeAltitudeMeters)} m",
                            subtext = "MSL: ${"%.0f".format(roadLayerState.absoluteAltitudeMslMeters)} m",
                            accentColor = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Confidence
                        LayerMetricBox(
                            label = "CONFIDENCE",
                            value = "${roadLayerState.confidencePercent}%",
                            subtext = "Baro + Kinematic",
                            accentColor = EmeraldGps,
                            modifier = Modifier.weight(1f)
                        )
                        // Vertical Rate
                        val rateSign = if (roadLayerState.verticalVelocityMps >= 0) "+" else ""
                        LayerMetricBox(
                            label = "VERTICAL RATE",
                            value = "$rateSign${"%.2f".format(roadLayerState.verticalVelocityMps)} m/s",
                            subtext = "Pitch: ${"%.1f".format(roadLayerState.pitchAngleDeg)}°",
                            accentColor = if (Math.abs(roadLayerState.verticalVelocityMps) > 0.3f) AmberAccent else TextMuted,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Barometer & Kinematic Sensor Context
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SpaceCardElevated)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Barometer",
                                tint = CyanAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Barometer: ${"%.1f".format(roadLayerState.atmosphericPressureHpa)} hPa",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = "Baseline: ${"%.1f".format(roadLayerState.baselinePressureHpa)} hPa",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Interactive Layer Simulation Switcher for Demonstrations
                    Text(
                        text = "DEMONSTRATION: MULTILAYER STACKED ROADS & TUNNELS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val layers = listOf(
                            null to "Auto",
                            "L2" to "L2: Flyover (+12m)",
                            "L1" to "L1: Ramp (+6.5m)",
                            "L0" to "L0: Surface (0m)",
                            "L-1" to "L-1: Underpass (-6m)",
                            "B1" to "B1: Tunnel (-9m)",
                            "B2" to "B2: Deep Underground (-15m)"
                        )

                        layers.forEach { (code, label) ->
                            val isSelected = code == roadLayerState.levelCode
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectLayerOverride(code) },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = levelBadgeColor,
                                    selectedLabelColor = SpaceDark,
                                    containerColor = SpaceCardElevated,
                                    labelColor = TextMuted
                                ),
                                modifier = Modifier.testTag("layer_chip_${code ?: "auto"}")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LayerMetricBox(
    label: String,
    value: String,
    subtext: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SpaceCardElevated)
            .padding(8.dp)
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = accentColor,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                fontSize = 15.sp
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}
