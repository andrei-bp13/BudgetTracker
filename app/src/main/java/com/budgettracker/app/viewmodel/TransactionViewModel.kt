package com.pardonmyspending.app.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pardonmyspending.app.data.model.*
import com.pardonmyspending.app.data.model.CategoryType
import com.pardonmyspending.app.data.repository.BudgetRepository
import com.pardonmyspending.app.data.repository.CategoryRepository
import com.pardonmyspending.app.data.repository.CategorySuggestion
import com.pardonmyspending.app.data.repository.AiRepository
import com.pardonmyspending.app.data.repository.ParsedTransaction
import com.pardonmyspending.app.data.repository.PreferencesRepository
import com.pardonmyspending.app.data.repository.RecurringTransactionRepository
import com.pardonmyspending.app.data.repository.TransactionRepository
import com.pardonmyspending.app.util.budgetSpentAmount
import com.pardonmyspending.app.util.monthlyExpensesForYear
import com.pardonmyspending.app.util.weeklyExpensesForMonth
import com.pardonmyspending.app.util.yearlyExpenses
import com.pardonmyspending.app.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth

data class ChartBar(
    val label: String,
    val title: String,
    val value: Double,
    val startDate: LocalDate,
    val endDate: LocalDate
)

private val allPaymentTypes = PaymentType.values()

