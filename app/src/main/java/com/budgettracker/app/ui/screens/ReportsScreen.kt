package com.budgettracker.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Refresh
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
import com.budgettracker.app.data.model.Transaction
import com.budgettracker.app.data.model.TransactionType
import com.budgettracker.app.ui.theme.LocalCurrencySymbol
import com.budgettracker.app.util.CatPeriod
import com.budgettracker.app.util.catPeriods
import com.budgettracker.app.util.computeCategoryTotals
import com.budgettracker.app.util.computeMonthlyBars
import com.budgettracker.app.util.computeWeeklyBars
import com.budgettracker.app.util.computeYearlyBars
import com.budgettracker.app.util.defaultCatPeriodIndex
import com.budgettracker.app.util.filterByTypeAndDateRange
import com.budgettracker.app.util.filterExpensesByDateRange
import com.budgettracker.app.util.filterInDateRange
import com.budgettracker.app.viewmodel.ChartBar
import com.budgettracker.app.viewmodel.TransactionViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

// ── Granularity options ───────────────────────────────────────────────────────

private val granularityOptions = listOf(
    Triple("weekly",  "Weekly",  "Selected month in 7-day groups"),
    Triple("monthly", "Monthly", "All months of selected year"),
    Triple("yearly",  "Yearly",  "All years"),
)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: TransactionViewModel,
    onBarClick: (ChartBar) -> Unit = {}
) {
    val granularity     by viewModel.chartGranularity.collectAsState()
    val allTransactions by viewModel.transactions.collectAsState()
    val currencySymbol  = LocalCurrencySymbol.current
    val today           = LocalDate.now()

    // ── Expenses chart context ────────────────────────────────────────────────
    var chartContextYear  by remember { mutableIntStateOf(today.year) }
    var chartContextMonth by remember { mutableIntStateOf(today.monthValue) }
    var showGranularityMenu by remember { mutableStateOf(false) }

    val chartBars = remember(allTransactions, granularity, chartContextYear, chartContextMonth) {
        when (granularity) {
            "weekly"  -> computeWeeklyBars(allTransactions, chartContextYear, chartContextMonth)
            "yearly"  -> computeYearlyBars(allTransactions)
            else      -> computeMonthlyBars(allTransactions, chartContextYear)
        }
    }

    val availableChartYears = remember(allTransactions) {
        (allTransactions.map { it.date.year } + today.year).toSortedSet().toList()
    }

    val chartTitle = when (granularity) {
        "weekly"  -> {
            val abbr = YearMonth.of(chartContextYear, chartContextMonth)
                .month.getDisplayName(JTextStyle.SHORT, Locale.getDefault())
            "Expenses — $abbr $chartContextYear by Week"
        }
        "yearly"  -> "Expenses — By Year"
        else      -> "Expenses — $chartContextYear"
    }

    // When switching granularity, carry context forward
    LaunchedEffect(granularity) {
        // contextYear/Month already hold the last selection; no reset needed
    }

    val chartChipScrollState = rememberScrollState()
    LaunchedEffect(granularity, chartContextYear, chartContextMonth) {
        val idx = when (granularity) {
            "monthly" -> availableChartYears.indexOf(chartContextYear).coerceAtLeast(0)
            "weekly"  -> chartContextMonth - 1
            else      -> 0
        }
        chartChipScrollState.animateScrollTo((idx * 58.dp.value).toInt())
    }

    // ── Category card context ─────────────────────────────────────────────────
    var catGranularity  by remember { mutableStateOf("monthly") }
    var showCatGranMenu by remember { mutableStateOf(false) }
    var catContextYear  by remember { mutableIntStateOf(today.year) }
    var catContextMonth by remember { mutableIntStateOf(today.monthValue) }

    val catPeriods = remember(catGranularity, allTransactions, catContextYear, catContextMonth) {
        catPeriods(catGranularity, allTransactions, catContextYear, catContextMonth)
    }
    var catPeriodIndex by remember { mutableIntStateOf(today.monthValue - 1) }
    LaunchedEffect(catGranularity) {
        catPeriodIndex = defaultCatPeriodIndex(catGranularity, catPeriods, catContextYear, catContextMonth)
    }

    val catChipScrollState = rememberScrollState()
    LaunchedEffect(catPeriodIndex) {
        catChipScrollState.animateScrollTo((catPeriodIndex * 58.dp.value).toInt())
    }

    val selectedCatPeriod = catPeriods.getOrNull(catPeriodIndex)
    var catDetailCategory by remember { mutableStateOf<String?>(null) }
    val catDetailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // All categories sorted — used for the detail sheet
    val catTotals = remember(allTransactions, selectedCatPeriod) {
        if (selectedCatPeriod == null) emptyList()
        else computeCategoryTotals(
            filterExpensesByDateRange(allTransactions, selectedCatPeriod.startDate, selectedCatPeriod.endDate)
        )
    }

    // Top 6 shown; remainder folded into "Other" for chart + legend
    val catDisplay = remember(catTotals) {
        val top = catTotals.take(6)
        val otherAmount = catTotals.drop(6).sumOf { it.second }
        if (otherAmount > 0.001) top + ("Other" to otherAmount) else top
    }

    val catTitle = selectedCatPeriod?.let { "Spending by Category — ${it.title}" }
        ?: "Spending by Category"

    // ── Income chart context ──────────────────────────────────────────────────
    var incomeGranularity        by remember { mutableStateOf("monthly") }
    var showIncomeGranularityMenu by remember { mutableStateOf(false) }
    var incomeChartContextYear   by remember { mutableIntStateOf(today.year) }
    var incomeChartContextMonth  by remember { mutableIntStateOf(today.monthValue) }

    val incomeChartBars = remember(allTransactions, incomeGranularity, incomeChartContextYear, incomeChartContextMonth) {
        when (incomeGranularity) {
            "weekly"  -> computeWeeklyBars(allTransactions, incomeChartContextYear, incomeChartContextMonth, TransactionType.INCOME)
            "yearly"  -> computeYearlyBars(allTransactions, type = TransactionType.INCOME)
            else      -> computeMonthlyBars(allTransactions, incomeChartContextYear, TransactionType.INCOME)
        }
    }

    val incomeChartTitle = when (incomeGranularity) {
        "weekly"  -> {
            val abbr = YearMonth.of(incomeChartContextYear, incomeChartContextMonth)
                .month.getDisplayName(JTextStyle.SHORT, Locale.getDefault())
            "Income — $abbr $incomeChartContextYear by Week"
        }
        "yearly"  -> "Income — By Year"
        else      -> "Income — $incomeChartContextYear"
    }

    val incomeChipScrollState = rememberScrollState()
    LaunchedEffect(incomeGranularity, incomeChartContextYear, incomeChartContextMonth) {
        val idx = when (incomeGranularity) {
            "monthly" -> availableChartYears.indexOf(incomeChartContextYear).coerceAtLeast(0)
            "weekly"  -> incomeChartContextMonth - 1
            else      -> 0
        }
        incomeChipScrollState.animateScrollTo((idx * 58.dp.value).toInt())
    }

    // ── Income category context ───────────────────────────────────────────────
    var incomeCatGranularity   by remember { mutableStateOf("monthly") }
    var showIncomeCatGranMenu  by remember { mutableStateOf(false) }
    var incomeCatContextYear   by remember { mutableIntStateOf(today.year) }
    var incomeCatContextMonth  by remember { mutableIntStateOf(today.monthValue) }

    val incomeCatPeriods = remember(incomeCatGranularity, allTransactions, incomeCatContextYear, incomeCatContextMonth) {
        catPeriods(incomeCatGranularity, allTransactions, incomeCatContextYear, incomeCatContextMonth)
    }
    var incomeCatPeriodIndex by remember { mutableIntStateOf(today.monthValue - 1) }
    LaunchedEffect(incomeCatGranularity) {
        incomeCatPeriodIndex = defaultCatPeriodIndex(incomeCatGranularity, incomeCatPeriods, incomeCatContextYear, incomeCatContextMonth)
    }

    val incomeCatChipScrollState = rememberScrollState()
    LaunchedEffect(incomeCatPeriodIndex) {
        incomeCatChipScrollState.animateScrollTo((incomeCatPeriodIndex * 58.dp.value).toInt())
    }

    val selectedIncomeCatPeriod = incomeCatPeriods.getOrNull(incomeCatPeriodIndex)
    var incomeCatDetailCategory by remember { mutableStateOf<String?>(null) }
    val incomeCatDetailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val incomeCatTotals = remember(allTransactions, selectedIncomeCatPeriod) {
        if (selectedIncomeCatPeriod == null) emptyList()
        else computeCategoryTotals(
            filterByTypeAndDateRange(allTransactions, selectedIncomeCatPeriod.startDate, selectedIncomeCatPeriod.endDate, TransactionType.INCOME)
        )
    }

    val incomeCatDisplay = remember(incomeCatTotals) {
        val top = incomeCatTotals.take(6)
        val otherAmount = incomeCatTotals.drop(6).sumOf { it.second }
        if (otherAmount > 0.001) top + ("Other" to otherAmount) else top
    }

    val incomeCatTitle = selectedIncomeCatPeriod?.let { "Income by Category — ${it.title}" }
        ?: "Income by Category"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Expenses bar chart ────────────────────────────────────────────────
        Card(
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape    = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                // Header
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        chartTitle,
                        style    = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick  = {
                                chartContextYear  = today.year
                                chartContextMonth = today.monthValue
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Refresh,
                                contentDescription = "Reset to current period",
                                modifier = Modifier.size(16.dp),
                                tint     = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box {
                            IconButton(
                                onClick  = { showGranularityMenu = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Tune,
                                    contentDescription = "Chart settings",
                                    modifier = Modifier.size(18.dp),
                                    tint     = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            GranularityDropdownMenu(
                                expanded    = showGranularityMenu,
                                selectedKey = granularity,
                                onDismiss   = { showGranularityMenu = false },
                                onSelect    = { viewModel.setChartGranularity(it) }
                            )
                        }
                    }
                }

                // Context chips — year selector (monthly) or month selector (weekly)
                if (granularity == "monthly" || granularity == "weekly") {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth().horizontalScroll(chartChipScrollState),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (granularity == "monthly") {
                            availableChartYears.forEach { y ->
                                FilterChip(
                                    selected = y == chartContextYear,
                                    onClick  = { chartContextYear = y },
                                    label    = { Text("$y", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        } else { // weekly
                            (1..12).forEach { m ->
                                val abbr = YearMonth.of(chartContextYear, m)
                                    .month.getDisplayName(JTextStyle.SHORT, Locale.getDefault())
                                FilterChip(
                                    selected = m == chartContextMonth,
                                    onClick  = { chartContextMonth = m },
                                    label    = { Text(abbr, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                ExpenseBarChart(
                    bars           = chartBars,
                    currencySymbol = currencySymbol,
                    onBarClick     = { bar ->
                        // Carry context forward before navigating
                        when (granularity) {
                            "yearly"  -> chartContextYear = bar.startDate.year
                            "monthly" -> {
                                chartContextYear  = bar.startDate.year
                                chartContextMonth = bar.startDate.monthValue
                            }
                        }
                        onBarClick(bar)
                    }
                )
            }
        }

        // ── Spending by category ──────────────────────────────────────────────
        Card(
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape    = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                // Header
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        catTitle,
                        style    = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick  = {
                                catContextYear  = today.year
                                catContextMonth = today.monthValue
                                catPeriodIndex  = defaultCatPeriodIndex(
                                    catGranularity, catPeriods, today.year, today.monthValue
                                )
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Refresh,
                                contentDescription = "Reset to current period",
                                modifier = Modifier.size(16.dp),
                                tint     = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box {
                            IconButton(
                                onClick  = { showCatGranMenu = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Tune,
                                    contentDescription = "Period settings",
                                    modifier = Modifier.size(18.dp),
                                    tint     = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            GranularityDropdownMenu(
                                expanded    = showCatGranMenu,
                                selectedKey = catGranularity,
                                onDismiss   = { showCatGranMenu = false },
                                onSelect    = { catGranularity = it }
                            )
                        }
                    }
                }

                // Period chip selector
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth().horizontalScroll(catChipScrollState),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    catPeriods.forEachIndexed { i, period ->
                        FilterChip(
                            selected = i == catPeriodIndex,
                            onClick  = {
                                catPeriodIndex = i
                                when (catGranularity) {
                                    "yearly"  -> catContextYear  = period.startDate.year
                                    "monthly" -> {
                                        catContextYear  = period.startDate.year
                                        catContextMonth = period.startDate.monthValue
                                    }
                                    "weekly"  -> {
                                        catContextYear  = period.startDate.year
                                        catContextMonth = period.startDate.monthValue
                                    }
                                }
                            },
                            label = { Text(period.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (catDisplay.isEmpty()) {
                    Text(
                        "No data for this period",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        DonutChart(
                            categories = catDisplay,
                            viewModel  = viewModel,
                            modifier   = Modifier.size(140.dp)
                        )
                        CategoryLegend(
                            categories      = catDisplay,
                            viewModel       = viewModel,
                            onCategoryClick = { if (it != "Other") catDetailCategory = it },
                            modifier        = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ── Income bar chart ──────────────────────────────────────────────────
        Card(
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape    = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        incomeChartTitle,
                        style    = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick  = {
                                incomeChartContextYear  = today.year
                                incomeChartContextMonth = today.monthValue
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Reset",
                                modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box {
                            IconButton(
                                onClick  = { showIncomeGranularityMenu = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Outlined.Tune, contentDescription = "Chart settings",
                                    modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            GranularityDropdownMenu(
                                expanded    = showIncomeGranularityMenu,
                                selectedKey = incomeGranularity,
                                onDismiss   = { showIncomeGranularityMenu = false },
                                onSelect    = { incomeGranularity = it }
                            )
                        }
                    }
                }

                if (incomeGranularity == "monthly" || incomeGranularity == "weekly") {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth().horizontalScroll(incomeChipScrollState),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (incomeGranularity == "monthly") {
                            availableChartYears.forEach { y ->
                                FilterChip(
                                    selected = y == incomeChartContextYear,
                                    onClick  = { incomeChartContextYear = y },
                                    label    = { Text("$y", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        } else {
                            (1..12).forEach { m ->
                                val abbr = YearMonth.of(incomeChartContextYear, m)
                                    .month.getDisplayName(JTextStyle.SHORT, Locale.getDefault())
                                FilterChip(
                                    selected = m == incomeChartContextMonth,
                                    onClick  = { incomeChartContextMonth = m },
                                    label    = { Text(abbr, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                ExpenseBarChart(
                    bars           = incomeChartBars,
                    currencySymbol = currencySymbol,
                    barColor       = Color(0xFF4CAF50),
                    onBarClick     = { bar ->
                        when (incomeGranularity) {
                            "yearly"  -> incomeChartContextYear = bar.startDate.year
                            "monthly" -> {
                                incomeChartContextYear  = bar.startDate.year
                                incomeChartContextMonth = bar.startDate.monthValue
                            }
                        }
                    }
                )
            }
        }

        // ── Income by category ────────────────────────────────────────────────
        Card(
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape    = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        incomeCatTitle,
                        style    = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick  = {
                                incomeCatContextYear  = today.year
                                incomeCatContextMonth = today.monthValue
                                incomeCatPeriodIndex  = defaultCatPeriodIndex(
                                    incomeCatGranularity, incomeCatPeriods, today.year, today.monthValue
                                )
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Reset",
                                modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box {
                            IconButton(
                                onClick  = { showIncomeCatGranMenu = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Outlined.Tune, contentDescription = "Period settings",
                                    modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            GranularityDropdownMenu(
                                expanded    = showIncomeCatGranMenu,
                                selectedKey = incomeCatGranularity,
                                onDismiss   = { showIncomeCatGranMenu = false },
                                onSelect    = { incomeCatGranularity = it }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth().horizontalScroll(incomeCatChipScrollState),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    incomeCatPeriods.forEachIndexed { i, period ->
                        FilterChip(
                            selected = i == incomeCatPeriodIndex,
                            onClick  = {
                                incomeCatPeriodIndex = i
                                when (incomeCatGranularity) {
                                    "yearly"  -> incomeCatContextYear  = period.startDate.year
                                    "monthly" -> {
                                        incomeCatContextYear  = period.startDate.year
                                        incomeCatContextMonth = period.startDate.monthValue
                                    }
                                    "weekly"  -> {
                                        incomeCatContextYear  = period.startDate.year
                                        incomeCatContextMonth = period.startDate.monthValue
                                    }
                                }
                            },
                            label = { Text(period.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (incomeCatDisplay.isEmpty()) {
                    Text(
                        "No income data for this period",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        DonutChart(
                            categories = incomeCatDisplay,
                            viewModel  = viewModel,
                            modifier   = Modifier.size(140.dp)
                        )
                        CategoryLegend(
                            categories      = incomeCatDisplay,
                            viewModel       = viewModel,
                            onCategoryClick = { if (it != "Other") incomeCatDetailCategory = it },
                            modifier        = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }

    // ── Category detail bottom sheet ──────────────────────────────────────────
    catDetailCategory?.let { category ->
        ModalBottomSheet(
            onDismissRequest = { catDetailCategory = null },
            sheetState       = catDetailSheetState
        ) {
            CategoryDetailSheet(
                category        = category,
                period          = selectedCatPeriod,
                allTransactions = allTransactions,
                viewModel       = viewModel,
                currencySymbol  = currencySymbol
            )
        }
    }

    incomeCatDetailCategory?.let { category ->
        ModalBottomSheet(
            onDismissRequest = { incomeCatDetailCategory = null },
            sheetState       = incomeCatDetailSheetState
        ) {
            CategoryDetailSheet(
                category        = category,
                period          = selectedIncomeCatPeriod,
                allTransactions = allTransactions,
                viewModel       = viewModel,
                currencySymbol  = currencySymbol
            )
        }
    }
}

// ── Shared granularity dropdown ───────────────────────────────────────────────

@Composable
private fun GranularityDropdownMenu(
    expanded: Boolean,
    selectedKey: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
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
                onClick = { onSelect(key); onDismiss() },
                trailingIcon = {
                    if (selectedKey == key) Icon(
                        Icons.Outlined.Check, null,
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

// ── Bar chart ─────────────────────────────────────────────────────────────────

@Composable
private fun ExpenseBarChart(
    bars: List<ChartBar>,
    currencySymbol: String,
    onBarClick: (ChartBar) -> Unit,
    barColor: Color = Color.Unspecified
) {
    val resolvedBarColor = if (barColor == Color.Unspecified) MaterialTheme.colorScheme.primary else barColor
    val gridColor    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val labelColor   = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val density      = LocalDensity.current
    val labelStyle   = TextStyle(fontSize = 10.sp, color = labelColor)

    val maxVal   = bars.maxOfOrNull { it.value }?.coerceAtLeast(1.0) ?: 1.0
    val animProg by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessVeryLow),
        label         = "bar"
    )

    fun fmtLabel(v: Double): String = when {
        v >= 1_000_000 -> "${"%.1f".format(v / 1_000_000)}M"
        v >= 1_000     -> "${"%.1f".format(v / 1_000)}k"
        else           -> "%.0f".format(v)
    }

    val yLabelWidthPx = remember(maxVal, textMeasurer) {
        listOf(maxVal, maxVal * 0.66, maxVal * 0.33)
            .maxOf { textMeasurer.measure(fmtLabel(it), labelStyle).size.width }
            .toFloat()
    }
    val xLabelHeightPx = remember(textMeasurer) {
        textMeasurer.measure("Jan", labelStyle).size.height.toFloat()
    }
    val leftMarginDp = with(density) { (yLabelWidthPx + 18f).toDp() }
    val bottomPadDp  = with(density) { (xLabelHeightPx + 6.dp.toPx()).toDp() }

    Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            val lm     = yLabelWidthPx + 18f
            val chartW = size.width - lm
            val chartH = size.height - xLabelHeightPx - 6.dp.toPx()

            listOf(1.0, 0.66, 0.33).forEach { frac ->
                val y    = chartH * (1f - frac.toFloat())
                val meas = textMeasurer.measure(fmtLabel(maxVal * frac), labelStyle)
                drawLine(gridColor, Offset(lm, y), Offset(size.width, y), strokeWidth = 0.5.dp.toPx())
                drawText(meas, topLeft = Offset(lm - meas.size.width - 4.dp.toPx(), y - meas.size.height / 2f))
            }
            drawLine(gridColor, Offset(lm, chartH), Offset(size.width, chartH), strokeWidth = 0.5.dp.toPx())

            val groupW = chartW / bars.size.coerceAtLeast(1)
            val barW   = groupW * 0.5f
            bars.forEachIndexed { i, bar ->
                val cx = lm + groupW * i + groupW / 2f
                val bh = (bar.value / maxVal * chartH * animProg).toFloat().coerceAtLeast(2f)
                drawRoundRect(
                    color        = resolvedBarColor,
                    topLeft      = Offset(cx - barW / 2f, chartH - bh),
                    size         = Size(barW, bh),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
                val meas = textMeasurer.measure(bar.label, labelStyle)
                drawText(meas, topLeft = Offset(cx - meas.size.width / 2f, chartH + 6.dp.toPx()))
            }
        }

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

// ── Donut + legend ────────────────────────────────────────────────────────────

private val chartPalette = listOf(
    Color(0xFF4361EE), // vivid blue
    Color(0xFFF72585), // hot pink
    Color(0xFF4CC9F0), // sky blue
    Color(0xFF06D6A0), // emerald
    Color(0xFFFFB703), // golden amber
    Color(0xFFE63946), // coral red
    Color(0xFF8338EC), // violet
    Color(0xFFFB8500), // orange
    Color(0xFF118AB2), // teal blue
    Color(0xFFBDE0FE), // pale blue (unlikely to be reached)
)
private val otherColor = Color(0xFFB0BEC5) // neutral blue-grey for "Other"

private fun chartColor(index: Int, categoryName: String): Color =
    if (categoryName == "Other") otherColor else chartPalette.getOrElse(index) { otherColor }

@Composable
private fun DonutChart(
    categories: List<Pair<String, Double>>,
    viewModel: TransactionViewModel,
    modifier: Modifier = Modifier
) {
    val total    = categories.sumOf { it.second }.coerceAtLeast(0.01)
    val colors   = categories.mapIndexed { i, (cat, _) -> chartColor(i, cat) }
    val animProg by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessVeryLow),
        label         = "donut"
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
    onCategoryClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val total = categories.sumOf { it.second }.coerceAtLeast(0.01)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        categories.forEachIndexed { i, (cat, amount) ->
            val dotColor  = chartColor(i, cat)
            val isOther   = cat == "Other"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .then(if (!isOther) Modifier.clickable { onCategoryClick(cat) } else Modifier)
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(dotColor)
                )
                Text(
                    cat,
                    style  = MaterialTheme.typography.labelSmall,
                    color  = if (isOther) MaterialTheme.colorScheme.onSurfaceVariant
                             else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Text(
                    "${"%.0f".format(amount / total * 100)}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isOther) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ── Category detail bottom sheet ──────────────────────────────────────────────

@Composable
private fun CategoryDetailSheet(
    category: String,
    period: CatPeriod?,
    allTransactions: List<Transaction>,
    viewModel: TransactionViewModel,
    currencySymbol: String
) {
    val catColor = viewModel.categoryFor(category)?.color ?: Color(0xFF9E9E9E)
    val txns = remember(allTransactions, category, period) {
        val inRange = if (period == null) allTransactions
                      else filterInDateRange(allTransactions, period.startDate, period.endDate)
        inRange.filter { it.category == category }.sortedByDescending { it.date }
    }
    val total = txns.sumOf { it.amount }
    val dateFmt = DateTimeFormatter.ofPattern("d MMM")

    Column(modifier = Modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(catColor)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(category, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                if (period != null) {
                    Text(
                        period.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                "$currencySymbol${"%.2f".format(total)}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = catColor
            )
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))

        if (txns.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No transactions in this period", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(txns) { txn ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            txn.date.format(dateFmt),
                            style    = MaterialTheme.typography.labelMedium,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(48.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(txn.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            if (txn.note.isNotBlank()) {
                                Text(
                                    txn.note,
                                    style   = MaterialTheme.typography.labelSmall,
                                    color   = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                        Text(
                            "$currencySymbol${"%.2f".format(txn.amount)}",
                            style    = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color    = if (txn.type == TransactionType.EXPENSE)
                                           MaterialTheme.colorScheme.error
                                       else
                                           MaterialTheme.colorScheme.primary
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
