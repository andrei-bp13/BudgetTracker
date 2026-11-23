package com.pardonmyspending.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
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
import com.pardonmyspending.app.MainActivity
import com.pardonmyspending.app.R
import com.pardonmyspending.app.data.model.PaymentType
import com.pardonmyspending.app.data.model.TransactionType
import com.pardonmyspending.app.ui.icons.iconById
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

const val EXTRA_CALENDAR_DATE_NAME = "open_calendar_date"
private val EXTRA_CALENDAR_DATE_KEY = ActionParameters.Key<String>(EXTRA_CALENDAR_DATE_NAME)

// ── Dynamic accent colour palette (adapts to user's chosen app accent + day/night) ──

private class WidgetColors(
    val bg:                  GlanceColorProvider,
    val surface:             GlanceColorProvider,
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

internal fun blendColor(fg: Color, bg: Color, ratio: Float): Color {
    val r = ratio.coerceIn(0f, 1f)
    return Color(
        red   = fg.red   * r + bg.red   * (1f - r),
        green = fg.green * r + bg.green * (1f - r),
        blue  = fg.blue  * r + bg.blue  * (1f - r),
        alpha = 1f
    )
}

@Composable
private fun widgetColors(seed: Color, themeMode: String = "system"): WidgetColors {
    fun cp(day: Color, night: Color): GlanceColorProvider = when (themeMode) {
        "light" -> ColorProvider(day = day, night = day)
        "dark"  -> ColorProvider(day = night, night = night)
        else    -> ColorProvider(day = day, night = night)
    }

    val lightBg      = blendColor(seed, Color(0xFFFFFFFF), 0.05f)
    val darkBg       = blendColor(seed, Color(0xFF141318), 0.08f)
    val lightSurface = blendColor(seed, Color(0xFFFFFFFF), 0.11f)
    val darkSurface  = blendColor(seed, Color(0xFF211F28), 0.14f)
    val darkPrimary  = blendColor(Color.White, seed, 0.36f)
    val lightCont    = blendColor(seed, Color(0xFFFFFFFF), 0.17f)
    val darkCont     = blendColor(seed, Color(0xFF1E1C26), 0.32f)
    val lightOnCont  = blendColor(Color(0xFF0E0E12), seed, 0.25f)
    val darkOnCont   = blendColor(Color.White, seed, 0.62f)

    return WidgetColors(
        bg                 = cp(day = lightBg,            night = darkBg),
        surface            = cp(day = lightSurface,       night = darkSurface),
        onBg               = cp(day = Color(0xFF18171D),  night = Color(0xFFF1EFF5)),
        subtext            = cp(day = Color(0xFF5D5A66),  night = Color(0xFFABA7B6)),
        primary            = cp(day = seed,               night = darkPrimary),
        onPrimary          = cp(day = Color(0xFFFFFFFF),  night = Color(0xFF141318)),
        primaryContainer   = cp(day = lightCont,          night = darkCont),
        onPrimaryContainer = cp(day = lightOnCont,        night = darkOnCont),
        green              = cp(day = Color(0xFF18794E),  night = Color(0xFF5EE3A1)),
        greenContainer     = cp(day = Color(0xFFDDF7E8),  night = Color(0xFF103323)),
        onGreenContainer   = cp(day = Color(0xFF0E5A36),  night = Color(0xFF7BEAB2)),
        error              = cp(day = Color(0xFFC62828),  night = Color(0xFFFF8A80)),
        errorContainer     = cp(day = Color(0xFFFDE7E9),  night = Color(0xFF3C1619)),
        onErrorContainer   = cp(day = Color(0xFF8E1B1B),  night = Color(0xFFFFB4AB)),
        track              = cp(day = blendColor(seed, Color(0xFFE4E1EC), 0.12f), night = Color(0xFF34313D)),
    )
}

private val shortDate       = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
private val calendarDayHdr  = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())