class TransactionViewModel(
    private val repo: TransactionRepository,
    private val prefs: PreferencesRepository,
    private val catRepo: CategoryRepository,
    private val recurringRepo: RecurringTransactionRepository,
    private val budgetRepo: BudgetRepository,
    private val appContext: Context? = null
) : ViewModel() {

    private val prefsState = PreferencesState(prefs, viewModelScope)

    private fun refreshWidgets() {
        val ctx = appContext ?: return
        viewModelScope.launch(Dispatchers.IO) {
            WidgetUpdater.updateAllWidgets(ctx)
        }
    }

    // â"€â"€ UI preferences (delegated) â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
    val themeMode:        StateFlow<String>  = prefsState.themeMode
    val accentIndex:      StateFlow<Int>     = prefsState.accentIndex
    val customAccentHex:  StateFlow<String>  = prefsState.customAccentHex
    val globalColoring:   StateFlow<Boolean> = prefsState.globalColoring
    val granularColoring: StateFlow<Boolean> = prefsState.granularColoring
    val currencyCode:     StateFlow<String>  = prefsState.currencyCode
    val chartGranularity: StateFlow<String>  = prefsState.chartGranularity
    val aiMode:           StateFlow<Boolean> = prefsState.aiMode
    val cardOrderJson:    StateFlow<String>  = prefsState.cardOrderJson
    val cardVisibleJson:  StateFlow<String>  = prefsState.cardVisibleJson
    val aiApiKey:         StateFlow<String>  = prefsState.aiApiKey

    val currencySymbol: StateFlow<String> = prefsState.currencyCode
        .map { code -> currencyCodeToSymbol[code] ?: "$" }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), "$")

    // â"€â"€ Transaction data â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

    val transactions: StateFlow<List<Transaction>> = repo.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    // True single-pass aggregation off the main thread; chart granularity bars
    // are computed separately so toggling granularity doesn't redo all aggregates.
    private data class ProcessedTransactions(
        val thisMonthDailyExpenses: List<Pair<Int, Double>>,
        val lastMonthDailyExpenses: List<Pair<Int, Double>>,
        val recentExpenses: List<Transaction>,
        val recentTransactions: List<Transaction>,
        val topCategories: List<Pair<String, Double>>,
        val monthlyTotals: List<Pair<String, Double>>,
        val categoryTotalsThisMonth: List<Pair<String, Double>>,
        val thisMonthExpense: Double,
        val thisMonthIncome: Double,
        val thisMonthByPaymentType: Map<PaymentType, Double>,
        val thisMonthIncomeByPaymentType: Map<PaymentType, Double>,
    )

    private val processed: StateFlow<ProcessedTransactions> =
        transactions.map { txns ->
            val today  = LocalDate.now()
            val now    = YearMonth.from(today)
            val lastMo = now.minusMonths(1)

            val thisMonthDays = today.dayOfMonth.coerceIn(1, now.lengthOfMonth())
            val lastMonthDays = lastMo.lengthOfMonth()
            val thisMonthByDay = DoubleArray(thisMonthDays + 1)
            val lastMonthByDay = DoubleArray(lastMonthDays + 1)

            val last6Months = (5 downTo 0).map { now.minusMonths(it.toLong()) }
            val last6Idx = HashMap<YearMonth, Int>(8).apply {
                last6Months.forEachIndexed { i, ym -> put(ym, i) }
            }
            val last6Totals = DoubleArray(6)

            val recentExp = ArrayList<Transaction>(5)
            val catTotals = HashMap<String, Double>()
            val expByPt   = HashMap<PaymentType, Double>(allPaymentTypes.size).apply {
                allPaymentTypes.forEach { put(it, 0.0) }
            }
            val incByPt   = HashMap<PaymentType, Double>(allPaymentTypes.size).apply {
                allPaymentTypes.forEach { put(it, 0.0) }
            }
            var monthExp = 0.0
            var monthInc = 0.0

            for (t in txns) {
                val ym = YearMonth.from(t.date)
                if (t.type == TransactionType.EXPENSE) {
                    if (recentExp.size < 5) recentExp.add(t)
                    val l6 = last6Idx[ym]
                    if (l6 != null) last6Totals[l6] += t.amount
                    if (ym == now) {
                        monthExp += t.amount
                        val d = t.date.dayOfMonth
                        if (d in 1..thisMonthDays) thisMonthByDay[d] += t.amount
                        catTotals[t.category] = (catTotals[t.category] ?: 0.0) + t.amount
                        expByPt[t.paymentType] = (expByPt[t.paymentType] ?: 0.0) + t.amount
                    } else if (ym == lastMo) {
                        val d = t.date.dayOfMonth
                        if (d in 1..lastMonthDays) lastMonthByDay[d] += t.amount
                    }
                } else if (ym == now) {
                    monthInc += t.amount
                    incByPt[t.paymentType] = (incByPt[t.paymentType] ?: 0.0) + t.amount
                }
            }

            var cumThis = 0.0
            val thisMonthDaily = (1..thisMonthDays).map { d ->
                cumThis += thisMonthByDay[d]
                d to cumThis
            }
            var cumLast = 0.0
            val lastMonthDaily = (1..lastMonthDays).map { d ->
                cumLast += lastMonthByDay[d]
                d to cumLast
            }
            val sortedCats = catTotals.entries
                .sortedByDescending { it.value }
                .map { it.key to it.value }
            val monthly6 = last6Months.mapIndexed { i, m ->
                "${m.month.name.take(3)} ${m.year}" to last6Totals[i]
            }

            ProcessedTransactions(
                thisMonthDailyExpenses       = thisMonthDaily,
                lastMonthDailyExpenses       = lastMonthDaily,
                recentExpenses               = recentExp,
                recentTransactions           = txns.take(5),
                topCategories                = sortedCats.take(5),
                monthlyTotals                = monthly6,
                categoryTotalsThisMonth      = sortedCats.take(10),
                thisMonthExpense             = monthExp,
                thisMonthIncome              = monthInc,
                thisMonthByPaymentType       = expByPt,
                thisMonthIncomeByPaymentType = incByPt,
            )
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            ProcessedTransactions(
                thisMonthDailyExpenses       = emptyList(),
                lastMonthDailyExpenses       = emptyList(),
                recentExpenses               = emptyList(),
                recentTransactions           = emptyList(),
                topCategories                = emptyList(),
                monthlyTotals                = emptyList(),
                categoryTotalsThisMonth      = emptyList(),
                thisMonthExpense             = 0.0,
                thisMonthIncome              = 0.0,
                thisMonthByPaymentType       = emptyMap(),
                thisMonthIncomeByPaymentType = emptyMap(),
            )
        )

    val expenseChartBars: StateFlow<List<ChartBar>> =
        combine(transactions, chartGranularity) { txns, gran ->
            val today = LocalDate.now()
            when (gran) {
                "weekly" -> weeklyExpensesForMonth(txns, today.year, today.monthValue)
                "yearly" -> yearlyExpenses(txns, today.year)
                else     -> monthlyExpensesForYear(txns, today.year)
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val thisMonthDailyExpenses: StateFlow<List<Pair<Int, Double>>> =
        processed.map { it.thisMonthDailyExpenses }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val lastMonthDailyExpenses: StateFlow<List<Pair<Int, Double>>> =
        processed.map { it.lastMonthDailyExpenses }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val recentExpenses: StateFlow<List<Transaction>> =
        processed.map { it.recentExpenses }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val recentTransactions: StateFlow<List<Transaction>> =
        processed.map { it.recentTransactions }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val topCategories: StateFlow<List<Pair<String, Double>>> =
        processed.map { it.topCategories }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val monthlyTotals: StateFlow<List<Pair<String, Double>>> =
        processed.map { it.monthlyTotals }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val categoryTotalsThisMonth: StateFlow<List<Pair<String, Double>>> =
        processed.map { it.categoryTotalsThisMonth }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val thisMonthExpense: StateFlow<Double> =
        processed.map { it.thisMonthExpense }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), 0.0)

    val thisMonthIncome: StateFlow<Double> =
        processed.map { it.thisMonthIncome }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), 0.0)

    val thisMonthByPaymentType: StateFlow<Map<PaymentType, Double>> =
        processed.map { it.thisMonthByPaymentType }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyMap())

    val thisMonthIncomeByPaymentType: StateFlow<Map<PaymentType, Double>> =
        processed.map { it.thisMonthIncomeByPaymentType }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyMap())

    // â"€â"€ Recurring transactions â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

    val recurringTransactions: StateFlow<List<RecurringTransaction>> = recurringRepo.recurring
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    private var lastRecurringCheckDate: LocalDate? = null

    fun processRecurringIfNeeded() {
        val today = LocalDate.now()
        if (lastRecurringCheckDate == today) return
        lastRecurringCheckDate = today
        viewModelScope.launch {
            recurringRepo.processRecurring(repo)
            refreshWidgets()
        }
    }

    fun addRecurring(rec: RecurringTransaction) = viewModelScope.launch {
        recurringRepo.add(rec)
        // Process immediately so past-dated recurrings generate their transactions right away
        // and nextDueDate is advanced to the correct upcoming date.
        recurringRepo.processRecurring(repo)
        refreshWidgets()
    }
    fun updateRecurring(rec: RecurringTransaction) = viewModelScope.launch { recurringRepo.update(rec) }
    fun deleteRecurring(id: String)                = viewModelScope.launch { recurringRepo.delete(id) }

    private val _dataLoaded = MutableStateFlow(false)
    val dataLoaded: StateFlow<Boolean> = _dataLoaded.asStateFlow()

    init {
        viewModelScope.launch {
            // Wait concurrently for theme prefs and both DB tables to emit their first value
            // before releasing the splash screen.
            combine(prefs.themeMode, repo.transactions, recurringRepo.recurring) { _, _, _ -> Unit }
                .first()
            _dataLoaded.value = true
        }
        processRecurringIfNeeded()
    }

    fun addTransaction(t: Transaction)    = viewModelScope.launch { repo.add(t); refreshWidgets() }
    fun updateTransaction(t: Transaction) = viewModelScope.launch { repo.update(t); refreshWidgets() }
    fun deleteTransaction(id: String)     = viewModelScope.launch { repo.delete(id); refreshWidgets() }

    private val _importErrorEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val importErrorEvent: SharedFlow<Unit> = _importErrorEvent.asSharedFlow()

    private val _ioErrorEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val ioErrorEvent: SharedFlow<String> = _ioErrorEvent.asSharedFlow()

    fun exportCsv(uri: Uri) = viewModelScope.launch {
        runCatching { repo.exportCsv(uri) }
            .onFailure { _ioErrorEvent.tryEmit("Export failed: ${it.message ?: "unable to write file"}") }
    }

    fun importCsv(uri: Uri) = viewModelScope.launch {
        runCatching { repo.importCsv(uri) }
            .onSuccess { failed ->
                refreshWidgets()
                if (failed > 0) _importErrorEvent.tryEmit(Unit)
            }
            .onFailure { _ioErrorEvent.tryEmit("Import failed: ${it.message ?: "unable to read file"}") }
    }

    fun setCardOrder(order: List<String>) =
        prefsState.setCardOrder(JSONArray(order).toString())

    fun setCardVisible(visible: Map<String, Boolean>) {
        val obj = JSONObject()
        visible.forEach { (k, v) -> obj.put(k, v) }
        prefsState.setCardVisible(obj.toString())
    }

    // â"€â"€ Categories â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

    val categories: StateFlow<List<CategoryItem>> = catRepo.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), DefaultCategories)

    val categoriesByName: StateFlow<Map<String, CategoryItem>> =
        categories.map { list -> list.associateBy { it.name } }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000L),
                DefaultCategories.associateBy { it.name }
            )

    // Single partition pass instead of two separate filter passes.
    private val partitionedCategories: StateFlow<Pair<List<CategoryItem>, List<CategoryItem>>> =
        categories.map { list -> list.partition { it.type == CategoryType.INCOME } }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000L),
                emptyList<CategoryItem>() to emptyList()
            )

    val incomeCategories: StateFlow<List<CategoryItem>> =
        partitionedCategories.map { it.first }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val expenseCategories: StateFlow<List<CategoryItem>> =
        partitionedCategories.map { it.second }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    fun saveCategory(item: CategoryItem) = viewModelScope.launch {
        catRepo.upsertCategory(categories.value, item)
    }

    fun deleteCategory(item: CategoryItem) = viewModelScope.launch {
        catRepo.save(categories.value.filter { it.id != item.id })
    }

    fun resetCategories() = viewModelScope.launch {
        catRepo.save(emptyList())
    }

    // â"€â"€ Budgets â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

    val budgets: StateFlow<List<Budget>> = budgetRepo.budgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val budgetSpent: StateFlow<Map<String, Double>> =
        combine(transactions, budgets) { txns, bgets ->
            val today = LocalDate.now()
            bgets.associate { b -> b.id to budgetSpentAmount(txns, b, today) }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyMap())

    fun addBudget(budget: Budget)                = viewModelScope.launch { budgetRepo.add(budget); refreshWidgets() }
    fun updateBudget(budget: Budget)             = viewModelScope.launch { budgetRepo.update(budget); refreshWidgets() }
    fun deleteBudget(id: String)                 = viewModelScope.launch { budgetRepo.delete(id); refreshWidgets() }
    fun updateBudgetOrder(ordered: List<Budget>) = viewModelScope.launch { budgetRepo.updateOrder(ordered); refreshWidgets() }

    fun categoryFor(name: String): CategoryItem? =
        categoriesByName.value[name]

    // â"€â"€ Preference setters (delegated) â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

    fun setThemeMode(mode: String)         = prefsState.setThemeMode(mode)
    fun setAccentIndex(i: Int)             = prefsState.setAccentIndex(i)
    fun setGlobalColoring(v: Boolean)      = prefsState.setGlobalColoring(v)
    fun setGranularColoring(v: Boolean)    = prefsState.setGranularColoring(v)
    fun setCurrencyCode(code: String)      { prefsState.setCurrencyCode(code); refreshWidgets() }
    fun setChartGranularity(value: String) = prefsState.setChartGranularity(value)
    fun setAiMode(enabled: Boolean)        = prefsState.setAiMode(enabled)
    fun setCustomAccentHex(hex: String)    = prefsState.setCustomAccentHex(hex)
    fun setAiApiKey(key: String)           = prefsState.setAiApiKey(key)

    // â"€â"€ Mistral AI â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

    private val aiRepo = AiRepository()

    suspend fun parseTransactionWithAI(input: String): Result<ParsedTransaction> =
        aiRepo.parseTransaction(
            apiKey             = aiApiKey.value,
            input              = input,
            expenseCategories  = expenseCategories.value.map { it.name },
            incomeCategories   = incomeCategories.value.map { it.name },
            today              = LocalDate.now()
        )

    suspend fun suggestCategoryWithAI(title: String): CategorySuggestion? =
        aiRepo.suggestCategory(
            apiKey             = aiApiKey.value,
            title              = title,
            expenseCategories  = expenseCategories.value.map { it.name },
            incomeCategories   = incomeCategories.value.map { it.name }
        )

    companion object {
        val DEFAULT_CARD_ORDER = listOf(
            "month_summary", "monthly_progress", "recent_transactions", "top_categories", "budgets"
        )
        val DEFAULT_CARD_VISIBLE = mapOf(
            "month_summary" to true, "monthly_progress" to true,
            "recent_transactions" to true, "top_categories" to true, "budgets" to false
        )
        // Pre-built lookup avoids a linear scan of AllCurrencies on every currencyCode emission.
        val currencyCodeToSymbol: Map<String, String> by lazy {
            AllCurrencies.associate { it.code to it.symbol }
        }
    }

    class Factory(
        private val repo: TransactionRepository,
        private val prefs: PreferencesRepository,
        private val catRepo: CategoryRepository,
        private val recurringRepo: RecurringTransactionRepository,
        private val budgetRepo: BudgetRepository,
        private val appContext: Context? = null
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return TransactionViewModel(repo, prefs, catRepo, recurringRepo, budgetRepo, appContext) as T
        }
    }
}
