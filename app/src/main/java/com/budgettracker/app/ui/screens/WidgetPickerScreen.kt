package com.pardonmyspending.app.ui.screens

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pardonmyspending.app.widget.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerScreen(onBack: () -> Unit) {
    var expandedName by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Widgets", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "Add to Home Screen",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            items(supportedWidgets, key = { it.name }) { info ->
                ExpandableWidgetCard(
                    info       = info,
                    isExpanded = expandedName == info.name,
                    onToggle   = { expandedName = if (expandedName == info.name) null else info.name }
                )
            }

            item {
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(4.dp))
                Text(
                    "Not Available",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            items(unsupportedWidgets, key = { it.name }) { info ->
                UnsupportedWidgetCard(info)
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ── Widget catalogue ──────────────────────────────────────────────────────

private data class WidgetInfo(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val receiverClass: Class<*>
)

private val supportedWidgets = listOf(
    WidgetInfo("Monthly Summary", "Spent & earned this month",    Icons.Rounded.CalendarMonth, MonthSummaryReceiver::class.java),
    WidgetInfo("Calendar",        "Monthly calendar with dots",   Icons.Rounded.DateRange,     CalendarReceiver::class.java),
    WidgetInfo("Total Spent",     "Monthly expenses at a glance", Icons.Rounded.TrendingDown,  TotalSpentReceiver::class.java),
    WidgetInfo("Total Earned",    "Monthly income at a glance",   Icons.Rounded.TrendingUp,    TotalEarnedReceiver::class.java),
    WidgetInfo("Total Savings",   "Your cumulative savings",       Icons.Rounded.Savings,       TotalSavingsReceiver::class.java),
    WidgetInfo("Highest Expense", "Your largest single expense",  Icons.Rounded.ArrowUpward,   HighestExpenseReceiver::class.java),
    WidgetInfo("Highest Income",  "Your largest single income",   Icons.Rounded.ArrowDownward, HighestIncomeReceiver::class.java),
    WidgetInfo("Budget Progress", "Monthly budget usage",         Icons.Rounded.DonutLarge,    BudgetProgressReceiver::class.java),
)

private data class UnsupportedInfo(val name: String, val icon: ImageVector)

private val unsupportedWidgets = listOf(
    UnsupportedInfo("Monthly Progress",     Icons.Rounded.ShowChart),
    UnsupportedInfo("Expense Chart",        Icons.Rounded.BarChart),
    UnsupportedInfo("Income Chart",         Icons.Rounded.BarChart),
    UnsupportedInfo("Spending by Category", Icons.Rounded.PieChart),
    UnsupportedInfo("Income by Category",   Icons.Rounded.PieChart),
)

// ── Expandable card ───────────────────────────────────────────────────────

@Composable
private fun ExpandableWidgetCard(
    info: WidgetInfo,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val context  = LocalContext.current
    val chevron  by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "chevron")

    Card(
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape    = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // ── Collapsed header ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector        = info.icon,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary,
                    modifier           = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        info.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        info.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector        = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier.size(22.dp).rotate(chevron)
                )
            }

            // ── Expanded panel ───────────────────────────────────────────
            AnimatedVisibility(
                visible = isExpanded,
                enter   = expandVertically(tween(220)) + fadeIn(tween(220)),
                exit    = shrinkVertically(tween(180)) + fadeOut(tween(180))
            ) {
                Column(
                    modifier            = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Text(
                        "Preview",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Mock widget preview
                    WidgetPreview(info.name)

                    Button(
                        onClick  = { pinWidget(context, info.receiverClass) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape    = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Rounded.AddToHomeScreen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add to Home Screen", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

// ── Widget mock previews ──────────────────────────────────────────────────

@Composable
private fun WidgetPreview(name: String) {
    val bg             = MaterialTheme.colorScheme.surfaceContainerHigh
    val innerSurface   = MaterialTheme.colorScheme.surface
    val onBg           = MaterialTheme.colorScheme.onSurface
    val sub            = MaterialTheme.colorScheme.onSurfaceVariant
    val primary        = MaterialTheme.colorScheme.primary
    val primaryCont    = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryCont  = MaterialTheme.colorScheme.onPrimaryContainer
    val error          = MaterialTheme.colorScheme.error
    val errorCont      = MaterialTheme.colorScheme.errorContainer
    val onErrorCont    = MaterialTheme.colorScheme.onErrorContainer
    val green          = Color(0xFF18794E)
    val greenCont      = Color(0xFFDDF7E8)
    val onGreenCont    = Color(0xFF0E5A36)
    val track          = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .padding(16.dp)
    ) {
        when (name) {
            "Monthly Summary" -> Column(
                modifier            = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        java.time.LocalDate.now().month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()),
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = onBg
                    )
                    PreviewPill("Net +$1,860", greenCont, onGreenCont)
                }
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(errorCont)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text("Spent", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = onErrorCont)
                        Text("$1,240.50", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = error)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(greenCont)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text("Earned", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = onGreenCont)
                        Text("$3,100.00", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = green)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(innerSurface)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    listOf(
                        Triple("Debit", "$740", "2,400"),
                        Triple("Credit", "$380", "0"),
                        Triple("Cash", "$120", "700")
                    ).forEach { (label, exp, inc) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = sub)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(exp, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = error)
                                Text("/", fontSize = 11.sp, color = sub)
                                Text(inc, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = green)
                            }
                        }
                    }
                }
            }
            "Calendar" -> CalendarPreview(primary = primary, primaryCont = primaryCont, onPrimaryCont = onPrimaryCont, surface = innerSurface, sub = sub, onBg = onBg)
            "Total Spent" -> SimpleValuePreview(
                pillText = "Spent",
                pillBg   = errorCont,
                pillFg   = onErrorCont,
                caption  = "This month",
                value    = "$1,240.50",
                trend    = "12% vs last mo",
                trendBg  = greenCont,
                trendFg  = onGreenCont,
                color    = error,
                sub      = sub
            )
            "Total Earned" -> SimpleValuePreview(
                pillText = "Earned",
                pillBg   = greenCont,
                pillFg   = onGreenCont,
                caption  = "This month",
                value    = "+$3,100.00",
                trend    = "8% vs last mo",
                trendBg  = greenCont,
                trendFg  = onGreenCont,
                color    = green,
                sub      = sub
            )
            "Total Savings" -> SimpleValuePreview(
                pillText = "Savings",
                pillBg   = primaryCont,
                pillFg   = onPrimaryCont,
                caption  = "All time",
                value    = "$8,450.00",
                trend    = null,
                trendBg  = primaryCont,
                trendFg  = onPrimaryCont,
                color    = primary,
                sub      = sub,
                subtitle = "Savings & investments"
            )
            "Highest Expense" -> TwoLineValuePreview(
                pillText = "Top Expense",
                pillBg   = errorCont,
                pillFg   = onErrorCont,
                date     = "Nov 14",
                value    = "-$420.00",
                subtitle = "Flight tickets · Travel",
                color    = error,
                onBg     = onBg,
                sub      = sub
            )
            "Highest Income" -> TwoLineValuePreview(
                pillText = "Top Income",
                pillBg   = greenCont,
                pillFg   = onGreenCont,
                date     = "Nov 1",
                value    = "+$2,800.00",
                subtitle = "Monthly Salary · Salary",
                color    = green,
                onBg     = onBg,
                sub      = sub
            )
            "Budget Progress" -> BudgetProgressPreview(
                primary       = primary,
                primaryCont   = primaryCont,
                onPrimaryCont = onPrimaryCont,
                greenCont     = greenCont,
                onGreenCont   = onGreenCont,
                onBg          = onBg,
                sub           = sub,
                track         = track
            )
        }
    }
}

