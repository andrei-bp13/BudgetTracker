package com.pardonmyspending.app.ui.screens

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
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import androidx.compose.foundation.shape.CircleShape
import com.pardonmyspending.app.data.model.Transaction
import com.pardonmyspending.app.data.model.TransactionType
import com.pardonmyspending.app.ui.components.PaymentTypeIcon
import com.pardonmyspending.app.ui.components.displayName
import com.pardonmyspending.app.ui.icons.iconById
import com.pardonmyspending.app.ui.theme.LocalCurrencySymbol
import com.pardonmyspending.app.util.CatPeriod
import com.pardonmyspending.app.util.catPeriods
import com.pardonmyspending.app.util.computeCategoryTotals
import com.pardonmyspending.app.util.computeMonthlyBars
import com.pardonmyspending.app.util.computeWeeklyBars
import com.pardonmyspending.app.util.computeYearlyBars
import com.pardonmyspending.app.util.defaultCatPeriodIndex
import com.pardonmyspending.app.util.filterByTypeAndDateRange
import com.pardonmyspending.app.util.filterExpensesByDateRange
import com.pardonmyspending.app.util.filterInDateRange
import com.pardonmyspending.app.viewmodel.ChartBar
import com.pardonmyspending.app.viewmodel.TransactionViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

// â"€â"€ Granularity options â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

private val granularityOptions = listOf(
    Triple("weekly",  "Weekly",  "Selected month in 7-day groups"),
    Triple("monthly", "Monthly", "All months of selected year"),
    Triple("yearly",  "Yearly",  "All years"),
)

