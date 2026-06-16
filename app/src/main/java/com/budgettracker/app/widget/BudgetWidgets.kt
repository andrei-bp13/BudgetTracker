package com.pardonmyspending.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider as GlanceColorProvider
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Adaptive colour theme ─────────────────────────────────────────────────

private class WidgetColors(
    val bg:      GlanceColorProvider,
    val onBg:    GlanceColorProvider,
    val subtext: GlanceColorProvider,
    val primary: GlanceColorProvider,
    val green:   GlanceColorProvider,
    val error:   GlanceColorProvider,
    val track:   GlanceColorProvider,
)

// ColorProvider(day, night) is @Composable — must be called inside composable scope
@Composable
private fun widgetColors() = WidgetColors(
    bg      = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF1C1B1F)),
    onBg    = ColorProvider(day = Color(0xFF1C1B1F), night = Color(0xFFE6E1E5)),
    subtext = ColorProvider(day = Color(0xFF6B7280), night = Color(0xFFA0A0A0)),
    primary = ColorProvider(day = Color(0xFF6750A4), night = Color(0xFFCEBDFF)),
    green   = ColorProvider(day = Color(0xFF2E7D32), night = Color(0xFF81C784)),
    error   = ColorProvider(day = Color(0xFFB00020), night = Color(0xFFCF6679)),
    track   = ColorProvider(day = Color(0x22000000), night = Color(0x33FFFFFF)),
)

private val shortDate = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

// ── Shared card shell ─────────────────────────────────────────────────────

@Composable
private fun WidgetCard(content: @Composable WidgetColors.() -> Unit) {
    val clr = widgetColors()
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(clr.bg)
            .cornerRadius(16.dp)
            .padding(16.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            clr.content()
        }
    }
}

// ── Total Spent ───────────────────────────────────────────────────────────

class TotalSpentWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo   = WidgetDataRepository(context)
        val symbol = repo.currencySymbol()
        val total  = repo.totalSpentThisMonth()
        provideContent {
            WidgetCard {
                Text("Total Spent", style = TextStyle(color = subtext, fontSize = 11.sp))
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    "$symbol ${"%.2f".format(total)}",
                    style = TextStyle(color = error, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(GlanceModifier.height(2.dp))
                Text("This month", style = TextStyle(color = subtext, fontSize = 11.sp))
            }
        }
    }
}

class TotalSpentReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TotalSpentWidget()
}

// ── Total Earned ──────────────────────────────────────────────────────────

class TotalEarnedWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo   = WidgetDataRepository(context)
        val symbol = repo.currencySymbol()
        val total  = repo.totalEarnedThisMonth()
        provideContent {
            WidgetCard {
                Text("Total Earned", style = TextStyle(color = subtext, fontSize = 11.sp))
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    "+$symbol ${"%.2f".format(total)}",
                    style = TextStyle(color = green, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(GlanceModifier.height(2.dp))
                Text("This month", style = TextStyle(color = subtext, fontSize = 11.sp))
            }
        }
    }
}

class TotalEarnedReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TotalEarnedWidget()
}

// ── Total Savings ─────────────────────────────────────────────────────────

class TotalSavingsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo   = WidgetDataRepository(context)
        val symbol = repo.currencySymbol()
        val total  = repo.totalSavings()
        provideContent {
            WidgetCard {
                Text("Total Savings", style = TextStyle(color = subtext, fontSize = 11.sp))
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    "$symbol ${"%.2f".format(total)}",
                    style = TextStyle(color = primary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(GlanceModifier.height(2.dp))
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
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo   = WidgetDataRepository(context)
        val symbol = repo.currencySymbol()
        val txn    = repo.highestExpense()
        provideContent {
            WidgetCard {
                Text("Highest Expense", style = TextStyle(color = subtext, fontSize = 11.sp))
                Spacer(GlanceModifier.height(4.dp))
                if (txn == null) {
                    Text("No transactions", style = TextStyle(color = onBg, fontSize = 14.sp))
                } else {
                    Text(
                        txn.title,
                        style = TextStyle(color = onBg, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        maxLines = 1
                    )
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        "-$symbol ${"%.2f".format(txn.amount)}",
                        style = TextStyle(color = error, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(GlanceModifier.height(2.dp))
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
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo   = WidgetDataRepository(context)
        val symbol = repo.currencySymbol()
        val txn    = repo.highestIncome()
        provideContent {
            WidgetCard {
                Text("Highest Income", style = TextStyle(color = subtext, fontSize = 11.sp))
                Spacer(GlanceModifier.height(4.dp))
                if (txn == null) {
                    Text("No transactions", style = TextStyle(color = onBg, fontSize = 14.sp))
                } else {
                    Text(
                        txn.title,
                        style = TextStyle(color = onBg, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        maxLines = 1
                    )
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        "+$symbol ${"%.2f".format(txn.amount)}",
                        style = TextStyle(color = green, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(GlanceModifier.height(2.dp))
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

class BudgetProgressWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo     = WidgetDataRepository(context)
        val symbol   = repo.currencySymbol()
        val progress = repo.budgetProgress()
        provideContent {
            WidgetCard {
                Text("Budget Progress", style = TextStyle(color = subtext, fontSize = 11.sp))
                Spacer(GlanceModifier.height(4.dp))
                if (progress == null) {
                    Text("No active budget", style = TextStyle(color = onBg, fontSize = 14.sp))
                } else {
                    val (spent, limit, budgetTitle) = progress
                    val fraction   = (spent / limit).coerceIn(0.0, 1.0).toFloat()
                    val overBudget = spent > limit
                    val fillColor  = if (overBudget) error else primary

                    Text(
                        budgetTitle,
                        style = TextStyle(color = onBg, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        maxLines = 1
                    )
                    Spacer(GlanceModifier.height(6.dp))

                    // Progress bar — LocalSize gives widget width; subtract horizontal padding (2×16dp)
                    val barWidth = (LocalSize.current.width - 32.dp) * fraction
                    Box(modifier = GlanceModifier.fillMaxWidth().height(8.dp)) {
                        Box(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .background(track)
                                .cornerRadius(4.dp)
                        ) {}
                        Box(
                            modifier = GlanceModifier
                                .width(barWidth)
                                .fillMaxHeight()
                                .background(fillColor)
                                .cornerRadius(4.dp)
                        ) {}
                    }

                    Spacer(GlanceModifier.height(6.dp))
                    Text(
                        "$symbol ${"%.0f".format(spent)} / $symbol ${"%.0f".format(limit)}" +
                            "  (${(fraction * 100).toInt()}%)",
                        style = TextStyle(
                            color    = if (overBudget) error else onBg,
                            fontSize = 11.sp
                        )
                    )
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
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo   = WidgetDataRepository(context)
        val symbol = repo.currencySymbol()
        val (spent, earned, paymentRows) = repo.monthSummary()
        provideContent {
            WidgetCard {
                val availWidth = LocalSize.current.width - 32.dp
                val halfWidth  = availWidth / 2
                val thirdWidth = availWidth / 3

                // ── Total Spent | Total Income ────────────────────────
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Column(modifier = GlanceModifier.width(halfWidth)) {
                        Text(
                            "Total spent this month",
                            style    = TextStyle(color = subtext, fontSize = 10.sp),
                            maxLines = 2
                        )
                        Spacer(GlanceModifier.height(3.dp))
                        Text(
                            "$symbol ${"%.2f".format(spent)}",
                            style = TextStyle(color = error, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                    Column(
                        modifier            = GlanceModifier.width(halfWidth),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            "Total income this month",
                            style    = TextStyle(color = subtext, fontSize = 10.sp),
                            maxLines = 2
                        )
                        Spacer(GlanceModifier.height(3.dp))
                        Text(
                            "$symbol ${"%.2f".format(earned)}",
                            style = TextStyle(color = green, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(GlanceModifier.height(10.dp))

                // ── Divider ───────────────────────────────────────────
                Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(track)) {}

                Spacer(GlanceModifier.height(8.dp))

                // ── Per-payment-type breakdown ────────────────────────
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    paymentRows.forEach { row ->
                        Column(
                            modifier            = GlanceModifier.width(thirdWidth),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(row.label, style = TextStyle(color = subtext, fontSize = 10.sp))
                            Spacer(GlanceModifier.height(2.dp))
                            Row {
                                Text(
                                    "$symbol${"%.0f".format(row.expense)}",
                                    style = TextStyle(color = error, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                )
                                Text("/", style = TextStyle(color = subtext, fontSize = 11.sp))
                                Text(
                                    "${"%.0f".format(row.income)}",
                                    style = TextStyle(color = green, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                )
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