@Composable
private fun PreviewPill(text: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

@Composable
private fun SimpleValuePreview(
    pillText: String, pillBg: Color, pillFg: Color,
    caption: String, value: String,
    trend: String?, trendBg: Color, trendFg: Color,
    color: Color, sub: Color, subtitle: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            PreviewPill(pillText, pillBg, pillFg)
            Text(caption, fontSize = 10.sp, color = sub)
        }
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
        if (trend != null) {
            PreviewPill(trend, trendBg, trendFg)
        } else if (subtitle != null) {
            Text(subtitle, fontSize = 11.sp, color = sub)
        }
    }
}

@Composable
private fun TwoLineValuePreview(
    pillText: String, pillBg: Color, pillFg: Color,
    date: String, value: String, subtitle: String,
    color: Color, onBg: Color, sub: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            PreviewPill(pillText, pillBg, pillFg)
            Text(date, fontSize = 10.sp, color = sub)
        }
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        Text(subtitle, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = onBg)
    }
}

@Composable
private fun CalendarPreview(
    primary: Color, primaryCont: Color, onPrimaryCont: Color,
    surface: Color, sub: Color, onBg: Color
) {
    val today = java.time.LocalDate.now()
    val ym    = java.time.YearMonth.now()
    val firstDayOffset = ym.atDay(1).dayOfWeek.value - 1
    val daysInMonth    = ym.lengthOfMonth()
    val rows           = ((firstDayOffset + daysInMonth) + 6) / 7
    val mockDots       = setOf(3, 7, 12, 15, 20, 24)

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PreviewPill(
                "${ym.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())} ${ym.year}",
                primaryCont, onPrimaryCont
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { d ->
                Text(d, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = sub)
            }
        }
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val cellIndex = row * 7 + col
                    val dayNum    = cellIndex - firstDayOffset + 1
                    val isValid   = dayNum in 1..daysInMonth
                    val isToday   = isValid && dayNum == today.dayOfMonth
                    val hasDot    = isValid && dayNum in mockDots
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (isValid) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(
                                            when {
                                                isToday -> primary
                                                hasDot  -> surface
                                                else    -> Color.Transparent
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        dayNum.toString(),
                                        fontSize   = 10.sp,
                                        fontWeight = if (isToday || hasDot) FontWeight.Bold else FontWeight.Normal,
                                        color      = if (isToday) Color.White else onBg
                                    )
                                }
                                Spacer(Modifier.height(1.dp))
                                Box(
                                    modifier = Modifier
                                        .size(3.5.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(if (hasDot) primary else Color.Transparent)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetProgressPreview(
    primary: Color, primaryCont: Color, onPrimaryCont: Color,
    greenCont: Color, onGreenCont: Color,
    onBg: Color, sub: Color, track: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            PreviewPill("Groceries", primaryCont, onPrimaryCont)
            PreviewPill("$180 left", greenCont, onGreenCont)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(54.dp)) {
                CircularProgressIndicator(
                    progress   = { 0.64f },
                    modifier   = Modifier.fillMaxSize(),
                    color      = primary,
                    trackColor = track,
                    strokeWidth = 6.dp
                )
                Text("64%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = onBg)
            }
            Column {
                Text("$320", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = onBg)
                Text("of $500", fontSize = 11.sp, color = sub)
            }
        }
    }
}

// ── Unsupported card ──────────────────────────────────────────────────────

@Composable
private fun UnsupportedWidgetCard(info: UnsupportedInfo) {
    Card(
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape    = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector        = info.icon,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier           = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    info.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Graph widgets are not supported",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    "Not available",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ── Launcher pin helper ───────────────────────────────────────────────────

private fun pinWidget(context: Context, receiverClass: Class<*>) {
    val manager  = AppWidgetManager.getInstance(context)
    val provider = ComponentName(context, receiverClass)
    if (manager.isRequestPinAppWidgetSupported) {
        manager.requestPinAppWidget(provider, null, null)
    }
}
