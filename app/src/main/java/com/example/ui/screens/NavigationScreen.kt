package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.map.MapLayerType
import com.example.model.FusionMode
import com.example.ui.DataSourceMode
import com.example.ui.NavViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.voice.VoiceAssistantState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationScreen(
    viewModel: NavViewModel,
    modifier: Modifier = Modifier
) {
    val vehicleState by viewModel.vehicleState.collectAsState()
    val currentImu by viewModel.currentImu.collectAsState()
    val currentGnss by viewModel.currentGnss.collectAsState()
    val driftMetrics by viewModel.driftMetrics.collectAsState()
    val digitalTwinMetrics by viewModel.digitalTwinMetrics.collectAsState()
    val navigationIntegrity by viewModel.navigationIntegrity.collectAsState()
    val gnssSpoofingReport by viewModel.gnssSpoofingReport.collectAsState()
    val roadLayerState by viewModel.roadLayerState.collectAsState()
    val multiHypothesisReport by viewModel.multiHypothesisReport.collectAsState()
    val undergroundTopologyReport by viewModel.undergroundTopologyReport.collectAsState()
    val isJamming by viewModel.isManualJammingActive.collectAsState()
    val mapOptions by viewModel.mapOptions.collectAsState()
    val dataSourceMode by viewModel.dataSourceMode.collectAsState()
    val isRecording by viewModel.isRecordingTrip.collectAsState()
    val destination by viewModel.destination.collectAsState()
    val sensorAvailability by viewModel.liveSensorManager.sensorAvailability.collectAsState()
    val tileRepaintTrigger by viewModel.tileRepaintTrigger.collectAsState()
    val voiceState by viewModel.voiceAssistant.assistantState.collectAsState()
    val isVoiceMuted by viewModel.voiceAssistant.isVoiceMuted.collectAsState()
    val lastVoiceMessage by viewModel.voiceAssistant.lastSpokenMessage.collectAsState()

    var showOptionsDropdown by remember { mutableStateOf(false) }
    var showLayerDropdown by remember { mutableStateOf(false) }

    // Pulsing animation for dynamic notch indicator
    val infiniteTransition = rememberInfiniteTransition(label = "notch_pulse")
    val notchPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "notch_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceDark)
    ) {
        // =========================================================================
        // 1. HERO MAP CANVAS (FULL SCREEN VIEWPORT)
        // =========================================================================
        VectorMapCanvas(
            vehicleState = vehicleState,
            groundTruthTrail = viewModel.fusionEngine.groundTruthTrail,
            rawDrTrail = viewModel.fusionEngine.rawDrTrail,
            aiInsTrail = viewModel.fusionEngine.aiInsTrail,
            mapMatchedTrail = viewModel.fusionEngine.mapMatchedTrail,
            visualTrail = viewModel.fusionEngine.trajectoryDVisualTrail,
            roadSegments = viewModel.benchmarkEngine.currentScenario.roadSegments,
            destination = destination,
            mapOptions = mapOptions,
            spoofingReport = gnssSpoofingReport,
            multiHypothesisReport = multiHypothesisReport,
            undergroundReport = undergroundTopologyReport,
            terrainTileEngine = viewModel.terrainTileEngine,
            tileRepaintTrigger = tileRepaintTrigger,
            isVoiceSpeaking = voiceState == VoiceAssistantState.SPEAKING,
            isVoiceMuted = isVoiceMuted,
            onToggleFollow = {
                viewModel.updateMapOptions { it.copy(followVehicle = !it.followVehicle) }
            },
            onChangeMapLayer = { layer ->
                viewModel.updateMapOptions { it.copy(mapLayerType = layer) }
            },
            onSpeakStatus = {
                viewModel.speakCurrentStatus()
            },
            onToggleVoiceMute = {
                viewModel.voiceAssistant.toggleMute()
            },
            onRelocateVehicle = { lat, lng ->
                viewModel.relocateVehicle(lat, lng)
            },
            onSetDestination = { lat, lng ->
                viewModel.setDestination(lat, lng)
            },
            onClearDestination = {
                viewModel.clearDestination()
            },
            onCurrentLocationClicked = {
                viewModel.syncWithCurrentDeviceLocation()
            },
            modifier = Modifier.fillMaxSize()
        )

        // =========================================================================
        // 2. DYNAMIC AEROSPACE NOTCH & TOP ACTION BAR (INSET-SAFE & NON-OVERLAPPING)
        // =========================================================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Dynamic Island Aerospace Notch Pill
            Surface(
                onClick = { viewModel.speakCurrentStatus() },
                shape = RoundedCornerShape(20.dp),
                color = SpaceDarkSurface.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(
                    1.2.dp,
                    when (vehicleState.fusionMode) {
                        FusionMode.GNSS_AIDED -> EmeraldGps.copy(alpha = 0.8f)
                        FusionMode.DEAD_RECKONING_AI -> AmberDr.copy(alpha = 0.8f)
                        FusionMode.GNSS_OUTAGE_JAMMED -> RedJam.copy(alpha = notchPulseAlpha)
                        FusionMode.CALIBRATING -> CyanAccent.copy(alpha = 0.8f)
                    }
                ),
                shadowElevation = 8.dp,
                modifier = Modifier.testTag("dynamic_aerospace_notch")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                when (vehicleState.fusionMode) {
                                    FusionMode.GNSS_AIDED -> EmeraldGps
                                    FusionMode.DEAD_RECKONING_AI -> AmberDr
                                    FusionMode.GNSS_OUTAGE_JAMMED -> RedJam
                                    FusionMode.CALIBRATING -> CyanAccent
                                },
                                CircleShape
                            )
                    )
                    Text(
                        text = "ISRO NavIC",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(10.dp)
                            .background(SpaceBorder)
                    )
                    Text(
                        text = when (vehicleState.fusionMode) {
                            FusionMode.GNSS_AIDED -> "10 Hz LOCKED"
                            FusionMode.DEAD_RECKONING_AI -> "AI-DR FUSION"
                            FusionMode.GNSS_OUTAGE_JAMMED -> "SIM BLACKOUT"
                            FusionMode.CALIBRATING -> "CALIBRATING"
                        },
                        color = when (vehicleState.fusionMode) {
                            FusionMode.GNSS_AIDED -> EmeraldGps
                            FusionMode.DEAD_RECKONING_AI -> AmberDr
                            FusionMode.GNSS_OUTAGE_JAMMED -> RedJam
                            FusionMode.CALIBRATING -> CyanAccent
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Top Action Controls Row: Sensor Pill (Left) & Tactical Actions (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Data Source Badge
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SpaceCard.copy(alpha = 0.90f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (dataSourceMode == DataSourceMode.LIVE_HARDWARE_SENSORS) Icons.Default.Sensors else Icons.Default.Speed,
                            contentDescription = null,
                            tint = if (dataSourceMode == DataSourceMode.LIVE_HARDWARE_SENSORS) EmeraldGps else CyanAccent,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (dataSourceMode == DataSourceMode.LIVE_HARDWARE_SENSORS) "LIVE GPS/IMU" else "IO-VNBD BENCH",
                            color = TextSecondary,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Right Action Pill Group: Voice + Layer + Options
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Voice Mute Button
                    Surface(
                        onClick = { viewModel.voiceAssistant.toggleMute() },
                        shape = RoundedCornerShape(14.dp),
                        color = SpaceCard.copy(alpha = 0.90f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isVoiceMuted) RedJam.copy(alpha = 0.7f) else EmeraldGps.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.testTag("voice_mute_toggle_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isVoiceMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (isVoiceMuted) "Unmute Voice" else "Mute Voice",
                                tint = if (isVoiceMuted) RedJam else EmeraldGps,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (isVoiceMuted) "MUTED" else "VOICE",
                                color = if (isVoiceMuted) RedJam else EmeraldGps,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Map Layer Selector Pill + Dropdown
                    Box {
                        Surface(
                            onClick = { showLayerDropdown = !showLayerDropdown },
                            shape = RoundedCornerShape(14.dp),
                            color = SpaceCard.copy(alpha = 0.90f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.7f)),
                            modifier = Modifier.testTag("map_layer_pill_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = when (mapOptions.mapLayerType) {
                                        MapLayerType.TERRAIN_TOPO -> Icons.Default.Terrain
                                        MapLayerType.SATELLITE -> Icons.Default.SatelliteAlt
                                        MapLayerType.STREET_MAP -> Icons.Default.Map
                                    },
                                    contentDescription = "Map Style",
                                    tint = CyanAccent,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = when (mapOptions.mapLayerType) {
                                        MapLayerType.TERRAIN_TOPO -> "TOPO"
                                        MapLayerType.SATELLITE -> "SATELLITE"
                                        MapLayerType.STREET_MAP -> "STREET"
                                    },
                                    color = CyanAccent,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                                Icon(
                                    imageVector = if (showLayerDropdown) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }

                        // Layer dropdown menu
                        DropdownMenu(
                            expanded = showLayerDropdown,
                            onDismissRequest = { showLayerDropdown = false },
                            modifier = Modifier.background(SpaceDarkSurface)
                        ) {
                            MapLayerType.entries.forEach { layer ->
                                val isSelected = mapOptions.mapLayerType == layer
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = layer.displayName,
                                                color = if (isSelected) CyanAccent else TextPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = layer.subtitle,
                                                color = TextMuted,
                                                fontSize = 8.5.sp
                                            )
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = when (layer) {
                                                MapLayerType.TERRAIN_TOPO -> Icons.Default.Terrain
                                                MapLayerType.SATELLITE -> Icons.Default.SatelliteAlt
                                                MapLayerType.STREET_MAP -> Icons.Default.Map
                                            },
                                            contentDescription = null,
                                            tint = if (isSelected) CyanAccent else TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    onClick = {
                                        viewModel.updateMapOptions { it.copy(mapLayerType = layer) }
                                        showLayerDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Tactical Options Dropdown Button
                    Surface(
                        onClick = { showOptionsDropdown = !showOptionsDropdown },
                        shape = RoundedCornerShape(14.dp),
                        color = if (showOptionsDropdown) AmberAccent else SpaceCard.copy(alpha = 0.90f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberAccent),
                        modifier = Modifier.testTag("dashboard_dropdown_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (showOptionsDropdown) Icons.Default.Close else Icons.Default.Tune,
                                contentDescription = "Tactical Options Menu",
                                tint = if (showOptionsDropdown) SpaceDark else AmberAccent,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (showOptionsDropdown) "CLOSE" else "OPTIONS",
                                color = if (showOptionsDropdown) SpaceDark else AmberAccent,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Real-time Navigation Integrity & Digital Twin Research Laboratory Card
            DigitalTwinLabCard(
                integrityReport = navigationIntegrity,
                digitalTwinMetrics = digitalTwinMetrics,
                mapOptions = mapOptions,
                onToggleMapOption = { transform -> viewModel.updateMapOptions(transform) },
                onSelectNavState = { state -> viewModel.setSimulatedNavState(state) },
                gnssSpoofingReport = gnssSpoofingReport,
                onToggleSpoofingSimulation = { active -> viewModel.toggleSimulatedSpoofingAttack(active) },
                roadLayerState = roadLayerState,
                onSelectLayerOverride = { layer -> viewModel.setTargetRoadLayer(layer) },
                multiHypothesisReport = multiHypothesisReport,
                onSelectMultiHypothesisScenario = { preset -> viewModel.setMultiHypothesisScenario(preset) },
                undergroundTopologyReport = undergroundTopologyReport,
                onToggleUndergroundDescent = { active -> viewModel.toggleUndergroundDescent(active) }
            )
        }

        // =========================================================================
        // 3. FLOATING BOTTOM COCKPIT HUD (CLEAN, SPACIOUS & NON-OVERLAPPING)
        // =========================================================================
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SpaceDarkSurface.copy(alpha = 0.95f),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, SpaceBorderBright.copy(alpha = 0.5f)),
            shadowElevation = 10.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Top telemetry row: Velocity & Heading (Left) | Coordinates & Hint (Center) | Drift (Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Velocity & Heading
                    Column {
                        Text(
                            text = "VELOCITY • HEADING",
                            fontSize = 8.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = "${vehicleState.speedKmph.toInt()} km/h",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${vehicleState.headingDeg.toInt()}°",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Center: 3D Geodetic Lat/Lng, Layer Level & Elevation
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CyanAccent.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, CyanAccent)
                            ) {
                                Text(
                                    text = vehicleState.roadLayerLevel,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = CyanAccent,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            val altSign = if (vehicleState.relativeAltitudeMeters >= 0) "+" else ""
                            Text(
                                text = "$altSign${"%.1f".format(vehicleState.relativeAltitudeMeters)}m · ${"%.4f".format(vehicleState.lat)}°N, ${"%.4f".format(vehicleState.lng)}°E",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        val rateSign = if (vehicleState.verticalVelocityMps >= 0) "+" else ""
                        Text(
                            text = "Vv: $rateSign${"%.2f".format(vehicleState.verticalVelocityMps)} m/s · Long press map to set target",
                            fontSize = 7.5.sp,
                            color = AmberAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Right: Drift Confidence Metric
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "DRIFT ERROR",
                            fontSize = 8.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${"%.1f".format(driftMetrics.currentDriftMeters)}m",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = if (driftMetrics.benchmarkPassed) EmeraldGps else AmberAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                HorizontalDivider(color = SpaceBorder.copy(alpha = 0.5f))

                // Bottom row: Quick GNSS / Blackout Simulator Toggle Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (isJamming) RedJam else EmeraldGps, CircleShape)
                        )
                        Text(
                            text = if (isJamming) "GNSS JAMMED / DEGRADED" else "GNSS SATELLITE FIX ACTIVE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isJamming) RedJam else EmeraldGps,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Surface(
                        onClick = { viewModel.toggleManualJamming() },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isJamming) RedJam.copy(alpha = 0.25f) else EmeraldGps.copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isJamming) RedJam else EmeraldGps
                        ),
                        modifier = Modifier.testTag("quick_jamming_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isJamming) Icons.Default.SignalCellularOff else Icons.Default.GpsFixed,
                                contentDescription = null,
                                tint = if (isJamming) RedJam else EmeraldGps,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (isJamming) "STOP BLACKOUT" else "SIMULATE BLACKOUT",
                                color = if (isJamming) RedJam else EmeraldGps,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 4. DROPDOWN / EXPANDABLE TACTICAL OPTIONS MODAL (APPEALING MODERN THEME)
        // =========================================================================
        AnimatedVisibility(
            visible = showOptionsDropdown,
            enter = slideInVertically { -it / 3 } + fadeIn(),
            exit = slideOutVertically { -it / 3 } + fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .statusBarsPadding()
                .padding(top = 50.dp, start = 10.dp, end = 10.dp, bottom = 10.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SpaceDarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, CyanAccent.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("tactical_options_dropdown_panel")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Bar with Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(CyanAccent.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                            }
                            Column {
                                Text(
                                    text = "TACTICAL MISSION OPTIONS",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "ISRO NavIC · Sensor & Fusion Controls",
                                    color = TextSecondary,
                                    fontSize = 8.5.sp
                                )
                            }
                        }
                        IconButton(
                            onClick = { showOptionsDropdown = false },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = AmberAccent)
                        }
                    }

                    HorizontalDivider(color = SpaceBorderBright.copy(alpha = 0.4f))

                    // 1. Data Source Switcher (Live Smartphone Sensors vs Benchmark)
                    Text(
                        text = "POSITION DATA SOURCE",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { viewModel.setDataSourceMode(DataSourceMode.LIVE_HARDWARE_SENSORS) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (dataSourceMode == DataSourceMode.LIVE_HARDWARE_SENSORS) EmeraldGps.copy(alpha = 0.15f) else SpaceCard,
                            border = androidx.compose.foundation.BorderStroke(
                                1.2.dp,
                                if (dataSourceMode == DataSourceMode.LIVE_HARDWARE_SENSORS) EmeraldGps else SpaceBorder
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Sensors, contentDescription = null, tint = EmeraldGps, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("LIVE HARDWARE", color = EmeraldGps, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    Text("Device GPS + IMU", color = TextSecondary, fontSize = 8.sp)
                                }
                            }
                        }

                        Surface(
                            onClick = { viewModel.setDataSourceMode(DataSourceMode.IO_VNBD_BENCHMARK_SIMULATOR) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (dataSourceMode == DataSourceMode.IO_VNBD_BENCHMARK_SIMULATOR) AmberAccent.copy(alpha = 0.15f) else SpaceCard,
                            border = androidx.compose.foundation.BorderStroke(
                                1.2.dp,
                                if (dataSourceMode == DataSourceMode.IO_VNBD_BENCHMARK_SIMULATOR) AmberAccent else SpaceBorder
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("BENCHMARK", color = AmberAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    Text("IO-VNBD Ground Truth", color = TextSecondary, fontSize = 8.sp)
                                }
                            }
                        }
                    }

                    // 2. GNSS Mode Toggle Bar & Blackout Simulator
                    GnssModeToggleBar(
                        vehicleState = vehicleState,
                        driftMetrics = driftMetrics,
                        isBlackoutActive = isJamming,
                        onToggleMode = { shouldBlackout ->
                            viewModel.setManualJamming(shouldBlackout)
                        }
                    )

                    // 3. Hardware Sensors & Gimbal Telemetry HUD
                    TelemetryHud(
                        vehicleState = vehicleState,
                        driftMetrics = driftMetrics,
                        imu = currentImu,
                        isManualJamming = isJamming,
                        onToggleJamming = { viewModel.toggleManualJamming() }
                    )

                    // 4. Voice Assistant Quick Status Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SpaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(if (isVoiceMuted) RedJam.copy(alpha = 0.15f) else CyanAccent.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isVoiceMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = if (isVoiceMuted) RedJam else CyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "VOICE NAVIGATION ASSISTANT",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (isVoiceMuted) "Audio guidance muted." else lastVoiceMessage,
                                    fontSize = 10.5.sp,
                                    color = TextPrimary
                                )
                            }
                            Button(
                                onClick = { viewModel.speakCurrentStatus() },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberAccentDark),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("SPEAK", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    // 5. Trajectory Layers Filter
                    Text(
                        text = "TRAJECTORY & MAP OVERLAYS",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyanAccent
                    )

                    LayerSwitchRow(
                        label = "Ground Truth (IO-VNBD Benchmark)",
                        color = GroundTruthPath,
                        checked = mapOptions.showGroundTruth,
                        onCheckedChange = { viewModel.updateMapOptions { opt -> opt.copy(showGroundTruth = it) } }
                    )
                    LayerSwitchRow(
                        label = "AI-ML + INS Fusion (ISRO Engine)",
                        color = AmberAccent,
                        checked = mapOptions.showAiFusion,
                        onCheckedChange = { viewModel.updateMapOptions { opt -> opt.copy(showAiFusion = it) } }
                    )
                    LayerSwitchRow(
                        label = "Raw Smartphone IMU Drift (Baseline)",
                        color = PureDrPath,
                        checked = mapOptions.showRawDr,
                        onCheckedChange = { viewModel.updateMapOptions { opt -> opt.copy(showRawDr = it) } }
                    )
                    LayerSwitchRow(
                        label = "Map-Matched Corridor Snapping",
                        color = MapMatchedPath,
                        checked = mapOptions.showMapMatched,
                        onCheckedChange = { viewModel.updateMapOptions { opt -> opt.copy(showMapMatched = it) } }
                    )
                    LayerSwitchRow(
                        label = "Visual-Inertial Odometry (Trajectory D)",
                        color = VisualInsPath,
                        checked = mapOptions.showVisualIns,
                        onCheckedChange = { viewModel.updateMapOptions { opt -> opt.copy(showVisualIns = it) } }
                    )
                    LayerSwitchRow(
                        label = "Road Vector Network & Street Labels",
                        color = RoadVectorColor,
                        checked = mapOptions.showRoadNetwork,
                        onCheckedChange = { viewModel.updateMapOptions { opt -> opt.copy(showRoadNetwork = it) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun LayerSwitchRow(
    label: String,
    color: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
            Text(label, color = TextPrimary, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyanAccent,
                checkedTrackColor = SpaceCardElevated
            )
        )
    }
}
