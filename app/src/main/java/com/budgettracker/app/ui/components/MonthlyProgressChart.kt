package com.pardonmyspending.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun MonthlyProgressChart(
    thisMonth: List<Pair<Int, Double>>,
    lastMonth: List<Pair<Int, Double>>,
    modifier: Modifier = Modifier
) {
    val primary   = MaterialTheme.colorScheme.primary
    val lastColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 9.sp, color = labelColor)

    val animProgress by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessVeryLow),
        label         = "chartAnim"
    )

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LegendDot(color = primary, label = "This month")
            LegendDot(color = lastColor, label = "Last month", dashed = true)
        }
        Spacer(Modifier.height(8.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val maxVal = maxOf(
                thisMonth.maxOfOrNull { it.second } ?: 1.0,
                lastMonth.maxOfOrNull { it.second } ?: 1.0
            ).coerceAtLeast(1.0)
            val maxDays = maxOf(thisMonth.size, lastMonth.size).coerceAtLeast(1)

            // Reserve left margin for Y labels
            val yLabelWidth = 36.dp.toPx()
            val chartLeft   = yLabelWidth
            val w = size.width - chartLeft
            val h = size.height

            // Y-axis: 4 evenly spaced grid lines + labels (0, 33%, 66%, 100%)
            val ySteps = 3
            repeat(ySteps + 1) { i ->
                val fraction = i.toFloat() / ySteps
                val y        = h * (1f - fraction)
                val value    = (maxVal * fraction).roundToInt()
                val label    = if (value >= 1000) "${"%.1f".format(value / 1000.0)}k" else "$value"

                drawLine(gridColor, Offset(chartLeft, y), Offset(chartLeft + w, y), strokeWidth = 1.dp.toPx())

                val measured = textMeasurer.measure(label, labelStyle)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        x = chartLeft - measured.size.width - 4.dp.toPx(),
                        y = y - measured.size.height / 2f
                    )
                )
            }

            fun toOffset(day: Int, value: Double): Offset {
                val x = chartLeft + (day - 1).toFloat() / (maxDays - 1).coerceAtLeast(1) * w
                val y = h - (value / maxVal * h * animProgress).toFloat()
                return Offset(x, y)
            }

            // Last month line (dashed, grayed)
            if (lastMonth.size >= 2) {
                for (i in 0 until lastMonth.size - 1) {
                    drawLine(
                        color       = lastColor,
                        start       = toOffset(lastMonth[i].first, lastMonth[i].second),
                        end         = toOffset(lastMonth[i + 1].first, lastMonth[i + 1].second),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect  = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                    )
                }
            }

            // This month — filled gradient + solid line
            if (thisMonth.size >= 2) {
                val fillPath = Path().apply {
                    moveTo(toOffset(thisMonth[0].first, 0.0).x, h)
                    thisMonth.forEach { (day, value) ->
                        lineTo(toOffset(day, value).x, toOffset(day, value).y)
                    }
                    lineTo(toOffset(thisMonth.last().first, 0.0).x, h)
                    close()
                }
                drawPath(
                    fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(primary.copy(alpha = 0.25f), Color.Transparent),
                        startY = 0f, endY = h
                    )
                )

                val linePath = Path().apply {
                    val first = toOffset(thisMonth[0].first, thisMonth[0].second)
                    moveTo(first.x, first.y)
                    thisMonth.drop(1).forEach { (day, value) ->
                        lineTo(toOffset(day, value).x, toOffset(day, value).y)
                    }
                }
                drawPath(linePath, color = primary,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

                val last = toOffset(thisMonth.last().first, thisMonth.last().second)
                drawCircle(primary, radius = 5.dp.toPx(), center = last)
                drawCircle(Color.White, radius = 2.5.dp.toPx(), center = last)
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String, dashed: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(modifier = Modifier.size(24.dp, 2.dp)) {
            if (dashed)
                drawLine(color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2),
                    strokeWidth = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
            else
                drawLine(color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2),
                    strokeWidth = 2.5.dp.toPx())
        }
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
