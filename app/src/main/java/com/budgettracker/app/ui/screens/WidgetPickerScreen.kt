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
    val bg       = MaterialTheme.colorScheme.surface
    val onBg     = MaterialTheme.colorScheme.onSurface
    val sub      = MaterialTheme.colorScheme.onSurfaceVariant
    val primary  = MaterialTheme.colorScheme.primary
    val error    = MaterialTheme.colorScheme.error
    val green    = Color(0xFF2E7D32)
    val track    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(16.dp)
    ) {
        when (name) {
            "Monthly Summary" -> Column(
                modifier            = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top row: Total Spent | Total Income
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.Top
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Total spent this month", fontSize = 10.sp, color = sub, lineHeight = 13.sp)
                        Text("$ 0.00", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = error)
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text("Total income this month", fontSize = 10.sp, color = sub, lineHeight = 13.sp)
                        Text("$ 0.00", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = green)
                    }
                }
                // Divider
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(track))
                // Payment type breakdown
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    listOf("Debit", "Credit", "Cash").forEach { label ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(label, fontSize = 10.sp, color = sub)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("$0", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = error)
                                Text("/", fontSize = 11.sp, color = sub)
                                Text("0",  fontSize = 11.sp, fontWeight = FontWeight.Medium, color = green)
                            }
                        }
                    }
                }
            }
            "Calendar" -> CalendarPreview(primary = primary, sub = sub, onBg = onBg)
            "Total Spent" -> SimpleValuePreview(
                label    = "Total Spent",
                value    = "$ 0.00",
                subtitle = "This month",
                color    = error,
                sub      = sub
            )
            "Total Earned" -> SimpleValuePreview(
                label    = "Total Earned",
                value    = "+ $ 0.00",
                subtitle = "This month",
                color    = green,
                sub      = sub
            )
            "Total Savings" -> SimpleValuePreview(
                label    = "Total Savings",
                value    = "$ 0.00",
                subtitle = "Savings & investments",
                color    = primary,
                sub      = sub
            )
            "Highest Expense" -> TwoLineValuePreview(
                label    = "Highest Expense",
                title    = "No transactions",
                value    = "",
                color    = error,
                onBg     = onBg,
                sub      = sub
            )
            "Highest Income" -> TwoLineValuePreview(
                label    = "Highest Income",
                title    = "No transactions",
                value    = "",
                color    = green,
                onBg     = onBg,
                sub      = sub
            )
            "Budget Progress" -> BudgetProgressPreview(
                primary = primary,
                onBg    = onBg,
                sub     = sub,
                track   = track
            )
        }
    }
}

@Composable
private fun SimpleValuePreview(
    label: String, value: String, subtitle: String,
    color: Color, sub: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label,   fontSize = 11.sp, color = sub)
        Text(value,   fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        Text(subtitle, fontSize = 11.sp, color = sub)
    }
}

@Composable
private fun TwoLineValuePreview(
    label: String, title: String, value: String,
    color: Color, onBg: Color, sub: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, fontSize = 11.sp, color = sub)
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = onBg)
        if (value.isNotEmpty())
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun CalendarPreview(primary: Color, sub: Color, onBg: Color) {
    val today = java.time.LocalDate.now()
    val ym    = java.time.YearMonth.now()
    val firstDayOffset = ym.atDay(1).dayOfWeek.value - 1
    val daysInMonth    = ym.lengthOfMonth()
    val rows           = ((firstDayOffset + daysInMonth) + 6) / 7
    val mockDots       = setOf(3, 7, 12, 15, 20, 24)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "${ym.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())} ${ym.year}",
            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = onBg
        )
        Spacer(Modifier.height(2.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { d ->
                Text(d, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 9.sp, color = sub)
            }
        }
        Spacer(Modifier.height(2.dp))
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
                                        .size(22.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(if (isToday) primary else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        dayNum.toString(),
                                        fontSize = 9.sp,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isToday) Color.White else onBg
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(3.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(if (hasDot) primary else Color.Transparent)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun BudgetProgressPreview(
    primary: Color, onBg: Color, sub: Color, track: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("Budget Progress", fontSize = 11.sp, color = sub)
        Text("No active budget", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = onBg)
        // progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(track)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.35f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(primary)
            )
        }
        Text("$ 0 / $ 0  (0%)", fontSize = 11.sp, color = onBg)
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
