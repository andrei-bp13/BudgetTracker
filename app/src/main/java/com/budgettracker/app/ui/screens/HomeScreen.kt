package com.pardonmyspending.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.pardonmyspending.app.data.model.PaymentType
import com.pardonmyspending.app.data.model.Transaction
import com.pardonmyspending.app.data.model.TransactionType
import com.pardonmyspending.app.util.computeCategoryTotals
import com.pardonmyspending.app.util.computeMonthlyBars
import java.time.YearMonth
import org.json.JSONArray
import org.json.JSONObject
import com.pardonmyspending.app.ui.components.*
import com.pardonmyspending.app.ui.theme.LocalCurrencySymbol
import com.pardonmyspending.app.viewmodel.TransactionViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TransactionViewModel,
    onAddTransaction: () -> Unit,
    onCategoryClick: (String) -> Unit = {},
    onPaymentTypeClick: (PaymentType) -> Unit = {},
    onEditTransaction: (Transaction) -> Unit = {},
    onDeleteTransaction: (Transaction) -> Unit = {},
    editMode: Boolean = false,
    onMenuClick: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    accountInitial: Char = 'A',
    isSignedIn: Boolean = false,
    onToggleEditMode: () -> Unit = {}
) {
    val thisMonth      by viewModel.thisMonthDailyExpenses.collectAsState()
    val lastMonth      by viewModel.lastMonthDailyExpenses.collectAsState()
    val recent         by viewModel.recentTransactions.collectAsState()
    val topCats        by viewModel.topCategories.collectAsState()
    val expense             by viewModel.thisMonthExpense.collectAsState()
    val income              by viewModel.thisMonthIncome.collectAsState()
    val allTransactions          by viewModel.transactions.collectAsState()
    val expenseChartBars         by viewModel.expenseChartBars.collectAsState()
    val categoryTotalsThisMonth  by viewModel.categoryTotalsThisMonth.collectAsState()
    val globalColoring      by viewModel.globalColoring.collectAsState()
    val byPaymentType       by viewModel.thisMonthByPaymentType.collectAsState()
    val byPaymentTypeIncome by viewModel.thisMonthIncomeByPaymentType.collectAsState()
    val budgets        by viewModel.budgets.collectAsState()
    val budgetSpent    by viewModel.budgetSpent.collectAsState()
    val categoriesByName by viewModel.categoriesByName.collectAsState()

    val accent = MaterialTheme.colorScheme.primary
    val resolveColor: (String) -> androidx.compose.ui.graphics.Color = remember(globalColoring, accent, categoriesByName) {
        { name -> if (globalColoring) accent else categoriesByName[name]?.color ?: accent }
    }

    val currencySymbol = LocalCurrencySymbol.current
    val monthName = LocalDate.now().month.getDisplayName(TextStyle.FULL, Locale.getDefault())

    // â"€â"€ Overlay state (lifted to this level for global dismiss & precise positioning) â"€â"€â"€â"€â"€
    var overlayTransaction by remember { mutableStateOf<Transaction?>(null) }
    var overlayBounds      by remember { mutableStateOf(Rect.Zero) }
    var homeBoxCoords      by remember { mutableStateOf<LayoutCoordinates?>(null) }

    // â"€â"€ Root box: content + scrim + overlay (Z order: content â†’ scrim â†’ overlay) â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = onToggleEditMode) {
                        Icon(
                            imageVector        = if (editMode) Icons.Rounded.Done else Icons.Rounded.Edit,
                            contentDescription = if (editMode) "Done editing" else "Edit layout",
                            tint               = if (editMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onAccountClick) {
                        Surface(
                            shape    = CircleShape,
                            color    = if (isSignedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    accountInitial.toString(),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                    color = if (isSignedIn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .onGloballyPositioned { homeBoxCoords = it }
    ) {
        // â"€â"€ Card order & visibility â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
        val cardOrder   = remember { mutableStateListOf(*TransactionViewModel.DEFAULT_CARD_ORDER.toTypedArray()) }
        val cardVisible = remember { mutableStateMapOf(*TransactionViewModel.DEFAULT_CARD_VISIBLE.entries.map { it.key to it.value }.toTypedArray()) }

        // â"€â"€ Drag-to-reorder state â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
        var draggedCardId by remember { mutableStateOf<String?>(null) }

        // Sync from persisted prefs (StateFlow always emits current value on collection)
        LaunchedEffect(Unit) {
            viewModel.cardOrderJson.collect { json ->
                if (json.isNotEmpty() && draggedCardId == null) {
                    try {
                        val arr = JSONArray(json)
                        val list = (0 until arr.length()).map { arr.getString(it) }
                        if (list.isNotEmpty()) { cardOrder.clear(); cardOrder.addAll(list) }
                    } catch (_: Exception) {}
                }
            }
        }
        LaunchedEffect(Unit) {
            viewModel.cardVisibleJson.collect { json ->
                if (json.isNotEmpty()) {
                    try {
                        val obj = JSONObject(json)
                        val map = buildMap<String, Boolean> { obj.keys().forEach { k -> put(k, obj.getBoolean(k)) } }
                        cardVisible.clear(); cardVisible.putAll(map)
                    } catch (_: Exception) {}
                }
            }
        }

        fun swapCards(a: String, b: String) {
            val i = cardOrder.indexOf(a); val j = cardOrder.indexOf(b)
            if (i >= 0 && j >= 0) { cardOrder[i] = b; cardOrder[j] = a }
        }
        val visibleCards = cardOrder.filter { cardVisible[it] == true && (it != "budgets" || budgets.isNotEmpty()) }
        val hiddenCards  = cardOrder.filter { cardVisible[it] != true  && (it != "budgets" || budgets.isNotEmpty()) }

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

        // â"€â"€ Card render dispatch â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
        val renderCard: @Composable (String) -> Unit = { cardId ->
            when (cardId) {
                "month_summary" -> Card(
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape    = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column {
                                Text("Total spent this month", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$currencySymbol ${"%.2f".format(expense)}", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black), color = MaterialTheme.colorScheme.error)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total income this month", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$currencySymbol ${"%.2f".format(income)}", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black), color = Color(0xFF4CAF50))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)
                        Spacer(Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                            PaymentType.values().forEach { pt ->
                                val spent  = byPaymentType[pt] ?: 0.0
                                val earned = byPaymentTypeIncome[pt] ?: 0.0
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onPaymentTypeClick(pt) }.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    PaymentTypeIcon(type = pt, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("$currencySymbol ${"%.0f".format(spent)}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.error)
                                        Text("/", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${"%.0f".format(earned)}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFF4CAF50))
                                    }
                                    Text(pt.displayName(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                "monthly_progress" -> Card(
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape    = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Monthly Progress - $monthName", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        MonthlyProgressChart(thisMonth = thisMonth, lastMonth = lastMonth, modifier = Modifier.fillMaxWidth())
                    }
                }
                "recent_transactions" -> RecentSpendingCard(
                    transactions = recent,
                    resolveColor = resolveColor,
                    categoryFor  = { categoriesByName[it] },
                    onLongPress  = if (editMode) { _, _ -> } else { txn, bounds -> overlayTransaction = txn; overlayBounds = bounds },
                    modifier     = Modifier.fillMaxWidth()
                )
                "top_categories" -> TopCategoriesCard(
                    categories      = topCats,
                    resolveColor    = resolveColor,
                    categoryFor     = { categoriesByName[it] },
                    onCategoryClick = onCategoryClick,
                    modifier        = Modifier.fillMaxWidth()
                )
                "budgets" -> BudgetOverviewCard(
                    budgets        = budgets,
                    budgetSpent    = budgetSpent,
                    categoryFor    = { categoriesByName[it] },
                    modifier       = Modifier.fillMaxWidth(),
                    globalColoring = globalColoring
                )

                // â"€â"€ Report cards pinned to dashboard â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
                "expense_chart" -> Card(
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape    = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Expenses - ${LocalDate.now().year}", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        ExpenseBarChart(bars = expenseChartBars, currencySymbol = currencySymbol, onBarClick = {})
                    }
                }

                "income_chart" -> {
                    val today = LocalDate.now()
                    val incomeBars = remember(allTransactions) {
                        computeMonthlyBars(allTransactions, today.year, TransactionType.INCOME)
                    }
                    Card(
                        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        shape    = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Income - ${today.year}", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(12.dp))
                            ExpenseBarChart(bars = incomeBars, currencySymbol = currencySymbol, barColor = Color(0xFF4CAF50), onBarClick = {})
                        }
                    }
                }

                "spending_category" -> {
                    val catDisplay = remember(categoryTotalsThisMonth) {
                        val top = categoryTotalsThisMonth.take(6)
                        val other = categoryTotalsThisMonth.drop(6).sumOf { it.second }
                        if (other > 0.001) top + ("Other" to other) else top
                    }
                    Card(
                        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        shape    = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Spending by Category", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(12.dp))
                            if (catDisplay.isEmpty()) {
                                Text("No expense data this month", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    DonutChart(categories = catDisplay, viewModel = viewModel, modifier = Modifier.size(140.dp))
                                    CategoryLegend(categories = catDisplay, viewModel = viewModel, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                "income_category" -> {
                    val now = YearMonth.now()
                    val incomeCatDisplay = remember(allTransactions) {
                        val totals = computeCategoryTotals(
                            allTransactions.filter { it.type == TransactionType.INCOME && YearMonth.from(it.date) == now }
                        )
                        val top = totals.take(6)
                        val other = totals.drop(6).sumOf { it.second }
                        if (other > 0.001) top + ("Other" to other) else top
                    }
                    Card(
                        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        shape    = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Income by Category", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(12.dp))
                            if (incomeCatDisplay.isEmpty()) {
                                Text("No income data this month", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    DonutChart(categories = incomeCatDisplay, viewModel = viewModel, modifier = Modifier.size(140.dp))
                                    CategoryLegend(categories = incomeCatDisplay, viewModel = viewModel, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                "highest_expense" -> {
                    var hxFilter by remember { mutableStateOf("all") }
                    val hxTxn = remember(allTransactions, hxFilter) {
                        allTransactions.filter { it.type == TransactionType.EXPENSE }
                            .let { list -> when (hxFilter) {
                                "year"  -> list.filter { it.date.year == LocalDate.now().year }
                                "month" -> list.filter { YearMonth.from(it.date) == YearMonth.now() }
                                else    -> list
                            }}.maxByOrNull { it.amount }
                    }
                    HighlightSummaryCard(
                        label          = "Highest Expense",
                        transaction    = hxTxn,
                        amountColor    = MaterialTheme.colorScheme.error,
                        amountPrefix   = "-",
                        currencySymbol = currencySymbol,
                        viewModel      = viewModel,
                        selectedFilter = hxFilter,
                        onFilterChange = { hxFilter = it },
                        modifier       = Modifier.fillMaxWidth()
                    )
                }

                "highest_income" -> {
                    var hiFilter by remember { mutableStateOf("all") }
                    val hiTxn = remember(allTransactions, hiFilter) {
                        allTransactions.filter { it.type == TransactionType.INCOME }
                            .let { list -> when (hiFilter) {
                                "year"  -> list.filter { it.date.year == LocalDate.now().year }
                                "month" -> list.filter { YearMonth.from(it.date) == YearMonth.now() }
                                else    -> list
                            }}.maxByOrNull { it.amount }
                    }
                    HighlightSummaryCard(
                        label          = "Highest Income",
                        transaction    = hiTxn,
                        amountColor    = Color(0xFF4CAF50),
                        amountPrefix   = "+",
                        currencySymbol = currencySymbol,
                        viewModel      = viewModel,
                        selectedFilter = hiFilter,
                        onFilterChange = { hiFilter = it },
                        modifier       = Modifier.fillMaxWidth()
                    )
                }

                "total_spent" -> {
                    var tsFilter by remember { mutableStateOf("month") }
                    val tsTotal = remember(allTransactions, tsFilter) {
                        allTransactions.filter { it.type == TransactionType.EXPENSE }
                            .let { list -> when (tsFilter) {
                                "year"  -> list.filter { it.date.year == LocalDate.now().year }
                                "month" -> list.filter { YearMonth.from(it.date) == YearMonth.now() }
                                "week"  -> { val s = LocalDate.now().with(java.time.DayOfWeek.MONDAY); list.filter { !it.date.isBefore(s) && !it.date.isAfter(s.plusDays(6)) } }
                                else    -> list
                            }}.sumOf { it.amount }
                    }
                    TotalSummaryCard(
                        label          = "Total Spent",
                        total          = tsTotal,
                        amountColor    = MaterialTheme.colorScheme.error,
                        amountPrefix   = "-",
                        currencySymbol = currencySymbol,
                        selectedFilter = tsFilter,
                        onFilterChange = { tsFilter = it },
                        modifier       = Modifier.fillMaxWidth()
                    )
                }

                "total_earned" -> {
                    var teFilter by remember { mutableStateOf("month") }
                    val teTotal = remember(allTransactions, teFilter) {
                        allTransactions.filter { it.type == TransactionType.INCOME }
                            .let { list -> when (teFilter) {
                                "year"  -> list.filter { it.date.year == LocalDate.now().year }
                                "month" -> list.filter { YearMonth.from(it.date) == YearMonth.now() }
                                "week"  -> { val s = LocalDate.now().with(java.time.DayOfWeek.MONDAY); list.filter { !it.date.isBefore(s) && !it.date.isAfter(s.plusDays(6)) } }
                                else    -> list
                            }}.sumOf { it.amount }
                    }
                    TotalSummaryCard(
                        label          = "Total Earned",
                        total          = teTotal,
                        amountColor    = Color(0xFF4CAF50),
                        amountPrefix   = "+",
                        currencySymbol = currencySymbol,
                        selectedFilter = teFilter,
                        onFilterChange = { teFilter = it },
                        modifier       = Modifier.fillMaxWidth()
                    )
                }

                "total_savings" -> {
                    var svFilter by remember { mutableStateOf("all") }
                    val savingsCategories = remember { setOf("Savings", "Investment") }
                    val svTxns = remember(allTransactions, svFilter) {
                        allTransactions.filter { it.category in savingsCategories }
                            .let { list -> when (svFilter) {
                                "year"  -> list.filter { it.date.year == LocalDate.now().year }
                                "month" -> list.filter { YearMonth.from(it.date) == YearMonth.now() }
                                else    -> list
                            }}.sortedByDescending { it.date.atTime(it.time) }
                    }
                    SavingsSummaryCard(
                        total          = svTxns.sumOf { it.amount },
                        latestTxn      = svTxns.firstOrNull(),
                        currencySymbol = currencySymbol,
                        viewModel      = viewModel,
                        selectedFilter = svFilter,
                        onFilterChange = { svFilter = it },
                        modifier       = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // â"€â"€ 1. Scrollable content â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
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
                    HomeEditableCardWrapper(
                        editMode        = editMode,
                        visible         = true,
                        isDragged       = draggedCardId == cardId,
                        dragOffsetY     = if (draggedCardId == cardId) dragOffsetY else 0f,
                        onDragStart     = { draggedCardId = cardId; dragOffsetY = 0f },
                        onDrag          = handleDrag,
                        onDragEnd       = { draggedCardId = null; dragOffsetY = 0f; viewModel.setCardOrder(cardOrder.toList()) },
                        onToggleVisible = { cardVisible[cardId] = false; viewModel.setCardVisible(cardVisible.toMap()) },
                        onHeightChanged = { cardHeights[cardId] = it }
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
                        HomeEditableCardWrapper(
                            editMode        = true,
                            visible         = false,
                            isDragged       = false,
                            dragOffsetY     = 0f,
                            onDragStart     = {},
                            onDrag          = {},
                            onDragEnd       = {},
                            onToggleVisible = { cardVisible[cardId] = true; viewModel.setCardVisible(cardVisible.toMap()) },
                            onHeightChanged = {}
                        ) { renderCard(cardId) }
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }

        // â"€â"€ 2. Overlay (scrim + edit/delete buttons) positioned over the long-pressed row â"€â"€
        val activeTxn = overlayTransaction
        val coords    = homeBoxCoords
        if (activeTxn != null && coords != null) {
            EditDeleteOverlay(
                transaction = activeTxn,
                bounds      = overlayBounds,
                rootCoords  = coords,
                onEdit      = { t -> onEditTransaction(t) },
                onDelete    = { t -> onDeleteTransaction(t) },
                onDismiss   = { overlayTransaction = null }
            )
        }
    }
    } // end Scaffold
}

// â"€â"€ Editable card wrapper (home screen) â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

@Composable
private fun HomeEditableCardWrapper(
    editMode: Boolean,
    visible: Boolean,
    isDragged: Boolean,
    dragOffsetY: Float,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onToggleVisible: () -> Unit,
    onHeightChanged: (Int) -> Unit,
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
                horizontalArrangement = Arrangement.End
            ) {
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
