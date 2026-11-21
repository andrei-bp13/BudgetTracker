package com.pardonmyspending.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider as GlanceColorProvider
import com.pardonmyspending.app.R
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

// ── M3 Expressive colour palette ──────────────────────────────────────────

private class WidgetColors(
    val bg:                  GlanceColorProvider,
    val onBg:                GlanceColorProvider,
    val subtext:             GlanceColorProvider,
    val primary:             GlanceColorProvider,
    val onPrimary:           GlanceColorProvider,
    val primaryContainer:    GlanceColorProvider,
    val onPrimaryContainer:  GlanceColorProvider,
    val green:               GlanceColorProvider,
    val greenContainer:      GlanceColorProvider,
    val onGreenContainer:    GlanceColorProvider,
    val error:               GlanceColorProvider,
    val errorContainer:      GlanceColorProvider,
    val onErrorContainer:    GlanceColorProvider,
    val track:               GlanceColorProvider,
)

// ColorProvider(day, night) must be called inside composable scope
@Composable
private fun widgetColors() = WidgetColors(
    bg                 = ColorProvider(day = Color(0xFFF3EDF7), night = Color(0xFF1D1B20)),
    onBg               = ColorProvider(day = Color(0xFF1C1B1F), night = Color(0xFFE6E1E5)),
    subtext            = ColorProvider(day = Color(0xFF49454F), night = Color(0xFFCAC4D0)),
    primary            = ColorProvider(day = Color(0xFF6750A4), night = Color(0xFFD0BCFF)),
    onPrimary          = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF381E72)),
    primaryContainer   = ColorProvider(day = Color(0xFFEADDFF), night = Color(0xFF4F378B)),
    onPrimaryContainer = ColorProvider(day = Color(0xFF21005D), night = Color(0xFFEADDFF)),
    green              = ColorProvider(day = Color(0xFF1A6D21), night = Color(0xFF6DD58C)),
    greenContainer     = ColorProvider(day = Color(0xFFC2EDBA), night = Color(0xFF003910)),
    onGreenContainer   = ColorProvider(day = Color(0xFF002204), night = Color(0xFF6DD58C)),
    error              = ColorProvider(day = Color(0xFFB3261E), night = Color(0xFFF2B8B5)),
    errorContainer     = ColorProvider(day = Color(0xFFF9DEDC), night = Color(0xFF8C1D18)),
    onErrorContainer   = ColorProvider(day = Color(0xFF410E0B), night = Color(0xFFF9DEDC)),
    track              = ColorProvider(day = Color(0xFFE7E0EC), night = Color(0xFF49454F)),
)

private val shortDate = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

// ── Shared card shell — 28dp corners, tinted surface bg ──────────────────
// Note: content lambda uses WidgetColors receiver; do NOT use defaultWeight()
// inside WidgetCard (ColumnScope is blocked by @DslMarker). Use inline layouts
// for widgets that need defaultWeight (MonthSummary, Calendar, BudgetProgress).

@Composable
private fun WidgetCard(content: @Composable WidgetColors.() -> Unit) {
    val clr = widgetColors()
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(clr.bg)
            .cornerRadius(28.dp)
            .padding(16.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            clr.content()
        }
    }
}

/**
 * Month-over-month trend badge: an up/down arrow + percentage, coloured green when the
 * change is favourable and red when it isn't (favourability depends on [goodWhenUp]).
 */
@Composable
private fun WidgetColors.TrendRow(changePercent: Double?, goodWhenUp: Boolean) {
    if (changePercent == null) return
    val isUp  = changePercent >= 0
    val good  = if (goodWhenUp) isUp else !isUp
    val tint  = if (good) green else error
    val icon  = if (isUp) R.drawable.ic_widget_trend_up else R.drawable.ic_widget_trend_down
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            provider           = ImageProvider(icon),
            contentDescription = if (isUp) "Up" else "Down",
            modifier           = GlanceModifier.size(12.dp),
            colorFilter        = ColorFilter.tint(tint)
        )
        Spacer(GlanceModifier.width(3.dp))
        Text(
            "${"%.0f".format(abs(changePercent))}% vs last month",
            style = TextStyle(color = tint, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        )
    }
}

/** Renders a ring progress indicator to a Bitmap — Glance has no Canvas/arc drawing API. */
private fun renderProgressRing(
    sizePx: Int,
    strokePx: Float,
    fraction: Float,
    trackColor: Int,
    fillColor: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val inset  = strokePx / 2f + 2f
    val rect   = RectF(inset, inset, sizePx - inset, sizePx - inset)

    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style       = Paint.Style.STROKE
        strokeWidth = strokePx
        strokeCap   = Paint.Cap.ROUND
        color       = trackColor
    }
    canvas.drawArc(rect, 0f, 360f, false, trackPaint)

    if (fraction > 0f) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style       = Paint.Style.STROKE
            strokeWidth = strokePx
            strokeCap   = Paint.Cap.ROUND
            color       = fillColor
        }
        canvas.drawArc(rect, -90f, 360f * fraction, false, fillPaint)
    }
    return bitmap
}