/** Renders a circular tinted category icon badge matching CalendarTransactionRow in the app. */
private fun renderCategoryBadgeBitmap(
    iconId: String,
    categoryColor: Color,
    sizePx: Int = 96
): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // 15% alpha tinted circle background
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = categoryColor.copy(alpha = 0.15f).toArgb()
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, bgPaint)

    // Render the exact Material Outlined ImageVector paths centered inside the circle
    val vector = iconById(iconId)
    val iconSize = sizePx * 0.54f
    val offset   = (sizePx - iconSize) / 2f
    val scaleX   = iconSize / vector.viewportWidth
    val scaleY   = iconSize / vector.viewportHeight

    val fgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = categoryColor.toArgb()
    }

    fun drawGroup(group: VectorGroup) {
        for (node in group) {
            when (node) {
                is VectorGroup -> drawGroup(node)
                is VectorPath -> {
                    val androidPath = node.pathData.toPath().asAndroidPath()
                    androidPath.fillType = if (node.pathFillType == PathFillType.EvenOdd) {
                        android.graphics.Path.FillType.EVEN_ODD
                    } else {
                        android.graphics.Path.FillType.WINDING
                    }
                    canvas.drawPath(androidPath, fgPaint)
                }
            }
        }
    }

    canvas.save()
    canvas.translate(offset, offset)
    canvas.scale(scaleX, scaleY)
    drawGroup(vector.root)
    canvas.restore()

    return bitmap
}

internal fun formatMoney(symbol: String, amount: Double): String =
    "$symbol${"%,.2f".format(Locale.getDefault(), amount)}"

internal fun formatWhole(symbol: String, amount: Double): String =
    "$symbol${"%,.0f".format(Locale.getDefault(), amount)}"

internal fun formatCompact(symbol: String, amount: Double): String =
    if (abs(amount) >= 10_000.0) "$symbol${"%.1f".format(Locale.getDefault(), amount / 1000.0)}k"
    else formatWhole(symbol, amount)

// ── Shared building blocks ────────────────────────────────────────────────

@Composable
private fun WidgetShell(
    clr: WidgetColors,
    tapToOpen: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val rootMod = GlanceModifier
        .fillMaxSize()
        .background(clr.bg)
        .cornerRadius(28.dp)
        .let { if (tapToOpen) it.clickable(actionStartActivity<MainActivity>()) else it }
        .padding(16.dp)
    Box(
        modifier = rootMod,
        contentAlignment = Alignment.TopStart
    ) {
        Column(modifier = GlanceModifier.fillMaxSize(), content = content)
    }
}

@Composable
private fun LabelPill(
    text: String,
    bg: GlanceColorProvider,
    fg: GlanceColorProvider,
    iconRes: Int? = null
) {
    Row(
        modifier = GlanceModifier
            .background(bg)
            .cornerRadius(10.dp)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconRes != null) {
            Image(
                provider           = ImageProvider(iconRes),
                contentDescription = null,
                modifier           = GlanceModifier.size(12.dp),
                colorFilter        = ColorFilter.tint(fg)
            )
            Spacer(GlanceModifier.width(4.dp))
        }
        Text(
            text  = text,
            style = TextStyle(color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold),
            maxLines = 1
        )
    }
}

/** Month-over-month trend pill: arrow + percentage inside a subtle tinted container. */
@Composable
private fun TrendBadge(
    clr: WidgetColors,
    changePercent: Double?,
    goodWhenUp: Boolean,
    compact: Boolean = false
) {
    if (changePercent == null) return
    val isUp = changePercent >= 0
    val good = if (goodWhenUp) isUp else !isUp
    val fg   = if (good) clr.onGreenContainer else clr.onErrorContainer
    val bg   = if (good) clr.greenContainer else clr.errorContainer
    val icon = if (isUp) R.drawable.ic_widget_trend_up else R.drawable.ic_widget_trend_down
    val suffix = if (compact) "%" else "% vs last mo"

    Row(
        modifier = GlanceModifier
            .background(bg)
            .cornerRadius(8.dp)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            provider           = ImageProvider(icon),
            contentDescription = if (isUp) "Up" else "Down",
            modifier           = GlanceModifier.size(11.dp),
            colorFilter        = ColorFilter.tint(fg)
        )
        Spacer(GlanceModifier.width(3.dp))
        Text(
            text  = "${"%.0f".format(abs(changePercent))}$suffix",
            style = TextStyle(color = fg, fontSize = 10.sp, fontWeight = FontWeight.Medium),
            maxLines = 1
        )
    }
}

