package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import com.example.model.LocalizationHypothesis
import com.example.model.MultiHypothesisReport
import com.example.model.RoadStructureType
import com.example.ui.theme.*

@Composable
fun MultiHypothesisLabCard(
    report: MultiHypothesisReport,
    onSelectScenario: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("multi_hypothesis_lab_card")
            .border(1.dp, Color(0xFF23354E), RoundedCornerShape(16.dp)),
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
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(CyanAccent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AltRoute,
                            contentDescription = "Multi-Hypothesis Localization",
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "MULTI-HYPOTHESIS LOCALIZATION",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Particle Filter + HMM + Bayesian Scoring",
                            color = CyanAccent,
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand/Collapse",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // Core Principle Callout
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F1E33))
                            .border(1.dp, CyanAccent.copy(alpha = 0.40f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ROAD-BEARING PARTICLE FILTERING",
                                    color = CyanAccent,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Never immediately declare 'The vehicle is here.' Instead, maintain concurrent road hypotheses with Bayesian scoring to solve parallel roads, intersections, flyovers, and tunnel forks.",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Real-Time Probabilistic Candidate Cards
                    Text(
                        text = "CONCURRENT ROAD HYPOTHESES (P(H_k | Z_1:t))",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    report.candidates.forEach { cand ->
                        CandidateRow(cand = cand)
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Metrics Strip: Entropy, Active Particles, Ambiguity Context
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricCardMini(
                            label = "SHANNON ENTROPY",
                            value = "${"%.2f".format(report.shannonEntropy)} bits",
                            subtext = if (report.shannonEntropy > 0.8f) "High Ambiguity" else "Consensus",
                            color = if (report.shannonEntropy > 0.8f) AmberAccent else EmeraldGps,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCardMini(
                            label = "PARTICLES",
                            value = "${report.totalActiveParticles} Swarm",
                            subtext = "Bearing Constrained",
                            color = CyanAccent,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCardMini(
                            label = "LEADING P",
                            value = "${(report.leadingHypothesis.probability * 100).toInt()}%",
                            subtext = "Rank #1 Choice",
                            color = EmeraldGps,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Interactive Nightmare Scenario Preset Bar
                    Text(
                        text = "TRIGGER AMBIGUITY TEST SCENARIOS",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScenarioPresetButton(
                            label = "Parallel Service Road",
                            subtext = "P: 0.61 vs 0.24 vs 0.15",
                            isSelected = report.ambiguityContext.contains("PARALLEL", ignoreCase = true),
                            onClick = { onSelectScenario("PARALLEL_SERVICE_ROAD_SPLIT") }
                        )
                        ScenarioPresetButton(
                            label = "Flyover Ramp Ingress",
                            subtext = "Deck (+12m) P: 0.78",
                            isSelected = report.ambiguityContext.contains("FLYOVER", ignoreCase = true),
                            onClick = { onSelectScenario("STACKED_FLYOVER_BIFURCATION") }
                        )
                        ScenarioPresetButton(
                            label = "Tunnel Portal Fork",
                            subtext = "Bottom Road P: 0.84",
                            isSelected = report.ambiguityContext.contains("TUNNEL", ignoreCase = true),
                            onClick = { onSelectScenario("SUBTERRANEAN_TUNNEL_PORTAL") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CandidateRow(cand: LocalizationHypothesis) {
    val animatedProb by animateFloatAsState(
        targetValue = cand.probability,
        label = "candidate_prob"
    )

    val candidateColor = when {
        cand.isLeading -> EmeraldGps
        cand.rank == 2 -> AmberAccent
        else -> Color(0xFF64B5F6)
    }

    val structureIcon = when (cand.structureType) {
        RoadStructureType.SURFACE_MAIN_CARRIAGEWAY -> Icons.Default.DirectionsCar
        RoadStructureType.PARALLEL_SERVICE_ROAD -> Icons.Default.AltRoute
        RoadStructureType.ELEVATED_FLYOVER_DECK -> Icons.Default.FlightTakeoff
        RoadStructureType.SUBTERRANEAN_TUNNEL_ROAD -> Icons.Default.Subway
        else -> Icons.Default.Navigation
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF142234))
            .border(
                width = if (cand.isLeading) 1.5.dp else 1.dp,
                color = if (cand.isLeading) candidateColor.copy(alpha = 0.8f) else Color(0xFF22364E),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = structureIcon,
                        contentDescription = null,
                        tint = candidateColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = cand.roadName,
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = if (cand.isLeading) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }

                // Probability Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(candidateColor.copy(alpha = 0.20f))
                        .border(1.dp, candidateColor.copy(alpha = 0.60f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "P = ${"%.2f".format(cand.probability)}",
                        color = candidateColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Probability Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF0D1826))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProb)
                            .clip(RoundedCornerShape(3.dp))
                            .background(candidateColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Candidate Telemetry Footprint
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Lat. Offset: ${"%.1f".format(cand.lateralOffsetMeters)}m · Δθ: ${"%.1f".format(cand.headingDeltaDeg)}°",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${cand.particleCount} particles · L: ${"%.2f".format(cand.bayesianLikelihood)}",
                    color = candidateColor.copy(alpha = 0.9f),
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun MetricCardMini(
    label: String,
    value: String,
    subtext: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF132032))
            .border(1.dp, Color(0xFF203248), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtext,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun ScenarioPresetButton(
    label: String,
    subtext: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) CyanAccent.copy(alpha = 0.22f) else Color(0xFF132032))
            .border(
                width = 1.dp,
                color = if (isSelected) CyanAccent else Color(0xFF263850),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column {
            Text(
                text = label,
                color = if (isSelected) CyanAccent else Color.White,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtext,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
