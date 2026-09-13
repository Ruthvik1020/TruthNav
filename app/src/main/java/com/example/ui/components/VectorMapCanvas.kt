package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.map.MapLayerType
import com.example.map.TerrainTileEngine
import com.example.model.*
import com.example.ui.MapDisplayOptions
import com.example.ui.theme.*
import kotlin.math.*

@Composable
fun VectorMapCanvas(
    vehicleState: VehicleState,
    groundTruthTrail: List<TrajectoryPoint>,
    rawDrTrail: List<TrajectoryPoint>,
    aiInsTrail: List<TrajectoryPoint>,
    mapMatchedTrail: List<TrajectoryPoint>,
    roadSegments: List<RoadSegment>,
    destination: TargetDestination? = null,
    outageZoneStartSec: Int? = null,
    outageZoneEndSec: Int? = null,
    mapOptions: MapDisplayOptions,
    terrainTileEngine: TerrainTileEngine? = null,
    tileRepaintTrigger: Long = 0L,
    isVoiceSpeaking: Boolean = false,
    isVoiceMuted: Boolean = false,
    modifier: Modifier = Modifier,
    onToggleFollow: () -> Unit = {},
    onChangeMapLayer: (MapLayerType) -> Unit = {},
    onSpeakStatus: () -> Unit = {},
    onToggleVoiceMute: () -> Unit = {},
    onRelocateVehicle: (Double, Double) -> Unit = { _, _ -> },
    onSetDestination: (Double, Double) -> Unit = { _, _ -> },
    onClearDestination: () -> Unit = {},
    onCurrentLocationClicked: () -> Unit = {}
) {
    var zoomScale by remember { mutableFloatStateOf(1.8f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }
    var showLayerSelector by remember { mutableStateOf(false) }

    // Pulsing radar ring animation
    val infiniteTransition = rememberInfiniteTransition(label = "radarPulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 38f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    val destPulseRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 44f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "destPulse"
    )
    val destPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "destAlpha"
    )

    val textMeasurer = rememberTextMeasurer()
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    // Geodetic reference
    val refLat = if (mapOptions.followVehicle) vehicleState.lat else (groundTruthTrail.firstOrNull()?.lat ?: vehicleState.lat)
    val refLng = if (mapOptions.followVehicle) vehicleState.lng else (groundTruthTrail.firstOrNull()?.lng ?: vehicleState.lng)

    val continuousZoom = (15.0 + ln(zoomScale.toDouble()) / ln(2.0)).coerceIn(10.0, 19.0)
    val discreteZoom = continuousZoom.toInt()
    val subScale = 2.0.pow(continuousZoom - discreteZoom).toFloat()

    val (cwx, cwy) = if (terrainTileEngine != null) {
        terrainTileEngine.latLngToWorldPixel(refLat, refLng, discreteZoom.toDouble())
    } else {
        Pair(0.0, 0.0)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    zoomScale = (zoomScale * zoom).coerceIn(0.4f, 8.0f)
                    panOffsetX += pan.x
                    panOffsetY += pan.y
                }
            }
            .pointerInput(vehicleState.lat, vehicleState.lng, destination?.lat, destination?.lng, zoomScale, panOffsetX, panOffsetY, canvasSize) {
                detectTapGestures(
                    onLongPress = { longPressOffset ->
                        val centerCanvasX = canvasSize.width / 2f + panOffsetX
                        val centerCanvasY = canvasSize.height / 2f + panOffsetY

                        // Long-press: Set Target Destination
                        val (targetLat, targetLng) = if (terrainTileEngine != null) {
                            val dx = (longPressOffset.x - centerCanvasX) / subScale
                            val dy = (longPressOffset.y - centerCanvasY) / subScale
                            val wx = cwx + dx
                            val wy = cwy + dy
                            terrainTileEngine.worldPixelToLatLng(wx, wy, discreteZoom.toDouble())
                        } else {
                            val metersPerPixel = 1.0f / (zoomScale * 0.4f)
                            val metersPerDegLat = 111132.954
                            val metersPerDegLng = 111132.954 * cos(Math.toRadians(refLat))
                            val dxPixels = longPressOffset.x - centerCanvasX
                            val dyPixels = -(longPressOffset.y - centerCanvasY)
                            val dEastMeters = dxPixels * metersPerPixel
                            val dNorthMeters = dyPixels * metersPerPixel
                            val lat = refLat + (dNorthMeters / metersPerDegLat)
                            val lng = refLng + (dEastMeters / metersPerDegLng)
                            Pair(lat, lng)
                        }

                        onSetDestination(targetLat, targetLng)
                    },
                    onTap = { tapOffset ->
                        val centerCanvasX = canvasSize.width / 2f + panOffsetX
                        val centerCanvasY = canvasSize.height / 2f + panOffsetY

                        // Calculate vehicle canvas position
                        val (vx, vy) = if (terrainTileEngine != null) {
                            val (wx, wy) = terrainTileEngine.latLngToWorldPixel(vehicleState.lat, vehicleState.lng, discreteZoom.toDouble())
                            val px = centerCanvasX + ((wx - cwx) * subScale).toFloat()
                            val py = centerCanvasY + ((wy - cwy) * subScale).toFloat()
                            Pair(px, py)
                        } else {
                            val metersPerPixel = 1.0f / (zoomScale * 0.4f)
                            val metersPerDegLat = 111132.954
                            val metersPerDegLng = 111132.954 * cos(Math.toRadians(refLat))
                            val dNorthMeters = (vehicleState.lat - refLat) * metersPerDegLat
                            val dEastMeters = (vehicleState.lng - refLng) * metersPerDegLng
                            val px = centerCanvasX + (dEastMeters / metersPerPixel).toFloat()
                            val py = centerCanvasY - (dNorthMeters / metersPerPixel).toFloat()
                            Pair(px, py)
                        }

                        val distToVehicle = hypot(tapOffset.x - vx, tapOffset.y - vy)

                        // If tapped directly on or near the vehicle cursor (< 45px) -> Recenter camera on it
                        if (distToVehicle < 45f) {
                            panOffsetX = 0f
                            panOffsetY = 0f
                            onToggleFollow()
                        }
                    }
                )
            }
    ) {
        // Redraw triggered when new tiles are downloaded
        val currentRepaint = tileRepaintTrigger

        Canvas(modifier = Modifier.fillMaxSize().testTag("map_canvas_viewport")) {
            canvasSize = size
            val centerCanvasX = size.width / 2f + panOffsetX
            val centerCanvasY = size.height / 2f + panOffsetY

            // 1. Draw Real Terrain Map Tiles from Online / Cached Terrain Provider
            if (terrainTileEngine != null) {
                terrainTileEngine.drawTiles(
                    drawScope = this,
                    centerLat = refLat,
                    centerLng = refLng,
                    zoomScale = zoomScale,
                    centerCanvasX = centerCanvasX,
                    centerCanvasY = centerCanvasY,
                    layerType = mapOptions.mapLayerType
                )
            } else {
                // Procedural Terrain Grid Fallback
                drawTacticalGrid(size, centerCanvasX, centerCanvasY, zoomScale)
            }

            val metersPerPixel = 1.0f / (zoomScale * 0.4f)
            val metersPerDegLat = 111132.954
            val metersPerDegLng = 111132.954 * cos(Math.toRadians(refLat))

            fun geoToCanvas(lat: Double, lng: Double): Offset {
                return if (terrainTileEngine != null) {
                    val (wx, wy) = terrainTileEngine.latLngToWorldPixel(lat, lng, discreteZoom.toDouble())
                    val px = centerCanvasX + ((wx - cwx) * subScale).toFloat()
                    val py = centerCanvasY + ((wy - cwy) * subScale).toFloat()
                    Offset(px, py)
                } else {
                    val dNorthMeters = (lat - refLat) * metersPerDegLat
                    val dEastMeters = (lng - refLng) * metersPerDegLng
                    val px = centerCanvasX + (dEastMeters / metersPerPixel).toFloat()
                    val py = centerCanvasY - (dNorthMeters / metersPerPixel).toFloat()
                    Offset(px, py)
                }
            }

            // Subtle dark overlay to keep HUD and trajectories crisp over high-contrast terrain
            if (mapOptions.mapLayerType == MapLayerType.SATELLITE || mapOptions.mapLayerType == MapLayerType.TERRAIN_TOPO) {
                drawRect(
                    color = Color.Black.copy(alpha = 0.15f),
                    size = size
                )
            }

            // 0. Road Vector Network & Street Labels
            if (mapOptions.showRoadNetwork && roadSegments.isNotEmpty()) {
                val roadWidth = (5.5f * zoomScale.coerceIn(0.8f, 2.5f)).coerceIn(4f, 12f)
                val roadBorderWidth = roadWidth + 3.0f

                // First pass: Road outlines/casing
                for (seg in roadSegments) {
                    val p1 = geoToCanvas(seg.startNode.lat, seg.startNode.lng)
                    val p2 = geoToCanvas(seg.endNode.lat, seg.endNode.lng)
                    drawLine(
                        color = Color.Black.copy(alpha = 0.6f),
                        start = p1,
                        end = p2,
                        strokeWidth = roadBorderWidth,
                        cap = StrokeCap.Round
                    )
                }

                // Second pass: Road surface
                for (seg in roadSegments) {
                    val p1 = geoToCanvas(seg.startNode.lat, seg.startNode.lng)
                    val p2 = geoToCanvas(seg.endNode.lat, seg.endNode.lng)
                    drawLine(
                        color = RoadVectorColor.copy(alpha = 0.85f),
                        start = p1,
                        end = p2,
                        strokeWidth = roadWidth,
                        cap = StrokeCap.Round
                    )
                    // Road center dashed marking
                    drawLine(
                        color = Color.White.copy(alpha = 0.7f),
                        start = p1,
                        end = p2,
                        strokeWidth = (roadWidth * 0.2f).coerceAtLeast(1.2f),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
                        cap = StrokeCap.Round
                    )
                }

                // Third pass: Road Name Badges & Landmark POIs
                for (seg in roadSegments) {
                    val p1 = geoToCanvas(seg.startNode.lat, seg.startNode.lng)
                    val p2 = geoToCanvas(seg.endNode.lat, seg.endNode.lng)
                    val mid = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)

                    // Draw Road Name Label
                    val roadLabel = seg.name
                    val roadTextLayout = textMeasurer.measure(
                        text = roadLabel,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = (8.5f * zoomScale.coerceIn(0.85f, 1.3f)).sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    val padX = 6f
                    val padY = 3f
                    val badgeW = roadTextLayout.size.width + padX * 2
                    val badgeH = roadTextLayout.size.height + padY * 2
                    val badgeTopLeft = Offset(mid.x - badgeW / 2f, mid.y - badgeH / 2f - 14f)

                    drawRoundRect(
                        color = SpaceDark.copy(alpha = 0.88f),
                        topLeft = badgeTopLeft,
                        size = Size(badgeW, badgeH),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = AmberAccent.copy(alpha = 0.65f),
                        topLeft = badgeTopLeft,
                        size = Size(badgeW, badgeH),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(width = 1.0f)
                    )
                    drawText(
                        textLayoutResult = roadTextLayout,
                        topLeft = Offset(badgeTopLeft.x + padX, badgeTopLeft.y + padY)
                    )
                }
            }

            // 0.5 Navigation Landmark POI Labels
            if (roadSegments.isNotEmpty()) {
                val startSeg = roadSegments.first()
                val endSeg = roadSegments.last()
                val landmarks = listOf(
                    Triple(startSeg.startNode.lat, startSeg.startNode.lng, "🛰️ ISRO NavIC Station"),
                    Triple(endSeg.endNode.lat, endSeg.endNode.lng, "🏁 Navigation Waypoint End")
                )

                for ((lat, lng, poiLabel) in landmarks) {
                    val pos = geoToCanvas(lat, lng)
                    drawCircle(
                        color = AmberAccent,
                        radius = 4.5f,
                        center = pos
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.0f,
                        center = pos
                    )

                    val poiTextLayout = textMeasurer.measure(
                        text = poiLabel,
                        style = TextStyle(
                            color = AmberAccent,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    val pw = poiTextLayout.size.width + 10f
                    val ph = poiTextLayout.size.height + 4f
                    val ptl = Offset(pos.x - pw / 2f, pos.y + 8f)

                    drawRoundRect(
                        color = SpaceDark.copy(alpha = 0.90f),
                        topLeft = ptl,
                        size = Size(pw, ph),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = AmberAccent.copy(alpha = 0.7f),
                        topLeft = ptl,
                        size = Size(pw, ph),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(width = 1f)
                    )
                    drawText(
                        textLayoutResult = poiTextLayout,
                        topLeft = Offset(ptl.x + 5f, ptl.y + 2f)
                    )
                }
            }

            // 0.8 Geodetic Coordinate Grid Lines & Axis Labels
            val latStep = 0.002
            val lngStep = 0.002
            val minGridLat = refLat - 0.008
            val maxGridLat = refLat + 0.008
            val minGridLng = refLng - 0.008
            val maxGridLng = refLng + 0.008

            var curLat = minGridLat
            while (curLat <= maxGridLat) {
                val p1 = geoToCanvas(curLat, minGridLng)
                val p2 = geoToCanvas(curLat, maxGridLng)
                drawLine(
                    color = Color.White.copy(alpha = 0.07f),
                    start = p1,
                    end = p2,
                    strokeWidth = 1f
                )
                // Grid Label
                if (p1.y in 20f..(size.height - 20f)) {
                    val latLabel = "${"%.4f".format(curLat)}°N"
                    val t = textMeasurer.measure(
                        latLabel,
                        style = TextStyle(color = Color.White.copy(alpha = 0.35f), fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                    )
                    drawText(t, topLeft = Offset(6f, p1.y - 10f))
                }
                curLat += latStep
            }

            var curLng = minGridLng
            while (curLng <= maxGridLng) {
                val p1 = geoToCanvas(minGridLat, curLng)
                val p2 = geoToCanvas(maxGridLat, curLng)
                drawLine(
                    color = Color.White.copy(alpha = 0.07f),
                    start = p1,
                    end = p2,
                    strokeWidth = 1f
                )
                if (p1.x in 20f..(size.width - 20f)) {
                    val lngLabel = "${"%.4f".format(curLng)}°E"
                    val t = textMeasurer.measure(
                        lngLabel,
                        style = TextStyle(color = Color.White.copy(alpha = 0.35f), fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                    )
                    drawText(t, topLeft = Offset(p1.x - 15f, size.height - 18f))
                }
                curLng += lngStep
            }

            // 1. Ground Truth Trail (Green) - only when enabled
            if (mapOptions.showGroundTruth && groundTruthTrail.size > 1) {
                val gtPath = Path()
                val start = geoToCanvas(groundTruthTrail.first().lat, groundTruthTrail.first().lng)
                gtPath.moveTo(start.x, start.y)
                for (i in 1 until groundTruthTrail.size) {
                    val pt = geoToCanvas(groundTruthTrail[i].lat, groundTruthTrail[i].lng)
                    gtPath.lineTo(pt.x, pt.y)
                }
                drawPath(
                    path = gtPath,
                    color = GroundTruthPath,
                    style = Stroke(width = 2.5f * zoomScale.coerceIn(0.8f, 2.5f), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // 2. Raw Unconstrained DR Trail (Red dashed) - only when enabled
            if (mapOptions.showRawDr && rawDrTrail.size > 1) {
                val rawPath = Path()
                val start = geoToCanvas(rawDrTrail.first().lat, rawDrTrail.first().lng)
                rawPath.moveTo(start.x, start.y)
                for (i in 1 until rawDrTrail.size) {
                    val pt = geoToCanvas(rawDrTrail[i].lat, rawDrTrail[i].lng)
                    rawPath.lineTo(pt.x, pt.y)
                }
                drawPath(
                    path = rawPath,
                    color = PureDrPath.copy(alpha = 0.85f),
                    style = Stroke(
                        width = 2.5f * zoomScale.coerceIn(0.8f, 2.5f),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
                        cap = StrokeCap.Round
                    )
                )
            }

            // 3. AI-ML INS Fusion Trail (Cyan / Amber)
            if (mapOptions.showAiFusion && aiInsTrail.size > 1) {
                val aiPath = Path()
                val start = geoToCanvas(aiInsTrail.first().lat, aiInsTrail.first().lng)
                aiPath.moveTo(start.x, start.y)
                for (i in 1 until aiInsTrail.size) {
                    val pt = geoToCanvas(aiInsTrail[i].lat, aiInsTrail[i].lng)
                    aiPath.lineTo(pt.x, pt.y)
                }
                // Sleek path with subtle shadow
                val trailWidth = (3.2f * zoomScale.pow(0.35f)).coerceIn(2.5f, 5.0f)
                drawPath(
                    path = aiPath,
                    color = Color.Black.copy(alpha = 0.45f),
                    style = Stroke(width = trailWidth + 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = aiPath,
                    color = AmberAccent,
                    style = Stroke(width = trailWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // 4. Map-Matched Corridor (Cyan dots)
            if (mapOptions.showMapMatched && mapMatchedTrail.isNotEmpty()) {
                for (pt in mapMatchedTrail) {
                    val c = geoToCanvas(pt.lat, pt.lng)
                    drawCircle(
                        color = MapMatchedPath,
                        radius = 2.5f * zoomScale.coerceIn(0.8f, 2.5f),
                        center = c
                    )
                }
            }

            // ============================================================
            // 5. RESPONSIVE VEHICLE CURSOR (ADAPTIVE SIZE ON ZOOM IN / OUT)
            // ============================================================
            val vehicleCanvasPos = geoToCanvas(vehicleState.lat, vehicleState.lng)

            // Dynamic Adaptive Scale: Damped curve so cursor remains comfortable, crisp, and tactile across all zooms
            val adaptiveCursorScale = (zoomScale.pow(0.30f)).coerceIn(0.85f, 1.40f)
            val markerColor = when (vehicleState.fusionMode) {
                FusionMode.GNSS_AIDED -> EmeraldGps
                FusionMode.DEAD_RECKONING_AI -> AmberDr
                FusionMode.GNSS_OUTAGE_JAMMED -> RedJam
                FusionMode.CALIBRATING -> AmberAccent
            }

            // 5.1 Outer Accuracy Radar Pulse Ring
            drawCircle(
                color = markerColor.copy(alpha = pulseAlpha),
                radius = pulseRadius * adaptiveCursorScale,
                center = vehicleCanvasPos,
                style = Stroke(width = 2f * adaptiveCursorScale)
            )

            // 5.2 Soft Accuracy Halo
            drawCircle(
                color = markerColor.copy(alpha = 0.12f),
                radius = 26f * adaptiveCursorScale,
                center = vehicleCanvasPos
            )

            // 5.3 Heading Navigation Arrow & Directional Chevron
            rotate(degrees = vehicleState.headingDeg, pivot = vehicleCanvasPos) {
                // Forward Heading Light Beam / Cone
                val beamPath = Path().apply {
                    moveTo(vehicleCanvasPos.x, vehicleCanvasPos.y - 12f * adaptiveCursorScale)
                    lineTo(vehicleCanvasPos.x + 18f * adaptiveCursorScale, vehicleCanvasPos.y - 54f * adaptiveCursorScale)
                    lineTo(vehicleCanvasPos.x - 18f * adaptiveCursorScale, vehicleCanvasPos.y - 54f * adaptiveCursorScale)
                    close()
                }
                drawPath(
                    path = beamPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(markerColor.copy(alpha = 0.0f), markerColor.copy(alpha = 0.35f)),
                        startY = vehicleCanvasPos.y - 54f * adaptiveCursorScale,
                        endY = vehicleCanvasPos.y - 12f * adaptiveCursorScale
                    )
                )

                // Outer Arrow Geometry
                val arrowTipY = vehicleCanvasPos.y - 20f * adaptiveCursorScale
                val arrowLeftX = vehicleCanvasPos.x - 12f * adaptiveCursorScale
                val arrowRightX = vehicleCanvasPos.x + 12f * adaptiveCursorScale
                val arrowBaseY = vehicleCanvasPos.y + 14f * adaptiveCursorScale
                val arrowInnerY = vehicleCanvasPos.y + 7f * adaptiveCursorScale

                val arrowPath = Path().apply {
                    moveTo(vehicleCanvasPos.x, arrowTipY)
                    lineTo(arrowRightX, arrowBaseY)
                    lineTo(vehicleCanvasPos.x, arrowInnerY)
                    lineTo(arrowLeftX, arrowBaseY)
                    close()
                }

                // Left Half for 3D Lighting Effect
                val leftHalfPath = Path().apply {
                    moveTo(vehicleCanvasPos.x, arrowTipY)
                    lineTo(vehicleCanvasPos.x, arrowInnerY)
                    lineTo(arrowLeftX, arrowBaseY)
                    close()
                }

                // Right Half for 3D Lighting Effect
                val rightHalfPath = Path().apply {
                    moveTo(vehicleCanvasPos.x, arrowTipY)
                    lineTo(arrowRightX, arrowBaseY)
                    lineTo(vehicleCanvasPos.x, arrowInnerY)
                    close()
                }

                // Drop Shadow
                drawPath(path = arrowPath, color = Color.Black.copy(alpha = 0.60f))

                // Split 3D Fill
                drawPath(path = leftHalfPath, color = markerColor)
                drawPath(path = rightHalfPath, color = markerColor.copy(red = (markerColor.red * 0.85f), green = (markerColor.green * 0.85f), blue = (markerColor.blue * 0.85f)))

                // Crisp White Outer Boundary Stroke
                drawPath(
                    path = arrowPath,
                    color = Color.White,
                    style = Stroke(width = 2.0f * adaptiveCursorScale, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // 5.4 Center Anchor Core Dot
            drawCircle(
                color = Color.White,
                radius = 3.5f * adaptiveCursorScale,
                center = vehicleCanvasPos
            )
            drawCircle(
                color = markerColor,
                radius = 2.2f * adaptiveCursorScale,
                center = vehicleCanvasPos
            )

            // 5.5 Speed Capsule Badge (Current Location Cursor)
            val speedLabel = "${vehicleState.speedKmph.toInt()} km/h · LIVE GPS"
            val textLayout = textMeasurer.measure(
                text = speedLabel,
                style = TextStyle(
                    color = Color.White,
                    fontSize = (9.0f * adaptiveCursorScale).sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            )

            val badgeWidth = textLayout.size.width + 12f * adaptiveCursorScale
            val badgeHeight = textLayout.size.height + 6f * adaptiveCursorScale
            val badgeTopLeft = Offset(
                vehicleCanvasPos.x - badgeWidth / 2f,
                vehicleCanvasPos.y + 18f * adaptiveCursorScale
            )

            // Badge Background
            drawRoundRect(
                color = SpaceDark.copy(alpha = 0.88f),
                topLeft = badgeTopLeft,
                size = Size(badgeWidth, badgeHeight),
                cornerRadius = CornerRadius(10f, 10f)
            )
            drawRoundRect(
                color = markerColor.copy(alpha = 0.7f),
                topLeft = badgeTopLeft,
                size = Size(badgeWidth, badgeHeight),
                cornerRadius = CornerRadius(10f, 10f),
                style = Stroke(width = 1.2f)
            )

            // Badge Text
            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(
                    badgeTopLeft.x + 6f * adaptiveCursorScale,
                    badgeTopLeft.y + 3f * adaptiveCursorScale
                )
            )

            // ============================================================
            // 6. CURSOR 2: SELECTED DESTINATION TARGET CURSOR
            // ============================================================
            if (destination != null) {
                val destCanvasPos = geoToCanvas(destination.lat, destination.lng)

                // 6.1 Tactical Navigation Route Guidance Dashed Line
                drawLine(
                    color = Color(0xFFFF334B).copy(alpha = 0.75f),
                    start = vehicleCanvasPos,
                    end = destCanvasPos,
                    strokeWidth = 2.2f * adaptiveCursorScale,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )

                // 6.2 Pulsing Destination Radar Ring
                drawCircle(
                    color = Color(0xFFFF334B).copy(alpha = destPulseAlpha),
                    radius = destPulseRadius * adaptiveCursorScale,
                    center = destCanvasPos,
                    style = Stroke(width = 1.8f)
                )
                drawCircle(
                    color = Color(0xFFFF334B).copy(alpha = 0.20f),
                    radius = 16f * adaptiveCursorScale,
                    center = destCanvasPos
                )

                // 6.3 Crosshair Ticks
                val tickLen = 14f * adaptiveCursorScale
                val tickGap = 8f * adaptiveCursorScale
                drawLine(
                    color = Color(0xFFFF334B),
                    start = Offset(destCanvasPos.x - tickLen, destCanvasPos.y),
                    end = Offset(destCanvasPos.x - tickGap, destCanvasPos.y),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = Color(0xFFFF334B),
                    start = Offset(destCanvasPos.x + tickGap, destCanvasPos.y),
                    end = Offset(destCanvasPos.x + tickLen, destCanvasPos.y),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = Color(0xFFFF334B),
                    start = Offset(destCanvasPos.x, destCanvasPos.y - tickLen),
                    end = Offset(destCanvasPos.x, destCanvasPos.y - tickGap),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = Color(0xFFFF334B),
                    start = Offset(destCanvasPos.x, destCanvasPos.y + tickGap),
                    end = Offset(destCanvasPos.x, destCanvasPos.y + tickLen),
                    strokeWidth = 1.5f
                )

                // 6.4 Anchor & Destination Beacon Pin
                drawCircle(
                    color = Color.White,
                    radius = 3.5f * adaptiveCursorScale,
                    center = destCanvasPos
                )
                drawCircle(
                    color = Color(0xFFFF334B),
                    radius = 2.0f * adaptiveCursorScale,
                    center = destCanvasPos
                )

                // Beacon Pole
                val pinHeadCenter = Offset(destCanvasPos.x, destCanvasPos.y - 22f * adaptiveCursorScale)
                drawLine(
                    color = Color.White,
                    start = destCanvasPos,
                    end = pinHeadCenter,
                    strokeWidth = 2.0f * adaptiveCursorScale
                )

                // Beacon Head
                drawCircle(
                    color = Color(0xFFFF334B),
                    radius = 7.5f * adaptiveCursorScale,
                    center = pinHeadCenter
                )
                drawCircle(
                    color = Color.White,
                    radius = 7.5f * adaptiveCursorScale,
                    center = pinHeadCenter,
                    style = Stroke(width = 1.5f)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.8f * adaptiveCursorScale,
                    center = pinHeadCenter
                )

                // 6.5 Destination Badge Callout
                val dLat = Math.toRadians(destination.lat - vehicleState.lat)
                val dLng = Math.toRadians(destination.lng - vehicleState.lng)
                val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(vehicleState.lat)) * cos(Math.toRadians(destination.lat)) * sin(dLng / 2) * sin(dLng / 2)
                val c = 2 * atan2(sqrt(a), sqrt(1 - a))
                val distMeters = 6371000.0 * c
                val distStr = if (distMeters >= 1000) "${"%.2f".format(distMeters / 1000.0)} km" else "${distMeters.toInt()} m"
                val destLabel = "TARGET · $distStr"

                val destTextLayout = textMeasurer.measure(
                    text = destLabel,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = (9.0f * adaptiveCursorScale).sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                )

                val destBadgeWidth = destTextLayout.size.width + 12f * adaptiveCursorScale
                val destBadgeHeight = destTextLayout.size.height + 6f * adaptiveCursorScale
                val destBadgeTopLeft = Offset(
                    destCanvasPos.x - destBadgeWidth / 2f,
                    destCanvasPos.y - 42f * adaptiveCursorScale
                )

                drawRoundRect(
                    color = SpaceDark.copy(alpha = 0.90f),
                    topLeft = destBadgeTopLeft,
                    size = Size(destBadgeWidth, destBadgeHeight),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                drawRoundRect(
                    color = Color(0xFFFF334B).copy(alpha = 0.85f),
                    topLeft = destBadgeTopLeft,
                    size = Size(destBadgeWidth, destBadgeHeight),
                    cornerRadius = CornerRadius(10f, 10f),
                    style = Stroke(width = 1.2f)
                )

                drawText(
                    textLayoutResult = destTextLayout,
                    topLeft = Offset(
                        destBadgeTopLeft.x + 6f * adaptiveCursorScale,
                        destBadgeTopLeft.y + 3f * adaptiveCursorScale
                    )
                )
            }
        }

        // ============================================================
        // 1. FLOATING ACTIVE DESTINATION BANNER (IF SET)
        // ============================================================
        if (destination != null) {
            val dLat = Math.toRadians(destination.lat - vehicleState.lat)
            val dLng = Math.toRadians(destination.lng - vehicleState.lng)
            val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(vehicleState.lat)) * cos(Math.toRadians(destination.lat)) * sin(dLng / 2) * sin(dLng / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            val distMeters = 6371000.0 * c
            val distStr = if (distMeters >= 1000) "${"%.2f".format(distMeters / 1000.0)} km" else "${distMeters.toInt()} m"

            val y = sin(dLng) * cos(Math.toRadians(destination.lat))
            val x = cos(Math.toRadians(vehicleState.lat)) * sin(Math.toRadians(destination.lat)) -
                    sin(Math.toRadians(vehicleState.lat)) * cos(Math.toRadians(destination.lat)) * cos(dLng)
            val bearingDeg = ((Math.toDegrees(atan2(y, x)) + 360.0) % 360.0).toInt()

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SpaceDarkSurface.copy(alpha = 0.94f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF334B)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 62.dp)
            ) {
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = Color(0xFFFF334B),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "DEST: $distStr · $bearingDeg°",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Surface(
                        onClick = { onClearDestination() },
                        shape = CircleShape,
                        color = Color(0xFFFF334B).copy(alpha = 0.25f),
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Destination",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // ============================================================
        // 2. MID-RIGHT FLOATING MAP CONTROLS (RECENTER, ZOOM IN, ZOOM OUT)
        // ============================================================
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SpaceDarkSurface.copy(alpha = 0.92f),
            border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBorder),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(3.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Current Location & Recenter Camera on Vehicle
                IconButton(
                    onClick = {
                        panOffsetX = 0f
                        panOffsetY = 0f
                        onCurrentLocationClicked()
                        onToggleFollow()
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("map_recenter_button")
                ) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = "Read Current Location & Recenter",
                        tint = if (mapOptions.followVehicle) AmberAccent else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                HorizontalDivider(
                    color = SpaceBorder.copy(alpha = 0.6f),
                    modifier = Modifier.width(20.dp)
                )

                // Zoom In
                IconButton(
                    onClick = { zoomScale = (zoomScale * 1.35f).coerceAtMost(8.0f) },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("map_zoom_in_button")
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                HorizontalDivider(
                    color = SpaceBorder.copy(alpha = 0.6f),
                    modifier = Modifier.width(20.dp)
                )

                // Zoom Out
                IconButton(
                    onClick = { zoomScale = (zoomScale / 1.35f).coerceAtLeast(0.4f) },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("map_zoom_out_button")
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawTacticalGrid(size: androidx.compose.ui.geometry.Size, cx: Float, cy: Float, zoom: Float) {
    val step = 60f * zoom
    var x = cx % step
    while (x < size.width) {
        drawLine(
            color = SpaceBorder.copy(alpha = 0.20f),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )
        x += step
    }

    var y = cy % step
    while (y < size.height) {
        drawLine(
            color = SpaceBorder.copy(alpha = 0.20f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
        y += step
    }

    for (r in listOf(140f * zoom, 280f * zoom, 440f * zoom)) {
        drawCircle(
            color = SpaceBorder.copy(alpha = 0.15f),
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f))
        )
    }
}
