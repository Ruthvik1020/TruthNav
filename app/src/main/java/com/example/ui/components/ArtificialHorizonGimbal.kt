package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArtificialHorizonGimbal(
    pitchDeg: Float,
    rollDeg: Float,
    headingDeg: Float,
    accelLong: Float = 0f,
    accelLat: Float = 0f,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .size(140.dp)
            .background(SpaceCardElevated, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = size.width / 2f - 4f

            // Outer Compass Ring
            drawCircle(
                color = SpaceBorder,
                radius = radius,
                center = Offset(cx, cy),
                style = Stroke(width = 2f)
            )

            // Artificial Horizon (Pitch and Roll)
            rotate(degrees = -rollDeg, pivot = Offset(cx, cy)) {
                val pitchOffsetPx = (pitchDeg.coerceIn(-45f, 45f) / 45f) * (radius * 0.6f)

                // Sky (Navy Blue) and Ground (Dark Slate)
                val horizonPath = Path().apply {
                    addOval(androidx.compose.ui.geometry.Rect(cx - radius, cy - radius, cx + radius, cy + radius))
                }

                drawContext.canvas.save()
                drawContext.canvas.clipPath(horizonPath)

                // Sky Top
                drawRect(
                    color = Color(0xFF0C4A6E),
                    topLeft = Offset(cx - radius, cy - radius),
                    size = Size(radius * 2, radius + pitchOffsetPx)
                )
                // Ground Bottom
                drawRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(cx - radius, cy + pitchOffsetPx),
                    size = Size(radius * 2, radius * 2)
                )

                // White Horizon Line
                drawLine(
                    color = Color.White,
                    start = Offset(cx - radius * 0.75f, cy + pitchOffsetPx),
                    end = Offset(cx + radius * 0.75f, cy + pitchOffsetPx),
                    strokeWidth = 2.5f
                )

                // Pitch Ladder bars (+10, -10, +20, -20)
                for (p in listOf(-20, -10, 10, 20)) {
                    val yBar = cy + pitchOffsetPx - (p / 45f) * (radius * 0.6f)
                    val barWidth = if (p % 20 == 0) radius * 0.4f else radius * 0.25f
                    drawLine(
                        color = Color.White.copy(alpha = 0.6f),
                        start = Offset(cx - barWidth / 2f, yBar),
                        end = Offset(cx + barWidth / 2f, yBar),
                        strokeWidth = 1.5f
                    )
                }

                drawContext.canvas.restore()
            }

            // Fixed Aircraft/Vehicle Crosshair
            drawLine(
                color = CyanAccent,
                start = Offset(cx - 24f, cy),
                end = Offset(cx - 8f, cy),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = CyanAccent,
                start = Offset(cx + 8f, cy),
                end = Offset(cx + 24f, cy),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
            drawCircle(color = CyanAccent, radius = 3.5f, center = Offset(cx, cy))

            // Heading Arrow & Degree Readout
            rotate(degrees = headingDeg, pivot = Offset(cx, cy)) {
                // North Needle (Red)
                drawLine(
                    color = RedJam,
                    start = Offset(cx, cy - radius + 2f),
                    end = Offset(cx, cy - radius + 12f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }

            // Heading Text
            val hText = "${headingDeg.toInt()}°"
            val textLayout = textMeasurer.measure(
                text = hText,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            )
            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(cx - textLayout.size.width / 2f, cy - radius - 1f)
            )
        }
    }
}
