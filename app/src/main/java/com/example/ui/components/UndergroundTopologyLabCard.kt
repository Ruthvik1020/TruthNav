package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import com.example.model.UndergroundTopologyReport
import com.example.ui.theme.*

@Composable
fun UndergroundTopologyLabCard(
    report: UndergroundTopologyReport,
    onToggleDescent: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("underground_topology_lab_card")
            .border(1.dp, Color(0xFF33251B), RoundedCornerShape(16.dp)),
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
                            .background(OrangeFlame.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Subway,
                            contentDescription = "Underground Topology Engine",
                            tint = OrangeFlame,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "UNDERGROUND TOPOLOGY ENGINE",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Motion History to Road Topology Reasoning",
                            color = OrangeFlame,
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
                    // Philosophy Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF201610))
                            .border(1.dp, OrangeFlame.copy(alpha = 0.40f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountTree,
                                    contentDescription = null,
                                    tint = OrangeFlame,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "MOTION-HISTORY TO ROAD-TOPOLOGY",
                                    color = OrangeFlame,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "“Don't match a point to a road. Match a motion history to a road topology.”",
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Heading + Barometric Descent + Map Topology + Turn History + Road Connectivity + IMU Trajectory  →  BOTTOM ROAD",
                                color = OrangeFlame.copy(alpha = 0.90f),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3D Stacked Road Topology Visualizer
                    Text(
                        text = "3D STACKED ROAD NETWORK TOPOLOGY",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF131A24))
                            .border(1.dp, Color(0xFF232D3B), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        RoadLevelIndicatorRow(
                            label = "Road A (Surface)",
                            level = "L0",
                            nominalAlt = "0.0m",
                            status = if (report.isUndergroundLocked) "REJECTED (Collinear Lat/Lng)" else "MATCHED",
                            isLocked = !report.isUndergroundLocked,
                            color = EmeraldGps
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        RoadLevelIndicatorRow(
                            label = "Road B (Parallel Service)",
                            level = "L0",
                            nominalAlt = "0.0m",
                            status = "Ingress Ramp Source",
                            isLocked = false,
                            color = CyanAccent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 28.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = AmberAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Tunnel Portal Descent Ramp (-7.2% grade, Δz = -12.4m)",
                                color = AmberAccent,
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        RoadLevelIndicatorRow(
                            label = "Road C (Subterranean Tunnel)",
                            level = "B1",
                            nominalAlt = "-12.4m",
                            status = if (report.isUndergroundLocked) "LOCKED (BOTTOM ROAD)" else "CANDIDATE",
                            isLocked = report.isUndergroundLocked,
                            color = OrangeFlame
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 6-Factor Reasoning Chain
                    Text(
                        text = "6-FACTOR TOPOLOGY REASONING CHAIN",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    report.reasoningChain.forEach { stepText ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (report.isUndergroundLocked) EmeraldGps else AmberAccent,
                                modifier = Modifier
                                    .size(14.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stepText,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Verdict Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (report.isUndergroundLocked)
                                    OrangeFlame.copy(alpha = 0.18f)
                                else
                                    EmeraldGps.copy(alpha = 0.18f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (report.isUndergroundLocked) OrangeFlame else EmeraldGps,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (report.isUndergroundLocked) "VERDICT: LOCKED TO BOTTOM ROAD" else "VERDICT: SURFACE GRADE NOMINAL",
                                    color = if (report.isUndergroundLocked) OrangeFlame else EmeraldGps,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${report.lockedRoadName} (${report.lockedLayerLevel}) · ${report.confidencePercent}% Confidence",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Button(
                                onClick = { onToggleDescent(!report.isUndergroundLocked) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (report.isUndergroundLocked) Color(0xFF263850) else OrangeFlame
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = if (report.isUndergroundLocked) "Ascend to L0" else "Descend to B1",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
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
private fun RoadLevelIndicatorRow(
    label: String,
    level: String,
    nominalAlt: String,
    status: String,
    isLocked: Boolean,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isLocked) color.copy(alpha = 0.20f) else Color.Transparent)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = level,
                    color = Color.Black,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = if (isLocked) FontWeight.Bold else FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Nominal Elevation: $nominalAlt",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Text(
            text = status,
            color = if (isLocked) color else Color.White.copy(alpha = 0.5f),
            fontSize = 9.5.sp,
            fontWeight = if (isLocked) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace
        )
    }
}