// â"€â"€ Screen â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: TransactionViewModel,
    onBarClick: (ChartBar) -> Unit = {},
    onIncomeBarClick: (ChartBar) -> Unit = {},
    editMode: Boolean = false
) {
    val granularity     by viewModel.chartGranularity.collectAsState()
    val allTransactions by viewModel.transactions.collectAsState()
    val currencySymbol  = LocalCurrencySymbol.current
    val today           = LocalDate.now()

    // â"€â"€ Expenses chart context â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
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
            "Expenses - $abbr $chartContextYear by Week"
        }
        "yearly"  -> "Expenses - By Year"
        else      -> "Expenses - $chartContextYear"
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

    // â"€â"€ Category card context â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
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

    // All categories sorted -" used for the detail sheet
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

    val catTitle = selectedCatPeriod?.let { "Spending by Category - ${it.title}" }
        ?: "Spending by Category"

    // â"€â"€ Income chart context â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
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
            "Income - $abbr $incomeChartContextYear by Week"
        }
        "yearly"  -> "Income - By Year"
        else      -> "Income - $incomeChartContextYear"
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

    // â"€â"€ Income category context â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
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

    val incomeCatTitle = selectedIncomeCatPeriod?.let { "Income by Category - ${it.title}" }
        ?: "Income by Category"

    // â"€â"€ Highlight & total summaries â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
    var expenseHighlightFilter by remember { mutableStateOf("all") }
    var incomeHighlightFilter  by remember { mutableStateOf("all") }
    var totalSpentFilter       by remember { mutableStateOf("all") }
    var totalEarnedFilter      by remember { mutableStateOf("all") }

    val weekStart = remember(today) { today.with(java.time.DayOfWeek.MONDAY) }
    val weekEnd   = remember(today) { weekStart.plusDays(6) }

    fun List<Transaction>.applyHighlightFilter(filter: String): List<Transaction> = when (filter) {
        "year"  -> filter { it.date.year == today.year }
        "month" -> filter { YearMonth.from(it.date) == YearMonth.now() }
        else    -> this
    }

    fun List<Transaction>.applyTotalFilter(filter: String): List<Transaction> = when (filter) {
        "year"  -> filter { it.date.year == today.year }
        "month" -> filter { YearMonth.from(it.date) == YearMonth.now() }
        "week"  -> filter { !it.date.isBefore(weekStart) && !it.date.isAfter(weekEnd) }
        else    -> this
    }

    val highestExpense = remember(allTransactions, expenseHighlightFilter) {
        allTransactions.filter { it.type == TransactionType.EXPENSE }
            .applyHighlightFilter(expenseHighlightFilter)
            .maxByOrNull { it.amount }
    }
    val highestIncome = remember(allTransactions, incomeHighlightFilter) {
        allTransactions.filter { it.type == TransactionType.INCOME }
            .applyHighlightFilter(incomeHighlightFilter)
            .maxByOrNull { it.amount }
    }
    val totalSpent = remember(allTransactions, totalSpentFilter) {
        allTransactions.filter { it.type == TransactionType.EXPENSE }
            .applyTotalFilter(totalSpentFilter)
            .sumOf { it.amount }
    }
    val totalEarned = remember(allTransactions, totalEarnedFilter) {
        allTransactions.filter { it.type == TransactionType.INCOME }
            .applyTotalFilter(totalEarnedFilter)
            .sumOf { it.amount }
    }

    // â"€â"€ Total savings â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
    var totalSavingsFilter by remember { mutableStateOf("all") }
    val savingsCategories = remember { setOf("Savings", "Investment") }

    val savingsTransactions = remember(allTransactions, totalSavingsFilter) {
        allTransactions
            .filter { it.category in savingsCategories }
            .applyHighlightFilter(totalSavingsFilter)
            .sortedByDescending { it.date.atTime(it.time) }
    }
    val totalSavings     = remember(savingsTransactions) { savingsTransactions.sumOf { it.amount } }
    val latestSavingsTxn = remember(savingsTransactions) { savingsTransactions.firstOrNull() }

    // â"€â"€ Dashboard card state (for "Add to dashboard" feature) â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
    val dashCardOrderJson   by viewModel.cardOrderJson.collectAsState()
    val dashCardVisibleJson by viewModel.cardVisibleJson.collectAsState()

    val dashboardCardIds = remember(dashCardOrderJson) {
        if (dashCardOrderJson.isBlank()) TransactionViewModel.DEFAULT_CARD_ORDER.toSet()
        else buildSet { val arr = JSONArray(dashCardOrderJson); repeat(arr.length()) { add(arr.getString(it)) } }
    }

    fun addCardToDashboard(cardId: String) {
        val currentOrder = if (dashCardOrderJson.isBlank()) TransactionViewModel.DEFAULT_CARD_ORDER
            else JSONArray(dashCardOrderJson).let { arr -> List(arr.length()) { arr.getString(it) } }
        val currentVisible = if (dashCardVisibleJson.isBlank()) TransactionViewModel.DEFAULT_CARD_VISIBLE
            else JSONObject(dashCardVisibleJson).let { obj -> obj.keys().asSequence().associateWith { obj.getBoolean(it) } }
        if (cardId !in currentOrder) {
            viewModel.setCardOrder(currentOrder + cardId)
            viewModel.setCardVisible(currentVisible + (cardId to true))
        }
    }

    // â"€â"€ Card order & visibility â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
    val cardOrder   = remember { mutableStateListOf(
        "expense_chart", "spending_category", "income_chart", "income_category",
        "highest_expense", "highest_income", "total_spent", "total_earned", "total_savings"
    ) }
    val cardVisible = remember { mutableStateMapOf(
        "expense_chart"     to true,
        "spending_category" to true,
        "income_chart"      to true,
        "income_category"   to true,
        "highest_expense"   to false,
        "highest_income"    to false,
        "total_spent"       to false,
        "total_earned"      to false,
        "total_savings"     to false
    ) }
    fun swapCards(a: String, b: String) {
        val i = cardOrder.indexOf(a); val j = cardOrder.indexOf(b)
        if (i >= 0 && j >= 0) { cardOrder[i] = b; cardOrder[j] = a }
    }
    val visibleCards = cardOrder.filter { cardVisible[it] == true }
    val hiddenCards  = cardOrder.filter { cardVisible[it] != true }

    // â"€â"€ Drag-to-reorder state â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
    var draggedCardId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY   by remember { mutableStateOf(0f) }
    val cardHeights   = remember { mutableStateMapOf<String, Int>() }

    val handleDrag: (Float) -> Unit = handleDrag@{ delta ->
        dragOffsetY += delta
        val dragged = draggedCardId ?: return@handleDrag
        val currentVisible = cardOrder.filter { cardVisible[it] == true }
        val idx = currentVisible.indexOf(dragged)
        if (dragOffsetY > 0f && idx < currentVisible.lastIndex) {
            val nextId    = currentVisible[idx + 1]
            val threshold = (cardHeights[nextId] ?: 0) / 2f
            if (dragOffsetY > threshold) {
                swapCards(dragged, nextId)
                dragOffsetY -= (cardHeights[nextId] ?: 0).toFloat()
            }
        } else if (dragOffsetY < 0f && idx > 0) {
            val prevId    = currentVisible[idx - 1]
            val threshold = (cardHeights[prevId] ?: 0) / 2f
            if (-dragOffsetY > threshold) {
                swapCards(dragged, prevId)
                dragOffsetY += (cardHeights[prevId] ?: 0).toFloat()
            }
        }
    }

    // â"€â"€ Card render dispatch â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
    val renderCard: @Composable (String) -> Unit = { cardId ->
        when (cardId) {
            "highest_expense" -> HighlightSummaryCard(
                label          = "Highest Expense",
                transaction    = highestExpense,
                amountColor    = MaterialTheme.colorScheme.error,
                amountPrefix   = "-",
                currencySymbol = currencySymbol,
                viewModel      = viewModel,
                selectedFilter = expenseHighlightFilter,
                onFilterChange = { expenseHighlightFilter = it },
                modifier       = Modifier.fillMaxWidth()
            )
            "highest_income" -> HighlightSummaryCard(
                label          = "Highest Income",
                transaction    = highestIncome,
                amountColor    = Color(0xFF4CAF50),
                amountPrefix   = "+",
                currencySymbol = currencySymbol,
                viewModel      = viewModel,
                selectedFilter = incomeHighlightFilter,
                onFilterChange = { incomeHighlightFilter = it },
                modifier       = Modifier.fillMaxWidth()
            )
            "total_spent" -> TotalSummaryCard(
                label          = "Total Spent",
                total          = totalSpent,
                amountColor    = MaterialTheme.colorScheme.error,
                amountPrefix   = "-",
                currencySymbol = currencySymbol,
                selectedFilter = totalSpentFilter,
                onFilterChange = { totalSpentFilter = it },
                modifier       = Modifier.fillMaxWidth()
            )
            "total_earned" -> TotalSummaryCard(
                label          = "Total Earned",
                total          = totalEarned,
                amountColor    = Color(0xFF4CAF50),
                amountPrefix   = "+",
                currencySymbol = currencySymbol,
                selectedFilter = totalEarnedFilter,
                onFilterChange = { totalEarnedFilter = it },
                modifier       = Modifier.fillMaxWidth()
            )
            "total_savings" -> SavingsSummaryCard(
                total          = totalSavings,
                latestTxn      = latestSavingsTxn,
                currencySymbol = currencySymbol,
                viewModel      = viewModel,
                selectedFilter = totalSavingsFilter,
                onFilterChange = { totalSavingsFilter = it },
                modifier       = Modifier.fillMaxWidth()
            )
            "expense_chart" -> Card(
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
                        Text(chartTitle, style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp), modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { chartContextYear = today.year; chartContextMonth = today.monthValue }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Outlined.Refresh, contentDescription = "Reset to current period", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Box {
                                IconButton(onClick = { showGranularityMenu = true }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Outlined.Tune, contentDescription = "Chart settings", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                GranularityDropdownMenu(expanded = showGranularityMenu, selectedKey = granularity, onDismiss = { showGranularityMenu = false }, onSelect = { viewModel.setChartGranularity(it) })
                            }
                        }
                    }
                    if (granularity == "monthly" || granularity == "weekly") {
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(chartChipScrollState), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (granularity == "monthly") {
                                availableChartYears.forEach { y -> FilterChip(selected = y == chartContextYear, onClick = { chartContextYear = y }, label = { Text("$y", style = MaterialTheme.typography.labelSmall) }) }
                            } else {
                                (1..12).forEach { m ->
                                    val abbr = YearMonth.of(chartContextYear, m).month.getDisplayName(JTextStyle.SHORT, Locale.getDefault())
                                    FilterChip(selected = m == chartContextMonth, onClick = { chartContextMonth = m }, label = { Text(abbr, style = MaterialTheme.typography.labelSmall) })
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    ExpenseBarChart(
                        bars           = chartBars,
                        currencySymbol = currencySymbol,
                        onBarClick     = { bar ->
                            when (granularity) {
                                "yearly"  -> chartContextYear = bar.startDate.year
                                "monthly" -> { chartContextYear = bar.startDate.year; chartContextMonth = bar.startDate.monthValue }
                            }
                            onBarClick(bar)
                        }
                    )
                }
            }
            "spending_category" -> Card(
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape    = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(catTitle, style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp), modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { catContextYear = today.year; catContextMonth = today.monthValue; catPeriodIndex = defaultCatPeriodIndex(catGranularity, catPeriods, today.year, today.monthValue) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Outlined.Refresh, contentDescription = "Reset to current period", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Box {
                                IconButton(onClick = { showCatGranMenu = true }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Outlined.Tune, contentDescription = "Period settings", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                GranularityDropdownMenu(expanded = showCatGranMenu, selectedKey = catGranularity, onDismiss = { showCatGranMenu = false }, onSelect = { catGranularity = it })
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(catChipScrollState), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        catPeriods.forEachIndexed { i, period ->
                            FilterChip(
                                selected = i == catPeriodIndex,
                                onClick  = {
                                    catPeriodIndex = i
                                    when (catGranularity) {
                                        "yearly"  -> catContextYear = period.startDate.year
                                        "monthly", "weekly" -> { catContextYear = period.startDate.year; catContextMonth = period.startDate.monthValue }
                                    }
                                },
                                label = { Text(period.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (catDisplay.isEmpty()) {
                        Text("No data for this period", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            DonutChart(categories = catDisplay, viewModel = viewModel, modifier = Modifier.size(140.dp))
                            CategoryLegend(categories = catDisplay, viewModel = viewModel, onCategoryClick = { if (it != "Other") catDetailCategory = it }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            "income_chart" -> Card(
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape    = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(incomeChartTitle, style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp), modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { incomeChartContextYear = today.year; incomeChartContextMonth = today.monthValue }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Outlined.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Box {
                                IconButton(onClick = { showIncomeGranularityMenu = true }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Outlined.Tune, contentDescription = "Chart settings", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                GranularityDropdownMenu(expanded = showIncomeGranularityMenu, selectedKey = incomeGranularity, onDismiss = { showIncomeGranularityMenu = false }, onSelect = { incomeGranularity = it })
                            }
                        }
                    }
                    if (incomeGranularity == "monthly" || incomeGranularity == "weekly") {
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(incomeChipScrollState), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (incomeGranularity == "monthly") {
                                availableChartYears.forEach { y -> FilterChip(selected = y == incomeChartContextYear, onClick = { incomeChartContextYear = y }, label = { Text("$y", style = MaterialTheme.typography.labelSmall) }) }
                            } else {
                                (1..12).forEach { m ->
                                    val abbr = YearMonth.of(incomeChartContextYear, m).month.getDisplayName(JTextStyle.SHORT, Locale.getDefault())
                                    FilterChip(selected = m == incomeChartContextMonth, onClick = { incomeChartContextMonth = m }, label = { Text(abbr, style = MaterialTheme.typography.labelSmall) })
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
                                "monthly" -> { incomeChartContextYear = bar.startDate.year; incomeChartContextMonth = bar.startDate.monthValue }
                            }
                            onIncomeBarClick(bar)
                        }
                    )
                }
            }
            "income_category" -> Card(
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape    = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(incomeCatTitle, style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp), modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { incomeCatContextYear = today.year; incomeCatContextMonth = today.monthValue; incomeCatPeriodIndex = defaultCatPeriodIndex(incomeCatGranularity, incomeCatPeriods, today.year, today.monthValue) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Outlined.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Box {
                                IconButton(onClick = { showIncomeCatGranMenu = true }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Outlined.Tune, contentDescription = "Period settings", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                GranularityDropdownMenu(expanded = showIncomeCatGranMenu, selectedKey = incomeCatGranularity, onDismiss = { showIncomeCatGranMenu = false }, onSelect = { incomeCatGranularity = it })
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(incomeCatChipScrollState), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        incomeCatPeriods.forEachIndexed { i, period ->
                            FilterChip(
                                selected = i == incomeCatPeriodIndex,
                                onClick  = {
                                    incomeCatPeriodIndex = i
                                    when (incomeCatGranularity) {
                                        "yearly"  -> incomeCatContextYear = period.startDate.year
                                        "monthly", "weekly" -> { incomeCatContextYear = period.startDate.year; incomeCatContextMonth = period.startDate.monthValue }
                                    }
                                },
                                label = { Text(period.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (incomeCatDisplay.isEmpty()) {
                        Text("No income data for this period", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            DonutChart(categories = incomeCatDisplay, viewModel = viewModel, modifier = Modifier.size(140.dp))
                            CategoryLegend(categories = incomeCatDisplay, viewModel = viewModel, onCategoryClick = { if (it != "Other") incomeCatDetailCategory = it }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (editMode) {
            Text(
                "Hold and drag to reorder",
                style    = MaterialTheme.typography.labelMedium,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        visibleCards.forEach { cardId ->
            key(cardId) {
                EditableCardWrapper(
                    editMode         = editMode,
                    visible          = true,
                    isDragged        = draggedCardId == cardId,
                    dragOffsetY      = if (draggedCardId == cardId) dragOffsetY else 0f,
                    onDragStart      = { draggedCardId = cardId; dragOffsetY = 0f },
                    onDrag           = handleDrag,
                    onDragEnd        = { draggedCardId = null; dragOffsetY = 0f },
                    onToggleVisible  = { cardVisible[cardId] = false },
                    onHeightChanged  = { cardHeights[cardId] = it },
                    isOnDashboard    = cardId in dashboardCardIds,
                    onAddToDashboard = { addCardToDashboard(cardId) }
                ) { renderCard(cardId) }
            }
        }

        if (editMode && hiddenCards.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier              = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Text("Hidden", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
            hiddenCards.forEach { cardId ->
                key(cardId) {
                    EditableCardWrapper(
                        editMode         = true,
                        visible          = false,
                        isDragged        = false,
                        dragOffsetY      = 0f,
                        onDragStart      = {},
                        onDrag           = {},
                        onDragEnd        = {},
                        onToggleVisible  = { cardVisible[cardId] = true },
                        onHeightChanged  = {},
                        isOnDashboard    = cardId in dashboardCardIds,
                        onAddToDashboard = { addCardToDashboard(cardId) }
                    ) { renderCard(cardId) }
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }

    // â"€â"€ Category detail bottom sheet â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
    catDetailCategory?.let { category ->
        ModalBottomSheet(
            onDismissRequest = { catDetailCategory = null },
            sheetState       = catDetailSheetState,
            modifier         = Modifier.fillMaxHeight(0.85f)
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
            sheetState       = incomeCatDetailSheetState,
            modifier         = Modifier.fillMaxHeight(0.85f)
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

// â"€â"€ Shared granularity dropdown â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

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

// â"€â"€ Bar chart â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

@Composable
fun ExpenseBarChart(
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

// â"€â"€ Donut + legend â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

val chartPalette = listOf(
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
val otherColor = Color(0xFFB0BEC5) // neutral blue-grey for "Other"

fun chartColor(index: Int, categoryName: String): Color =
    if (categoryName == "Other") otherColor else chartPalette.getOrElse(index) { otherColor }

@Composable
fun DonutChart(
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
fun CategoryLegend(
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

// â"€â"€ Category detail bottom sheet â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

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

// â"€â"€ Highlight summary card â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

val highlightDateFmt = DateTimeFormatter.ofPattern("MMM d")
val highlightTimeFmt = DateTimeFormatter.ofPattern("HH:mm")

val highlightFilterOptions = listOf(
    "all"   to "All Time",
    "year"  to "This Year",
    "month" to "This Month"
)

fun filterDisplayLabel(key: String): String = when (key) {
    "year"  -> "This Year"
    "month" -> "This Month"
    "week"  -> "This Week"
    else    -> "All Time"
}

@Composable
fun HighlightSummaryCard(
    label: String,
    transaction: Transaction?,
    amountColor: Color,
    amountPrefix: String,
    currencySymbol: String,
    viewModel: TransactionViewModel,
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    var showFilterMenu by remember { mutableStateOf(false) }
    Card(
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape    = MaterialTheme.shapes.extraLarge,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = secondary
                    )
                    Text("·", style = MaterialTheme.typography.labelMedium, color = secondary.copy(alpha = 0.5f))
                    Text(
                        filterDisplayLabel(selectedFilter),
                        style = MaterialTheme.typography.labelMedium,
                        color = secondary.copy(alpha = 0.5f)
                    )
                }
                Box {
                    IconButton(
                        onClick  = { showFilterMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Tune,
                            contentDescription = "Filter",
                            modifier = Modifier.size(16.dp),
                            tint     = secondary
                        )
                    }
                    DropdownMenu(
                        expanded        = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        highlightFilterOptions.forEach { (key, displayLabel) ->
                            DropdownMenuItem(
                                text = { Text(displayLabel, style = MaterialTheme.typography.bodyMedium) },
                                onClick = { onFilterChange(key); showFilterMenu = false },
                                trailingIcon = {
                                    if (selectedFilter == key) Icon(
                                        Icons.Outlined.Check, null,
                                        tint     = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                if (transaction != null) "$amountPrefix$currencySymbol ${"%.2f".format(transaction.amount)}" else "-",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = if (transaction != null) amountColor else secondary
            )
            if (transaction != null) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(
                    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    thickness = 0.5.dp
                )
                Spacer(Modifier.height(10.dp))
                val cat     = viewModel.categoryFor(transaction.category)
                val iconId  = cat?.iconId ?: "more_horiz"
                val color   = cat?.color ?: MaterialTheme.colorScheme.primary
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = iconById(iconId),
                            contentDescription = null,
                            tint               = color,
                            modifier           = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            transaction.title,
                            style    = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            transaction.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = secondary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${transaction.date.format(highlightDateFmt)} · ${transaction.time.format(highlightTimeFmt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = secondary
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            PaymentTypeIcon(
                                type = transaction.paymentType,
                                tint = secondary,
                                size = 12.dp
                            )
                            Text(
                                transaction.paymentType.displayName(),
                                style = MaterialTheme.typography.labelSmall,
                                color = secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

// â"€â"€ Total summary card â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

val totalFilterOptions = listOf(
    "all"   to "All Time",
    "year"  to "This Year",
    "month" to "This Month",
    "week"  to "This Week"
)

@Composable
fun TotalSummaryCard(
    label: String,
    total: Double,
    amountColor: Color,
    amountPrefix: String,
    currencySymbol: String,
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    var showFilterMenu by remember { mutableStateOf(false) }
    Card(
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape    = MaterialTheme.shapes.extraLarge,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = secondary
                    )
                    Text("·", style = MaterialTheme.typography.labelMedium, color = secondary.copy(alpha = 0.5f))
                    Text(
                        filterDisplayLabel(selectedFilter),
                        style = MaterialTheme.typography.labelMedium,
                        color = secondary.copy(alpha = 0.5f)
                    )
                }
                Box {
                    IconButton(
                        onClick  = { showFilterMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Tune,
                            contentDescription = "Filter",
                            modifier = Modifier.size(16.dp),
                            tint     = secondary
                        )
                    }
                    DropdownMenu(
                        expanded         = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        totalFilterOptions.forEach { (key, displayLabel) ->
                            DropdownMenuItem(
                                text = { Text(displayLabel, style = MaterialTheme.typography.bodyMedium) },
                                onClick = { onFilterChange(key); showFilterMenu = false },
                                trailingIcon = {
                                    if (selectedFilter == key) Icon(
                                        Icons.Outlined.Check, null,
                                        tint     = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                "$amountPrefix$currencySymbol ${"%.2f".format(total)}",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = amountColor
            )
        }
    }
}

// â"€â"€ Savings summary card â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

// â"€â"€ Editable card wrapper â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

@Composable
private fun EditableCardWrapper(
    editMode: Boolean,
    visible: Boolean,
    isDragged: Boolean,
    dragOffsetY: Float,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onToggleVisible: () -> Unit,
    onHeightChanged: (Int) -> Unit,
    isOnDashboard: Boolean = true,
    onAddToDashboard: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val latestOnDragStart by rememberUpdatedState(onDragStart)
    val latestOnDrag      by rememberUpdatedState(onDrag)
    val latestOnDragEnd   by rememberUpdatedState(onDragEnd)

    val scale = remember { Animatable(1f) }
    LaunchedEffect(isDragged) {
        if (isDragged) scale.animateTo(1.04f, spring(dampingRatio = 0.4f, stiffness = 500f))
        else           scale.animateTo(1f,    spring(dampingRatio = 0.6f, stiffness = 500f))
    }

    val dragMod = if (editMode && visible) {
        Modifier.pointerInput(editMode) {
            detectDragGesturesAfterLongPress(
                onDragStart  = { latestOnDragStart() },
                onDrag       = { change, delta -> change.consume(); latestOnDrag(delta.y) },
                onDragEnd    = { latestOnDragEnd() },
                onDragCancel = { latestOnDragEnd() }
            )
        }
    } else Modifier

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale.value)
            .zIndex(if (isDragged) 1f else 0f)
            .onGloballyPositioned { onHeightChanged(it.size.height) }
            .then(dragMod)
            .offset { IntOffset(0, if (isDragged) dragOffsetY.roundToInt() else 0) }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = if (editMode && !visible) Modifier.fillMaxWidth().alpha(0.35f) else Modifier.fillMaxWidth()) {
                content()
            }
            if (editMode) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(editMode) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                val up = waitForUpOrCancellation()
                                up?.consume()
                            }
                        }
                )
            }
        }
        if (editMode) {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(end = 28.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                val dashTint = if (!isOnDashboard) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f))
                        .then(if (!isOnDashboard) Modifier.clickable(onClick = onAddToDashboard) else Modifier)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.Dashboard,
                        contentDescription = "Add to dashboard",
                        tint               = dashTint,
                        modifier           = Modifier.size(13.dp)
                    )
                    Text(
                        text  = "add to dashboard",
                        style = MaterialTheme.typography.labelSmall,
                        color = dashTint
                    )
                }
                Spacer(Modifier.width(6.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f))
                        .clickable(onClick = onToggleVisible)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector        = if (visible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = if (visible) "Hide card" else "Show card",
                        tint               = if (visible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier           = Modifier.size(13.dp)
                    )
                    Text(
                        text  = if (visible) "visible" else "hidden",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (visible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// â"€â"€ Savings filter options â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

val savingsFilterOptions = listOf(
    "all"   to "All Time",
    "year"  to "This Year",
    "month" to "This Month"
)

@Composable
fun SavingsSummaryCard(
    total: Double,
    latestTxn: Transaction?,
    currencySymbol: String,
    viewModel: TransactionViewModel,
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val secondary      = MaterialTheme.colorScheme.onSurfaceVariant
    val savingsColor   = Color(0xFF1B5E20)
    var showFilterMenu by remember { mutableStateOf(false) }

    Card(
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape    = MaterialTheme.shapes.extraLarge,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Total Savings",
                        style = MaterialTheme.typography.labelMedium,
                        color = secondary
                    )
                    Text("·", style = MaterialTheme.typography.labelMedium, color = secondary.copy(alpha = 0.5f))
                    Text(
                        filterDisplayLabel(selectedFilter),
                        style = MaterialTheme.typography.labelMedium,
                        color = secondary.copy(alpha = 0.5f)
                    )
                }
                Box {
                    IconButton(
                        onClick  = { showFilterMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Tune,
                            contentDescription = "Filter",
                            modifier = Modifier.size(16.dp),
                            tint     = secondary
                        )
                    }
                    DropdownMenu(
                        expanded         = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        savingsFilterOptions.forEach { (key, displayLabel) ->
                            DropdownMenuItem(
                                text = { Text(displayLabel, style = MaterialTheme.typography.bodyMedium) },
                                onClick = { onFilterChange(key); showFilterMenu = false },
                                trailingIcon = {
                                    if (selectedFilter == key) Icon(
                                        Icons.Outlined.Check, null,
                                        tint     = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                if (total > 0) "+$currencySymbol ${"%.2f".format(total)}" else "$currencySymbol 0.00",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = if (total > 0) savingsColor else secondary
            )
            if (latestTxn != null) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(
                    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    thickness = 0.5.dp
                )
                Spacer(Modifier.height(10.dp))
                val cat    = viewModel.categoryFor(latestTxn.category)
                val iconId = cat?.iconId ?: "account_balance"
                val color  = cat?.color ?: savingsColor
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = iconById(iconId),
                            contentDescription = null,
                            tint               = color,
                            modifier           = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            latestTxn.title,
                            style    = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            latestTxn.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = secondary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${latestTxn.date.format(highlightDateFmt)} · ${latestTxn.time.format(highlightTimeFmt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = secondary
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            PaymentTypeIcon(type = latestTxn.paymentType, tint = secondary, size = 12.dp)
                            Text(
                                latestTxn.paymentType.displayName(),
                                style = MaterialTheme.typography.labelSmall,
                                color = secondary
                            )
                        }
                    }
                }
            }
        }
    }
}
