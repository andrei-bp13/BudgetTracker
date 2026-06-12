package com.budgettracker.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgettracker.app.ui.theme.LocalCurrencySymbol
import com.budgettracker.app.viewmodel.TransactionViewModel

@Composable
fun ReportsScreen(viewModel: TransactionViewModel) {
    val monthly   by viewModel.monthlyTotals.collectAsState()
    val catTotals by viewModel.categoryTotalsThisMonth.collectAsState()
    val currencySymbol = LocalCurrencySymbol.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Reports", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black))

        // ── Expenses bar chart ────────────────────────────────────────────────
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Expenses — Last 6 Months", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                ExpenseBarChart(monthly = monthly, currencySymbol = currencySymbol)
            }
        }

        // ── Donut chart for category breakdown ────────────────────────────────
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Spending by Category — This Month", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                if (catTotals.isEmpty()) {
                    Text("No data this month", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DonutChart(categories = catTotals, viewModel = viewModel, modifier = Modifier.size(140.dp))
                        CategoryLegend(categories = catTotals, viewModel = viewModel, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun ExpenseBarChart(monthly: List<Pair<String, Double>>, currencySymbol: String) {
    val barColor   = MaterialTheme.colorScheme.error
    val gridColor  = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)

    val maxVal = monthly.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
    val animProg by animateFloatAsState(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessVeryLow), label = "bar")

    fun fmtLabel(v: Double): String = when {
        v >= 1_000_000 -> "${"%.1f".format(v / 1_000_000)}M"
        v >= 1_000     -> "${"%.1f".format(v / 1_000)}k"
        else           -> "%.0f".format(v)
    }

    // Measure the widest Y label to compute left margin
    val yLabels = listOf(maxVal, maxVal * 0.66, maxVal * 0.33)
    val yLabelWidth = yLabels.maxOf { textMeasurer.measure(fmtLabel(it), labelStyle).size.width }.toFloat()
    val leftMargin = yLabelWidth + 6.dp.value * 3   // rough dp → px factor applied in Canvas

    Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
        val lm = yLabelWidth + 18f  // left margin in px
        val chartW = size.width - lm
        val chartH = size.height

        // Grid lines + Y labels at 100%, 66%, 33%
        listOf(1.0, 0.66, 0.33).forEach { frac ->
            val y = chartH * (1f - frac.toFloat())
            drawLine(gridColor, Offset(lm, y), Offset(size.width, y), strokeWidth = 0.5.dp.toPx())
            val txt   = fmtLabel(maxVal * frac)
            val meas  = textMeasurer.measure(txt, labelStyle)
            drawText(meas, topLeft = Offset(lm - meas.size.width - 4.dp.toPx(), y - meas.size.height / 2f))
        }
        // Baseline
        drawLine(gridColor, Offset(lm, chartH), Offset(size.width, chartH), strokeWidth = 0.5.dp.toPx())

        // Bars
        val groupW = chartW / monthly.size.coerceAtLeast(1)
        val barW   = groupW * 0.5f

        monthly.forEachIndexed { i, (_, expense) ->
            val cx = lm + groupW * i + groupW / 2f
            val bh = (expense / maxVal * chartH * animProg).toFloat().coerceAtLeast(2f)
            drawRoundRect(
                color  = barColor,
                topLeft = Offset(cx - barW / 2f, chartH - bh),
                size   = Size(barW, bh),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
        }
    }

    // Month labels below chart
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = (yLabelWidth / 3f).dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        monthly.forEach { (label, _) ->
            Text(label.take(3), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DonutChart(categories: List<Pair<String, Double>>, viewModel: TransactionViewModel, modifier: Modifier = Modifier) {
    val total  = categories.sumOf { it.second }.coerceAtLeast(0.01)
    val colors = categories.map { (cat, _) -> viewModel.categoryFor(cat)?.color ?: Color(0xFF9E9E9E) }
    val animProg by animateFloatAsState(1f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessVeryLow), label = "donut")

    Canvas(modifier = modifier) {
        val stroke = 28.dp.toPx()
        val inset  = stroke / 2f
        val rect   = androidx.compose.ui.geometry.Rect(inset, inset, size.width - inset, size.height - inset)

        var startAngle = -90f
        categories.forEachIndexed { i, (_, amount) ->
            val sweep = (amount / total * 360f * animProg).toFloat()
            drawArc(
                color      = colors[i],
                startAngle = startAngle,
                sweepAngle = sweep - 1f,
                useCenter  = false,
                topLeft    = Offset(rect.left, rect.top),
                size       = Size(rect.width, rect.height),
                style      = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun CategoryLegend(categories: List<Pair<String, Double>>, viewModel: TransactionViewModel, modifier: Modifier = Modifier) {
    val total = categories.sumOf { it.second }.coerceAtLeast(0.01)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        categories.take(6).forEach { (cat, amount) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(viewModel.categoryFor(cat)?.color ?: Color(0xFF9E9E9E)))
                Text(cat, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), maxLines = 1)
                Text(
                    "${"%.0f".format(amount / total * 100)}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