// ── Total Spent ───────────────────────────────────────────────────────────

class TotalSpentWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo   = WidgetDataRepository(context)
        val symbol = repo.currencySymbol()
        val trend  = repo.totalSpentTrend()
        provideContent {
            WidgetCard {
                Box(
                    modifier = GlanceModifier
                        .background(errorContainer).cornerRadius(8.dp)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("Total Spent", style = TextStyle(color = onErrorContainer, fontSize = 10.sp, fontWeight = FontWeight.Medium))
                }
                Spacer(GlanceModifier.height(12.dp))
                Text(
                    "$symbol ${"%.2f".format(trend.current)}",
                    style = TextStyle(color = error, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(GlanceModifier.height(4.dp))
                if (trend.changePercent != null) {
                    TrendRow(trend.changePercent, goodWhenUp = false)
                } else {
                    Text("This month", style = TextStyle(color = subtext, fontSize = 11.sp))
                }
            }
        }
    }
}

class TotalSpentReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TotalSpentWidget()
}

// ── Total Earned ──────────────────────────────────────────────────────────

class TotalEarnedWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo   = WidgetDataRepository(context)
        val symbol = repo.currencySymbol()
        val trend  = repo.totalEarnedTrend()
        provideContent {
            WidgetCard {
                Box(
                    modifier = GlanceModifier
                        .background(greenContainer).cornerRadius(8.dp)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("Total Earned", style = TextStyle(color = onGreenContainer, fontSize = 10.sp, fontWeight = FontWeight.Medium))
                }
                Spacer(GlanceModifier.height(12.dp))
                Text(
                    "+$symbol ${"%.2f".format(trend.current)}",
                    style = TextStyle(color = green, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(GlanceModifier.height(4.dp))
                if (trend.changePercent != null) {
                    TrendRow(trend.changePercent, goodWhenUp = true)
                } else {
                    Text("This month", style = TextStyle(color = subtext, fontSize = 11.sp))
                }
            }
        }
    }
}

class TotalEarnedReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TotalEarnedWidget()
}

// ── Total Savings ─────────────────────────────────────────────────────────

class TotalSavingsWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo   = WidgetDataRepository(context)
        val symbol = repo.currencySymbol()
        val total  = repo.totalSavings()
        provideContent {
            WidgetCard {
                Box(
                    modifier = GlanceModifier
                        .background(primaryContainer).cornerRadius(8.dp)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("Total Savings", style = TextStyle(color = onPrimaryContainer, fontSize = 10.sp, fontWeight = FontWeight.Medium))
                }
                Spacer(GlanceModifier.height(12.dp))
                Text(
                    "$symbol ${"%.2f".format(total)}",
                    style = TextStyle(color = primary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(GlanceModifier.height(4.dp))
                Text("Savings & investments", style = TextStyle(color = subtext, fontSize = 11.sp))
            }
        }
    }
}

class TotalSavingsReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TotalSavingsWidget()
}

// ── Highest Expense ───────────────────────────────────────────────────────

class HighestExpenseWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo   = WidgetDataRepository(context)
        val symbol = repo.currencySymbol()
        val txn    = repo.highestExpense()
        provideContent {
            WidgetCard {
                Box(
                    modifier = GlanceModifier
                        .background(errorContainer).cornerRadius(8.dp)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("Highest Expense", style = TextStyle(color = onErrorContainer, fontSize = 10.sp, fontWeight = FontWeight.Medium))
                }
                Spacer(GlanceModifier.height(12.dp))
                if (txn == null) {
                    Text("No transactions", style = TextStyle(color = subtext, fontSize = 13.sp))
                } else {
                    Text(txn.title, style = TextStyle(color = onBg, fontSize = 12.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        "-$symbol ${"%.2f".format(txn.amount)}",
                        style = TextStyle(color = error, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(GlanceModifier.height(3.dp))
                    Text(txn.date.format(shortDate), style = TextStyle(color = subtext, fontSize = 11.sp))
                }
            }
        }
    }
}

class HighestExpenseReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HighestExpenseWidget()
}

// ── Highest Income ────────────────────────────────────────────────────────

class HighestIncomeWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo   = WidgetDataRepository(context)
        val symbol = repo.currencySymbol()
        val txn    = repo.highestIncome()
        provideContent {
            WidgetCard {
                Box(
                    modifier = GlanceModifier
                        .background(greenContainer).cornerRadius(8.dp)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("Highest Income", style = TextStyle(color = onGreenContainer, fontSize = 10.sp, fontWeight = FontWeight.Medium))
                }
                Spacer(GlanceModifier.height(12.dp))
                if (txn == null) {
                    Text("No transactions", style = TextStyle(color = subtext, fontSize = 13.sp))
                } else {
                    Text(txn.title, style = TextStyle(color = onBg, fontSize = 12.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        "+$symbol ${"%.2f".format(txn.amount)}",
                        style = TextStyle(color = green, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(GlanceModifier.height(3.dp))
                    Text(txn.date.format(shortDate), style = TextStyle(color = subtext, fontSize = 11.sp))
                }
            }
        }
    }
}

class HighestIncomeReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HighestIncomeWidget()
}

// ── Budget Progress ───────────────────────────────────────────────────────
// Uses inline layout for defaultWeight() support. Progress is drawn as a ring
// (rendered to a Bitmap with android.graphics.Canvas — Glance has no arc API).

class BudgetProgressWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo     = WidgetDataRepository(context)
        val symbol   = repo.currencySymbol()
        val progress = repo.budgetProgress()
        provideContent {
            val clr = widgetColors()
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(clr.bg)
                    .cornerRadius(28.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    // Label chip
                    Box(
                        modifier = GlanceModifier
                            .background(clr.primaryContainer).cornerRadius(8.dp)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("Budget Progress", style = TextStyle(color = clr.onPrimaryContainer, fontSize = 10.sp, fontWeight = FontWeight.Medium))
                    }

                    if (progress == null) {
                        Spacer(GlanceModifier.height(12.dp))
                        Text("No active budget", style = TextStyle(color = clr.subtext, fontSize = 13.sp))
                    } else {
                        val (spent, limit, budgetTitle) = progress
                        val fraction   = (spent / limit).coerceIn(0.0, 1.0).toFloat()
                        val overBudget = spent > limit
                        val fillColor  = if (overBudget || fraction > 0.85f) clr.error else clr.primary

                        Spacer(GlanceModifier.height(8.dp))
                        Text(budgetTitle, style = TextStyle(color = clr.onBg, fontSize = 14.sp, fontWeight = FontWeight.Bold), maxLines = 1)

                        Spacer(GlanceModifier.defaultWeight())

                        val ringBitmap = renderProgressRing(
                            sizePx     = 220,
                            strokePx   = 26f,
                            fraction   = fraction,
                            trackColor = clr.track.getColor(context).toArgb(),
                            fillColor  = fillColor.getColor(context).toArgb()
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    provider           = ImageProvider(ringBitmap),
                                    contentDescription = "Budget progress ring",
                                    modifier           = GlanceModifier.size(72.dp)
                                )
                                Text(
                                    "${(fraction * 100).toInt()}%",
                                    style = TextStyle(
                                        color      = if (overBudget) clr.error else clr.onBg,
                                        fontSize   = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(GlanceModifier.width(14.dp))
                            Column {
                                Text(
                                    "$symbol ${"%.0f".format(spent)}",
                                    style = TextStyle(
                                        color      = if (overBudget) clr.error else clr.onBg,
                                        fontSize   = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    "of $symbol ${"%.0f".format(limit)}",
                                    style = TextStyle(color = clr.subtext, fontSize = 11.sp)
                                )
                            }
                        }

                        Spacer(GlanceModifier.defaultWeight())
                    }
                }
            }
        }
    }
}

class BudgetProgressReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BudgetProgressWidget()
}

// ── Month Summary ─────────────────────────────────────────────────────────
// Uses inline layout for defaultWeight() support.

class MonthSummaryWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo    = WidgetDataRepository(context)
        val symbol  = repo.currencySymbol()
        val summary = repo.monthSummary()
        provideContent {
            val clr = widgetColors()

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(clr.bg)
                    .cornerRadius(28.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    // ── Two M3E colour-block columns ──────────────────
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        Column(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .background(clr.errorContainer)
                                .cornerRadius(16.dp)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text("Total spent", style = TextStyle(color = clr.onErrorContainer, fontSize = 10.sp))
                            Spacer(GlanceModifier.height(4.dp))
                            Text(
                                "$symbol ${"%.2f".format(summary.spentTrend.current)}",
                                style = TextStyle(color = clr.error, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            )
                            if (summary.spentTrend.changePercent != null) {
                                Spacer(GlanceModifier.height(3.dp))
                                clr.TrendRow(summary.spentTrend.changePercent, goodWhenUp = false)
                            }
                        }
                        Spacer(GlanceModifier.width(8.dp))
                        Column(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .background(clr.greenContainer)
                                .cornerRadius(16.dp)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text("Total earned", style = TextStyle(color = clr.onGreenContainer, fontSize = 10.sp))
                            Spacer(GlanceModifier.height(4.dp))
                            Text(
                                "$symbol ${"%.2f".format(summary.earnedTrend.current)}",
                                style = TextStyle(color = clr.green, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            )
                            if (summary.earnedTrend.changePercent != null) {
                                Spacer(GlanceModifier.height(3.dp))
                                clr.TrendRow(summary.earnedTrend.changePercent, goodWhenUp = true)
                            }
                        }
                    }

                    Spacer(GlanceModifier.defaultWeight())

                    // ── Divider ────────────────────────────────────────
                    Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(clr.track)) {}

                    Spacer(GlanceModifier.defaultWeight())

                    // ── Per-payment-type breakdown ─────────────────────
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        summary.paymentRows.forEach { row ->
                            val iconRes = if (row.label == "Cash") R.drawable.ic_widget_payments
                                          else                     R.drawable.ic_widget_credit_card
                            Column(
                                modifier            = GlanceModifier.defaultWeight(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    provider           = ImageProvider(iconRes),
                                    contentDescription = row.label,
                                    modifier           = GlanceModifier.size(16.dp),
                                    colorFilter        = ColorFilter.tint(clr.subtext)
                                )
                                Spacer(GlanceModifier.height(3.dp))
                                Row {
                                    Text("$symbol${"%.0f".format(row.expense)}", style = TextStyle(color = clr.error,   fontSize = 11.sp, fontWeight = FontWeight.Medium))
                                    Text("/",                                     style = TextStyle(color = clr.subtext, fontSize = 11.sp))
                                    Text("${"%.0f".format(row.income)}",          style = TextStyle(color = clr.green,  fontSize = 11.sp, fontWeight = FontWeight.Medium))
                                }
                                Spacer(GlanceModifier.height(2.dp))
                                Text(row.label, style = TextStyle(color = clr.subtext, fontSize = 10.sp))
                            }
                        }
                    }
                }
            }
        }
    }
}

class MonthSummaryReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonthSummaryWidget()
}

// ── Calendar ──────────────────────────────────────────────────────────────
// Uses inline layout for defaultWeight() support.

class CalendarWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = WidgetDataRepository(context)
        val data = repo.calendarData()
        provideContent {
            val clr        = widgetColors()
            val availWidth = LocalSize.current.width - 32.dp   // 16dp × 2 padding
            val cellWidth  = availWidth / 7

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(clr.bg)
                    .cornerRadius(28.dp)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    // ── Month / year header in primary ────────────────
                    Row(
                        modifier            = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "${data.monthName} ${data.year}",
                            style = TextStyle(color = clr.primary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(GlanceModifier.height(10.dp))

                    // ── Day-of-week headers in primary ────────────────
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { label ->
                            Box(modifier = GlanceModifier.width(cellWidth), contentAlignment = Alignment.Center) {
                                Text(label, style = TextStyle(color = clr.primary, fontSize = 10.sp, fontWeight = FontWeight.Medium))
                            }
                        }
                    }

                    Spacer(GlanceModifier.height(6.dp))

                    // ── Day grid ───────────────────────────────────────
                    val rows = ((data.firstDayOffset + data.daysInMonth) + 6) / 7
                    repeat(rows) { row ->
                        Row(modifier = GlanceModifier.fillMaxWidth()) {
                            repeat(7) { col ->
                                val cellIndex = row * 7 + col
                                val dayNum    = cellIndex - data.firstDayOffset + 1
                                val isValid   = dayNum in 1..data.daysInMonth
                                val isToday   = isValid && dayNum == data.todayDay
                                val hasDot    = isValid && dayNum in data.daysWithTransactions

                                Box(
                                    modifier         = GlanceModifier.width(cellWidth).padding(vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isValid) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(contentAlignment = Alignment.Center) {
                                                // Halo glow behind today for a bolder, more premium highlight
                                                if (isToday) {
                                                    Box(
                                                        modifier = GlanceModifier
                                                            .size(38.dp)
                                                            .cornerRadius(19.dp)
                                                            .background(clr.primaryContainer)
                                                    ) {}
                                                }
                                                val circleMod = GlanceModifier
                                                    .size(30.dp)
                                                    .cornerRadius(15.dp)
                                                    .let { if (isToday) it.background(clr.primary) else it }
                                                Box(modifier = circleMod, contentAlignment = Alignment.Center) {
                                                    Text(
                                                        dayNum.toString(),
                                                        style = TextStyle(
                                                            color      = if (isToday) clr.onPrimary else clr.onBg,
                                                            fontSize   = 11.sp,
                                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    )
                                                }
                                            }
                                            Spacer(GlanceModifier.height(2.dp))
                                            Box(
                                                modifier = GlanceModifier
                                                    .size(5.dp)
                                                    .cornerRadius(3.dp)
                                                    .let { if (hasDot) it.background(clr.primaryContainer) else it }
                                            ) {}
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(GlanceModifier.height(2.dp))
                    }
                }
            }
        }
    }
}

class CalendarReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CalendarWidget()
}
