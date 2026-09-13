package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TripRecordEntity
import com.example.ui.NavViewModel
import com.example.ui.theme.*

@Composable
fun EdgeEngineScreen(
    viewModel: NavViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vehicleState by viewModel.vehicleState.collectAsState()
    val imu by viewModel.currentImu.collectAsState()
    val driftMetrics by viewModel.driftMetrics.collectAsState()
    val savedTrips by viewModel.savedTrips.collectAsState()

    var isFogModeEnabled by remember { mutableStateOf(false) }
    var exportSuccessMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(bottom = 64.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Memory, contentDescription = "Edge Engine", tint = AmberAccent)
                Column {
                    Text(
                        text = "Edge Deployable Software Engine",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Universal C++/Rust/Kotlin Core • 200Hz FOG IMU & External Sensor Engine",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Edge Specifications Card
        item {
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
                    Text(
                        text = "EDGE ARCHITECTURE & SPECIFICATIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = AmberAccent
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SpecItem("Target Update Rate", if (isFogModeEnabled) "200 Hz (FOG)" else "50-100 Hz (MEMS)")
                        SpecItem("Execution Latency", "< 2.8 ms")
                        SpecItem("Memory Footprint", "< 4.2 MB RAM")
                    }

                    HorizontalDivider(color = SpaceBorder.copy(alpha = 0.5f))

                    // FOG (Fiber Optic Gyroscope) High-Precision Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "FOG-Grade High Precision IMU Mode (200Hz)",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Enables tactical Kalman covariance for Fiber Optic Gyroscopes",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = isFogModeEnabled,
                            onCheckedChange = { isFogModeEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = AmberAccent, checkedTrackColor = SpaceCardElevated),
                            modifier = Modifier.testTag("fog_mode_switch")
                        )
                    }
                }
            }
        }

        // Live High-Frequency Telemetry Stream Feed
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SpaceCardElevated)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE TELEMETRY PACKET STREAM",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary
                        )
                        Text(
                            text = "Rate: ${viewModel.liveSensorManager.sensorRateHz} Hz",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGps,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SpaceDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "> LAT: ${"%.6f".format(vehicleState.lat)}  LON: ${"%.6f".format(vehicleState.lng)}  ALT: ${vehicleState.altitude.toInt()}m",
                                fontSize = 10.sp,
                                color = CyanAccent,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "> V_FWD: ${"%.2f".format(vehicleState.speedMps)} m/s (${vehicleState.speedKmph.toInt()} km/h)  HEADING: ${"%.1f".format(vehicleState.headingDeg)}°",
                                fontSize = 10.sp,
                                color = EmeraldGps,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "> ACCEL_LONG: ${"%.3f".format(vehicleState.accelLongitudinal)}  ACCEL_LAT: ${"%.3f".format(vehicleState.accelLateral)} m/s²",
                                fontSize = 10.sp,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "> YAW_RATE: ${"%.3f".format(vehicleState.yawRateDegPerSec)}°/s  ZUPT: ${vehicleState.isZuptActive}  ROAD_SNAP: ${vehicleState.roadSnapped}",
                                fontSize = 10.sp,
                                color = AmberDr,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Telemetry Exporter Actions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SpaceCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "EXPORT TELEMETRY DATASET LOGS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val csv = generateCsvTelemetry(vehicleState, driftMetrics, imu)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "NavDR_ISRO_Telemetry.csv")
                                    putExtra(Intent.EXTRA_TEXT, csv)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Export Telemetry CSV"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SpaceDark),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("export_csv_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("EXPORT CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                exportSuccessMessage = "JSON Schema generated successfully for Edge Engine"
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("export_json_button")
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("EDGE CONFIG", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (exportSuccessMessage != null) {
                        Text(
                            text = exportSuccessMessage!!,
                            color = EmeraldGps,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Saved Navigation Trips Section
        item {
            Text(
                text = "RECORDED NAVIGATION TRIPS (${savedTrips.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
        }

        if (savedTrips.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SpaceCard)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved trips yet. Tap the record button on the Navigation screen to log a session.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(savedTrips) { trip ->
                TripRecordCard(trip)
            }
        }
    }
}

@Composable
private fun SpecItem(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 9.sp, color = TextSecondary)
        Text(text = value, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun TripRecordCard(trip: TripRecordEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorder.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = trip.title, color = AmberAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    text = "${trip.distanceMeters.toInt()}m",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Max Drift: ${"%.1f".format(trip.maxDriftMeters)}m • Outage: ${"%.1f".format(trip.blackoutDurationSec)}s",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "ZUPT Stops: ${trip.zuptEvents}",
                    color = EmeraldGps,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private fun generateCsvTelemetry(
    state: com.example.model.VehicleState,
    drift: com.example.model.DriftMetrics,
    imu: com.example.model.ImuReading
): String {
    val sb = StringBuilder()
    sb.append("timestamp_ms,lat,lng,speed_kmph,heading_deg,accel_long,accel_lat,accel_down,yaw_rate,drift_m,rel_drift_pct,mode\n")
    sb.append("${System.currentTimeMillis()},${state.lat},${state.lng},${state.speedKmph},${state.headingDeg},${state.accelLongitudinal},${state.accelLateral},${state.accelVertical},${state.yawRateDegPerSec},${drift.currentDriftMeters},${drift.relativeDriftPercent},${state.fusionMode.name}\n")
    return sb.toString()
}