/** Renders a crisp ring progress indicator to a Bitmap — Glance has no Canvas/arc drawing API. */
private fun renderProgressRing(
    sizePx: Int,
    strokePx: Float,
    fraction: Float,
    trackColor: Int,
    fillColor: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val inset  = strokePx / 2f + 4f
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
        canvas.drawArc(rect, -90f, 360f * fraction.coerceIn(0f, 1f), false, fillPaint)
    }
    return bitmap
}

// ── Total Spent ───────────────────────────────────────────────────────────

class TotalSpentWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo      = WidgetDataRepository(context)
        val seed      = repo.accentSeedColor()
        val themeMode = repo.themeMode()
        val symbol    = repo.currencySymbol()
        val trend     = repo.totalSpentTrend()
        provideContent {
            val clr = widgetColors(seed, themeMode)
            WidgetShell(clr) {
                Row(
                    modifier          = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LabelPill(
                        text    = "Spent",
                        bg      = clr.errorContainer,
                        fg      = clr.onErrorContainer,
                        iconRes = R.drawable.ic_widget_trend_down
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    Text("This month", style = TextStyle(color = clr.subtext, fontSize = 10.sp))
                }
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    text     = formatMoney(symbol, trend.current),
                    style    = TextStyle(color = clr.error, fontSize = 26.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                if (trend.changePercent != null) {
                    Spacer(GlanceModifier.height(6.dp))
                    TrendBadge(clr, trend.changePercent, goodWhenUp = false)
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
        val repo      = WidgetDataRepository(context)
        val seed      = repo.accentSeedColor()
        val themeMode = repo.themeMode()
        val symbol    = repo.currencySymbol()
        val trend     = repo.totalEarnedTrend()
        provideContent {
            val clr = widgetColors(seed, themeMode)
            WidgetShell(clr) {
                Row(
                    modifier          = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LabelPill(
                        text    = "Earned",
                        bg      = clr.greenContainer,
                        fg      = clr.onGreenContainer,
                        iconRes = R.drawable.ic_widget_trend_up
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    Text("This month", style = TextStyle(color = clr.subtext, fontSize = 10.sp))
                }
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    text     = "+${formatMoney(symbol, trend.current)}",
                    style    = TextStyle(color = clr.green, fontSize = 26.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                if (trend.changePercent != null) {
                    Spacer(GlanceModifier.height(6.dp))
                    TrendBadge(clr, trend.changePercent, goodWhenUp = true)
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
        val repo      = WidgetDataRepository(context)
        val seed      = repo.accentSeedColor()
        val themeMode = repo.themeMode()
        val symbol    = repo.currencySymbol()
        val total     = repo.totalSavings()
        provideContent {
            val clr = widgetColors(seed, themeMode)
            WidgetShell(clr) {
                Row(
                    modifier          = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LabelPill(
                        text    = "Savings",
                        bg      = clr.primaryContainer,
                        fg      = clr.onPrimaryContainer,
                        iconRes = R.drawable.ic_widget_payments
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    Text("All time", style = TextStyle(color = clr.subtext, fontSize = 10.sp))
                }
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    text     = formatMoney(symbol, total),
                    style    = TextStyle(color = clr.primary, fontSize = 26.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text     = "Savings & investments",
                    style    = TextStyle(color = clr.subtext, fontSize = 11.sp),
                    maxLines = 1
                )
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
        val repo      = WidgetDataRepository(context)
        val seed      = repo.accentSeedColor()
        val themeMode = repo.themeMode()
        val symbol    = repo.currencySymbol()
        val txn       = repo.highestExpense()
        provideContent {
            val clr = widgetColors(seed, themeMode)
            WidgetShell(clr) {
                Row(
                    modifier          = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LabelPill(
                        text    = "Top Expense",
                        bg      = clr.errorContainer,
                        fg      = clr.onErrorContainer,
                        iconRes = R.drawable.ic_widget_trend_down
                    )
                    if (txn != null) {
                        Spacer(GlanceModifier.defaultWeight())
                        Text(txn.date.format(shortDate), style = TextStyle(color = clr.subtext, fontSize = 10.sp))
                    }
                }
                Spacer(GlanceModifier.defaultWeight())
                if (txn == null) {
                    Text("No expenses yet", style = TextStyle(color = clr.subtext, fontSize = 13.sp))
                } else {
                    Text(
                        text     = "-${formatMoney(symbol, txn.amount)}",
                        style    = TextStyle(color = clr.error, fontSize = 24.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        text     = "${txn.title} · ${txn.category}",
                        style    = TextStyle(color = clr.onBg, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                        maxLines = 1
                    )
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
        val repo      = WidgetDataRepository(context)
        val seed      = repo.accentSeedColor()
        val themeMode = repo.themeMode()
        val symbol    = repo.currencySymbol()
        val txn       = repo.highestIncome()
        provideContent {
            val clr = widgetColors(seed, themeMode)
            WidgetShell(clr) {
                Row(
                    modifier          = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LabelPill(
                        text    = "Top Income",
                        bg      = clr.greenContainer,
                        fg      = clr.onGreenContainer,
                        iconRes = R.drawable.ic_widget_trend_up
                    )
                    if (txn != null) {
                        Spacer(GlanceModifier.defaultWeight())
                        Text(txn.date.format(shortDate), style = TextStyle(color = clr.subtext, fontSize = 10.sp))
                    }
                }
                Spacer(GlanceModifier.defaultWeight())
                if (txn == null) {
                    Text("No income yet", style = TextStyle(color = clr.subtext, fontSize = 13.sp))
                } else {
                    Text(
                        text     = "+${formatMoney(symbol, txn.amount)}",
                        style    = TextStyle(color = clr.green, fontSize = 24.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        text     = "${txn.title} · ${txn.category}",
                        style    = TextStyle(color = clr.onBg, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

class HighestIncomeReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HighestIncomeWidget()
}

// ── Budget Progress ───────────────────────────────────────────────────────

class BudgetProgressWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo      = WidgetDataRepository(context)
        val seed      = repo.accentSeedColor()
        val themeMode = repo.themeMode()
        val symbol    = repo.currencySymbol()
        val progress  = repo.budgetProgress()
        provideContent {
            val clr = widgetColors(seed, themeMode)
            WidgetShell(clr) {
                if (progress == null) {
                    LabelPill(text = "Budget", bg = clr.primaryContainer, fg = clr.onPrimaryContainer)
                    Spacer(GlanceModifier.defaultWeight())
                    Text("No active budget", style = TextStyle(color = clr.subtext, fontSize = 13.sp))
                } else {
                    val (spent, limit, budgetTitle) = progress
                    val fraction   = if (limit > 0.0) (spent / limit).coerceIn(0.0, 1.0).toFloat() else 0f
                    val overBudget = limit > 0.0 && spent > limit
                    val nearLimit  = !overBudget && fraction > 0.85f
                    val fillColor  = if (overBudget || nearLimit) clr.error else clr.primary
                    val remaining  = abs(limit - spent)

                    Row(
                        modifier          = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LabelPill(text = budgetTitle, bg = clr.primaryContainer, fg = clr.onPrimaryContainer)
                        Spacer(GlanceModifier.defaultWeight())
                        LabelPill(
                            text = if (overBudget) "${formatCompact(symbol, remaining)} over"
                                   else "${formatCompact(symbol, remaining)} left",
                            bg   = if (overBudget) clr.errorContainer else clr.greenContainer,
                            fg   = if (overBudget) clr.onErrorContainer else clr.onGreenContainer
                        )
                    }

                    Spacer(GlanceModifier.defaultWeight())

                    val ringBitmap = renderProgressRing(
                        sizePx     = 260,
                        strokePx   = 28f,
                        fraction   = fraction,
                        trackColor = clr.track.getColor(context).toArgb(),
                        fillColor  = fillColor.getColor(context).toArgb()
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center) {
                            Image(
                                provider           = ImageProvider(ringBitmap),
                                contentDescription = "Budget progress ring",
                                modifier           = GlanceModifier.size(66.dp)
                            )
                            Text(
                                text  = "${(fraction * 100).toInt()}%",
                                style = TextStyle(
                                    color      = if (overBudget) clr.error else clr.onBg,
                                    fontSize   = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Spacer(GlanceModifier.width(12.dp))
                        Column {
                            Text(
                                text     = formatWhole(symbol, spent),
                                style    = TextStyle(
                                    color      = if (overBudget) clr.error else clr.onBg,
                                    fontSize   = 20.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                            Text(
                                text     = "of ${formatWhole(symbol, limit)}",
                                style    = TextStyle(color = clr.subtext, fontSize = 11.sp),
                                maxLines = 1
                            )
                        }
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

class MonthSummaryWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo      = WidgetDataRepository(context)
        val seed      = repo.accentSeedColor()
        val themeMode = repo.themeMode()
        val symbol    = repo.currencySymbol()
        val summary   = repo.monthSummary()
        provideContent {
            val clr = widgetColors(seed, themeMode)
            val netPositive = summary.net >= 0.0
            val netPrefix   = if (netPositive) "+" else "-"

            WidgetShell(clr) {
                // ── Month title + Net badge ──────────────────────────────
                Row(
                    modifier          = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text     = summary.monthLabel,
                        style    = TextStyle(color = clr.onBg, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    LabelPill(
                        text = "Net $netPrefix${formatCompact(symbol, abs(summary.net))}",
                        bg   = if (netPositive) clr.greenContainer else clr.errorContainer,
                        fg   = if (netPositive) clr.onGreenContainer else clr.onErrorContainer
                    )
                }

                Spacer(GlanceModifier.height(10.dp))

                // ── Two tonal summary cards ──────────────────────────────
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Column(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .background(clr.errorContainer)
                            .cornerRadius(16.dp)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text("Spent", style = TextStyle(color = clr.onErrorContainer, fontSize = 10.sp, fontWeight = FontWeight.Medium))
                        Spacer(GlanceModifier.height(3.dp))
                        Text(
                            text     = formatMoney(symbol, summary.spentTrend.current),
                            style    = TextStyle(color = clr.error, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        if (summary.spentTrend.changePercent != null) {
                            Spacer(GlanceModifier.height(4.dp))
                            TrendBadge(clr, summary.spentTrend.changePercent, goodWhenUp = false, compact = true)
                        }
                    }
                    Spacer(GlanceModifier.width(8.dp))
                    Column(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .background(clr.greenContainer)
                            .cornerRadius(16.dp)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text("Earned", style = TextStyle(color = clr.onGreenContainer, fontSize = 10.sp, fontWeight = FontWeight.Medium))
                        Spacer(GlanceModifier.height(3.dp))
                        Text(
                            text     = formatMoney(symbol, summary.earnedTrend.current),
                            style    = TextStyle(color = clr.green, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        if (summary.earnedTrend.changePercent != null) {
                            Spacer(GlanceModifier.height(4.dp))
                            TrendBadge(clr, summary.earnedTrend.changePercent, goodWhenUp = true, compact = true)
                        }
                    }
                }

                Spacer(GlanceModifier.defaultWeight())

                // ── Per-payment-type breakdown inside tonal bar ──────────
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(clr.surface)
                        .cornerRadius(16.dp)
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    summary.paymentRows.forEach { row ->
                        val iconRes = if (row.label == "Cash") R.drawable.ic_widget_payments
                                      else                     R.drawable.ic_widget_credit_card
                        Column(
                            modifier            = GlanceModifier.defaultWeight(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    provider           = ImageProvider(iconRes),
                                    contentDescription = row.label,
                                    modifier           = GlanceModifier.size(13.dp),
                                    colorFilter        = ColorFilter.tint(clr.subtext)
                                )
                                Spacer(GlanceModifier.width(3.dp))
                                Text(row.label, style = TextStyle(color = clr.subtext, fontSize = 10.sp, fontWeight = FontWeight.Medium))
                            }
                            Spacer(GlanceModifier.height(3.dp))
                            Row {
                                Text(formatCompact(symbol, row.expense), style = TextStyle(color = clr.error, fontSize = 11.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                                Text("/", style = TextStyle(color = clr.subtext, fontSize = 11.sp))
                                Text(formatCompact("", row.income), style = TextStyle(color = clr.green, fontSize = 11.sp, fontWeight = FontWeight.Bold), maxLines = 1)
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

class CalendarWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo      = WidgetDataRepository(context)
        val seed      = repo.accentSeedColor()
        val themeMode = repo.themeMode()
        val symbol    = repo.currencySymbol()
        val data      = repo.calendarData()

        // Pre-render exact app category icon badges off-composition
        val badgeBitmaps: Map<String, Bitmap> = data.todayTransactions.associate { txn ->
            txn.id to renderCategoryBadgeBitmap(txn.categoryIconId, txn.categoryColor)
        }

        provideContent {
            val clr        = widgetColors(seed, themeMode)
            val availWidth = LocalSize.current.width - 32.dp   // 16dp × 2 padding
            val cellWidth  = availWidth / 7
            val openTodayAction = actionStartActivity<MainActivity>(
                actionParametersOf(EXTRA_CALENDAR_DATE_KEY to data.todayDate.toString())
            )

            WidgetShell(clr, tapToOpen = false) {
                // ── 1. Month / year pill header ──────────────────────────
                Row(
                    modifier            = GlanceModifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable(openTodayAction),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LabelPill(
                        text = "${data.monthName} ${data.year}",
                        bg   = clr.primaryContainer,
                        fg   = clr.onPrimaryContainer
                    )
                }

                // ── 2. Day-of-week headers ───────────────────────────────
                Row(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 4.dp)) {
                    listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { label ->
                        Box(modifier = GlanceModifier.width(cellWidth), contentAlignment = Alignment.Center) {
                            Text(label, style = TextStyle(color = clr.subtext, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                        }
                    }
                }

                // ── 3. Day grid (max 6 week rows; each day opens Calendar at that date) ──
                val rows = ((data.firstDayOffset + data.daysInMonth) + 6) / 7
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    repeat(rows) { row ->
                        Row(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 1.dp)) {
                            repeat(7) { col ->
                                val cellIndex = row * 7 + col
                                val dayNum    = cellIndex - data.firstDayOffset + 1
                                val isValid   = dayNum in 1..data.daysInMonth
                                val isToday   = isValid && dayNum == data.todayDay
                                val hasDot    = isValid && dayNum in data.daysWithTransactions

                                val cellMod = GlanceModifier
                                    .width(cellWidth)
                                    .padding(vertical = 1.dp)
                                    .let {
                                        if (isValid) {
                                            val dateStr = LocalDate.of(data.year, data.month, dayNum).toString()
                                            it.clickable(
                                                actionStartActivity<MainActivity>(
                                                    actionParametersOf(EXTRA_CALENDAR_DATE_KEY to dateStr)
                                                )
                                            )
                                        } else it
                                    }

                                Box(
                                    modifier         = cellMod,
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isValid) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            val circleMod = GlanceModifier
                                                .size(26.dp)
                                                .cornerRadius(13.dp)
                                                .let {
                                                    when {
                                                        isToday -> it.background(clr.primary)
                                                        hasDot  -> it.background(clr.surface)
                                                        else    -> it
                                                    }
                                                }
                                            Box(modifier = circleMod, contentAlignment = Alignment.Center) {
                                                Text(
                                                    text  = dayNum.toString(),
                                                    style = TextStyle(
                                                        color      = if (isToday) clr.onPrimary else clr.onBg,
                                                        fontSize   = 11.sp,
                                                        fontWeight = if (isToday || hasDot) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                )
                                            }
                                            Spacer(GlanceModifier.height(1.dp))
                                            Box(
                                                modifier = GlanceModifier
                                                    .size(4.dp)
                                                    .cornerRadius(2.dp)
                                                    .let { if (hasDot) it.background(clr.primary) else it }
                                            ) {}
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 4. Divider + Today's date header ─────────────────────
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 4.dp)
                        .clickable(openTodayAction)
                ) {
                    Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(clr.track)) {}
                    Spacer(GlanceModifier.height(6.dp))
                    Text(
                        text     = data.todayDate.format(calendarDayHdr),
                        style    = TextStyle(color = clr.subtext, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }

                // ── 5. Scrollable list of today's transactions ───────────
                if (data.todayTransactions.isEmpty()) {
                    Box(
                        modifier         = GlanceModifier.fillMaxWidth().defaultWeight().clickable(openTodayAction),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text  = "No transactions today",
                            style = TextStyle(color = clr.subtext, fontSize = 11.sp)
                        )
                    }
                } else {
                    LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                        items(data.todayTransactions, itemId = { it.id.hashCode().toLong() }) { txn ->
                            val badge      = badgeBitmaps[txn.id]
                            val payIconRes = if (txn.paymentType == PaymentType.CASH) R.drawable.ic_widget_payments
                                             else                                     R.drawable.ic_widget_credit_card
                            val payLabel   = txn.paymentType.name.lowercase().replaceFirstChar { it.titlecase() }
                            val isExpense  = txn.type == TransactionType.EXPENSE
                            val amtColor   = if (isExpense) clr.error
                                             else ColorProvider(day = txn.categoryColor, night = txn.categoryColor)

                            Column(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .clickable(openTodayAction)
                            ) {
                                Row(
                                    modifier          = GlanceModifier.fillMaxWidth().padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (badge != null) {
                                        Image(
                                            provider           = ImageProvider(badge),
                                            contentDescription = txn.category,
                                            modifier           = GlanceModifier.size(32.dp)
                                        )
                                        Spacer(GlanceModifier.width(10.dp))
                                    }
                                    Column(modifier = GlanceModifier.defaultWeight()) {
                                        Text(
                                            text     = txn.title,
                                            style    = TextStyle(color = clr.onBg, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                            maxLines = 1
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text     = "${txn.category} · ",
                                                style    = TextStyle(color = clr.subtext, fontSize = 10.sp),
                                                maxLines = 1
                                            )
                                            Image(
                                                provider           = ImageProvider(payIconRes),
                                                contentDescription = payLabel,
                                                modifier           = GlanceModifier.size(11.dp),
                                                colorFilter        = ColorFilter.tint(clr.subtext)
                                            )
                                            Spacer(GlanceModifier.width(3.dp))
                                            Text(
                                                text     = payLabel,
                                                style    = TextStyle(color = clr.subtext, fontSize = 10.sp),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    Spacer(GlanceModifier.width(6.dp))
                                    Text(
                                        text     = "${if (isExpense) "-" else "+"}${formatMoney(symbol, txn.amount)}",
                                        style    = TextStyle(color = amtColor, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                        maxLines = 1
                                    )
                                }
                                Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(clr.track)) {}
                            }
                        }
                    }
                }
            }
        }
    }
}

class CalendarReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CalendarWidget()
}

object WidgetUpdater {
    private val allWidgets: List<GlanceAppWidget> = listOf(
        TotalSpentWidget(),
        TotalEarnedWidget(),
        TotalSavingsWidget(),
        HighestExpenseWidget(),
        HighestIncomeWidget(),
        BudgetProgressWidget(),
        MonthSummaryWidget(),
        CalendarWidget()
    )

    suspend fun updateAllWidgets(context: Context) {
        val appCtx = context.applicationContext
        for (widget in allWidgets) {
            runCatching { widget.updateAll(appCtx) }
        }
    }
}
