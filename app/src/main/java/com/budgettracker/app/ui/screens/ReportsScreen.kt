package com.budgettracker.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgettracker.app.ui.theme.LocalCurrencySymbol
import com.budgettracker.app.viewmodel.ChartBar
import com.budgettracker.app.viewmodel.TransactionViewModel
import java.time.LocalDate

private val granularityOptions = listOf(
    Triple("weekly",  "Weekly",  "Last 12 weeks"),
    Triple("monthly", "Monthly", "All months this year"),
    Triple("yearly",  "Yearly",  "All years"),
)

@Composable
fun ReportsScreen(
    viewModel: TransactionViewModel,
    onBarClick: (ChartBar) -> Unit = {}
) {
    val bars         by viewModel.expenseChartBars.collectAsState()
    val granularity  by viewModel.chartGranularity.collectAsState()
    val catTotals    by viewModel.categoryTotalsThisMonth.collectAsState()
    val currencySymbol = LocalCurrencySymbol.current

    var showGranularityMenu by remember { mutableStateOf(false) }

    val chartTitle = when (granularity) {
        "weekly"  -> "Expenses — Last 12 Weeks"
        "yearly"  -> "Expenses — By Year"
        else      -> "Expenses — This Year"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Expenses bar chart ────────────────────────────────────────────────
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(chartTitle, style = MaterialTheme.typography.titleMedium)
                    Box {
                        IconButton(
                            onClick = { showGranularityMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Tune,
                                contentDescription = "Chart settings",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showGranularityMenu,
                            onDismissRequest = { showGranularityMenu = false }
                        ) {
                            granularityOptions.forEach { (key, label, sub) ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(label, style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                sub,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.setChartGranularity(key)
                                        showGranularityMenu = false
                                    },
                                    trailingIcon = {
                                        if (granularity == key) {
                                            Icon(
                                                Icons.Outlined.Check, null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                ExpenseBarChart(
                    bars           = bars,
                    currencySymbol = currencySymbol,
                    onBarClick     = onBarClick
                )
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
                    Text(
                        "No data this month",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DonutChart(
                            categories = catTotals,
                            viewModel  = viewModel,
                            modifier   = Modifier.size(140.dp)
                        )
                        CategoryLegend(
                            categories = catTotals,
                            viewModel  = viewModel,
                            modifier   = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun ExpenseBarChart(
    bars: List<ChartBar>,
    currencySymbol: String,
    onBarClick: (ChartBar) -> Unit
) {
    val barColor     = MaterialTheme.colorScheme.primary
    val gridColor    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val labelColor   = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val density      = LocalDensity.current
    val labelStyle   = TextStyle(fontSize = 10.sp, color = labelColor)

    val maxVal = bars.maxOfOrNull { it.value }?.coerceAtLeast(1.0) ?: 1.0
    val animProg by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessVeryLow),
        label = "bar"
    )

    fun fmtLabel(v: Double): String = when {
        v >= 1_000_000 -> "${"%.1f".format(v / 1_000_000)}M"
        v >= 1_000     -> "${"%.1f".format(v / 1_000)}k"
        else           -> "%.0f".format(v)
    }

    // Measure y-label width and x-label height outside Canvas so we can use
    // the same values for the transparent tap overlay Row.
    val yLabelWidthPx = remember(maxVal, textMeasurer) {
        listOf(maxVal, maxVal * 0.66, maxVal * 0.33)
            .maxOf { textMeasurer.measure(fmtLabel(it), labelStyle).size.width }
            .toFloat()
    }
    val xLabelHeightPx = remember(textMeasurer) {
        textMeasurer.measure("Jan", labelStyle).size.height.toFloat()
    }
    val leftMarginDp   = with(density) { (yLabelWidthPx + 18f).toDp() }
    val bottomPadDp    = with(density) { (xLabelHeightPx + 6.dp.toPx()).toDp() }

    Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            val lm      = yLabelWidthPx + 18f
            val chartW  = size.width - lm
            val chartH  = size.height - xLabelHeightPx - 6.dp.toPx()

            // Grid + y-axis labels
            listOf(1.0, 0.66, 0.33).forEach { frac ->
                val y    = chartH * (1f - frac.toFloat())
                val meas = textMeasurer.measure(fmtLabel(maxVal * frac), labelStyle)
                drawLine(gridColor, Offset(lm, y), Offset(size.width, y), strokeWidth = 0.5.dp.toPx())
                drawText(meas, topLeft = Offset(lm - meas.size.width - 4.dp.toPx(), y - meas.size.height / 2f))
            }
            drawLine(gridColor, Offset(lm, chartH), Offset(size.width, chartH), strokeWidth = 0.5.dp.toPx())

            // Bars + x-axis labels (full label, no truncation)
            val groupW = chartW / bars.size.coerceAtLeast(1)
            val barW   = groupW * 0.5f
            bars.forEachIndexed { i, bar ->
                val cx = lm + groupW * i + groupW / 2f
                val bh = (bar.value / maxVal * chartH * animProg).toFloat().coerceAtLeast(2f)
                drawRoundRect(
                    color        = barColor,
                    topLeft      = Offset(cx - barW / 2f, chartH - bh),
                    size         = Size(barW, bh),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
                val meas = textMeasurer.measure(bar.label, labelStyle)
                drawText(meas, topLeft = Offset(cx - meas.size.width / 2f, chartH + 6.dp.toPx()))
            }
        }

        // Transparent tap targets aligned to each bar column
        if (bars.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = leftMarginDp, bottom = bottomPadDp)
            ) {
                bars.forEach { bar ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onBarClick(bar) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DonutChart(
    categories: List<Pair<String, Double>>,
    viewModel: TransactionViewModel,
    modifier: Modifier = Modifier
) {
    val total    = categories.sumOf { it.second }.coerceAtLeast(0.01)
    val colors   = categories.map { (cat, _) -> viewModel.categoryFor(cat)?.color ?: Color(0xFF9E9E9E) }
    val animProg by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessVeryLow),
        label = "donut"
    )

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
private fun CategoryLegend(
    categories: List<Pair<String, Double>>,
    viewModel: TransactionViewModel,
    modifier: Modifier = Modifier
) {
    val total = categories.sumOf { it.second }.coerceAtLeast(0.01)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        categories.take(6).forEach { (cat, amount) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(viewModel.categoryFor(cat)?.color ?: Color(0xFF9E9E9E))
                )
                Text(
                    cat,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Text(
                    "${"%.0f".format(amount / total * 100)}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
