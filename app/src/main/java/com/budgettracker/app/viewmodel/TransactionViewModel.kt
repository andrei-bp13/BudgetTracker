package com.pardonmyspending.app.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pardonmyspending.app.data.model.*
import com.pardonmyspending.app.data.model.CategoryType
import com.pardonmyspending.app.data.repository.CategoryRepository
import com.pardonmyspending.app.data.repository.PreferencesRepository
import com.pardonmyspending.app.data.repository.RecurringTransactionRepository
import com.pardonmyspending.app.data.repository.TransactionRepository
import com.pardonmyspending.app.util.dailyExpenses
import com.pardonmyspending.app.util.monthTotal
import com.pardonmyspending.app.util.monthlyExpensesForYear
import com.pardonmyspending.app.util.monthlyExpensesLast6
import com.pardonmyspending.app.util.topCats
import com.pardonmyspending.app.util.weeklyExpensesForMonth
import com.pardonmyspending.app.util.yearlyExpenses
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    private val recurringRepo: RecurringTransactionRepository
) : ViewModel() {

    private val prefsState = PreferencesState(prefs, viewModelScope)

    // ── UI preferences (delegated) ────────────────────────────────────────────
    val themeMode:        StateFlow<String>  = prefsState.themeMode
    val accentIndex:      StateFlow<Int>     = prefsState.accentIndex
    val customAccentHex:  StateFlow<String>  = prefsState.customAccentHex
    val globalColoring:   StateFlow<Boolean> = prefsState.globalColoring
    val granularColoring: StateFlow<Boolean> = prefsState.granularColoring
    val currencyCode:     StateFlow<String>  = prefsState.currencyCode
    val chartGranularity: StateFlow<String>  = prefsState.chartGranularity
    val aiMode:           StateFlow<Boolean> = prefsState.aiMode

    val currencySymbol: StateFlow<String> = prefsState.currencyCode
        .map { code -> currencyCodeToSymbol[code] ?: "$" }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), "$")

    // ── Transaction data ──────────────────────────────────────────────────────

    val transactions: StateFlow<List<Transaction>> = repo.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    // Single combine pass: all derived aggregates computed together so that one
    // transaction change triggers one list iteration instead of 10+.
    private data class ProcessedTransactions(
        val thisMonthDailyExpenses: List<Pair<Int, Double>>,
        val lastMonthDailyExpenses: List<Pair<Int, Double>>,
        val recentExpenses: List<Transaction>,
        val recentTransactions: List<Transaction>,
        val topCategories: List<Pair<String, Double>>,
        val expenseChartBars: List<ChartBar>,
        val monthlyTotals: List<Pair<String, Double>>,
        val categoryTotalsThisMonth: List<Pair<String, Double>>,
        val thisMonthExpense: Double,
        val thisMonthIncome: Double,
        val thisMonthByPaymentType: Map<PaymentType, Double>,
        val thisMonthIncomeByPaymentType: Map<PaymentType, Double>,
    )

    private val processed: StateFlow<ProcessedTransactions> =
        combine(transactions, chartGranularity) { txns, gran ->
            val today  = LocalDate.now()
            val now    = YearMonth.now()
            val lastMo = now.minusMonths(1)
            ProcessedTransactions(
                thisMonthDailyExpenses       = dailyExpenses(txns, now, today.dayOfMonth),
                lastMonthDailyExpenses       = dailyExpenses(txns, lastMo),
                recentExpenses               = txns.filter { it.type == TransactionType.EXPENSE }.take(5),
                recentTransactions           = txns.take(5),
                topCategories                = topCats(txns, now),
                expenseChartBars             = when (gran) {
                    "weekly" -> weeklyExpensesForMonth(txns, today.year, today.monthValue)
                    "yearly" -> yearlyExpenses(txns, today.year)
                    else     -> monthlyExpensesForYear(txns, today.year)
                },
                monthlyTotals                = monthlyExpensesLast6(txns),
                categoryTotalsThisMonth      = topCats(txns, now, limit = 10),
                thisMonthExpense             = monthTotal(txns, now, TransactionType.EXPENSE),
                thisMonthIncome              = monthTotal(txns, now, TransactionType.INCOME),
                thisMonthByPaymentType       = allPaymentTypes.associateWith { pt ->
                    txns.sumOf {
                        if (it.type == TransactionType.EXPENSE &&
                            YearMonth.from(it.date) == now &&
                            it.paymentType == pt) it.amount else 0.0
                    }
                },
                thisMonthIncomeByPaymentType = allPaymentTypes.associateWith { pt ->
                    txns.sumOf {
                        if (it.type == TransactionType.INCOME &&
                            YearMonth.from(it.date) == now &&
                            it.paymentType == pt) it.amount else 0.0
                    }
                },
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            ProcessedTransactions(
                thisMonthDailyExpenses       = emptyList(),
                lastMonthDailyExpenses       = emptyList(),
                recentExpenses               = emptyList(),
                recentTransactions           = emptyList(),
                topCategories                = emptyList(),
                expenseChartBars             = emptyList(),
                monthlyTotals                = emptyList(),
                categoryTotalsThisMonth      = emptyList(),
                thisMonthExpense             = 0.0,
                thisMonthIncome              = 0.0,
                thisMonthByPaymentType       = emptyMap(),
                thisMonthIncomeByPaymentType = emptyMap(),
            )
        )

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

    val expenseChartBars: StateFlow<List<ChartBar>> =
        processed.map { it.expenseChartBars }
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

    // ── Recurring transactions ────────────────────────────────────────────────

    val recurringTransactions: StateFlow<List<RecurringTransaction>> = recurringRepo.recurring
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    fun addRecurring(rec: RecurringTransaction)    = viewModelScope.launch { recurringRepo.add(rec) }
    fun updateRecurring(rec: RecurringTransaction) = viewModelScope.launch { recurringRepo.update(rec) }
    fun deleteRecurring(id: String)                = viewModelScope.launch { recurringRepo.delete(id) }

    private val _dataLoaded = MutableStateFlow(false)
    val dataLoaded: StateFlow<Boolean> = _dataLoaded.asStateFlow()

    init {
        viewModelScope.launch {
            // Wait for theme prefs and both DB tables to emit their first value
            // before releasing the splash screen.
            prefs.themeMode.first()
            repo.transactions.first()
            recurringRepo.recurring.first()
            _dataLoaded.value = true
        }
        viewModelScope.launch { recurringRepo.processRecurring(repo) }
    }

    fun addTransaction(t: Transaction)    = viewModelScope.launch { repo.add(t) }
    fun updateTransaction(t: Transaction) = viewModelScope.launch { repo.update(t) }
    fun deleteTransaction(id: String)     = viewModelScope.launch { repo.delete(id) }

    fun exportCsv(uri: Uri) = viewModelScope.launch { repo.exportCsv(uri) }

    private val _importErrorEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val importErrorEvent: SharedFlow<Unit> = _importErrorEvent.asSharedFlow()

    fun importCsv(uri: Uri) = viewModelScope.launch {
        val failed = repo.importCsv(uri)
        if (failed > 0) _importErrorEvent.tryEmit(Unit)
    }

    // ── Categories ────────────────────────────────────────────────────────────

    val categories: StateFlow<List<CategoryItem>> = catRepo.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), DefaultCategories)

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

    fun categoryFor(name: String): CategoryItem? =
        categories.value.firstOrNull { it.name == name }

    // ── Preference setters (delegated) ────────────────────────────────────────

    fun setThemeMode(mode: String)         = prefsState.setThemeMode(mode)
    fun setAccentIndex(i: Int)             = prefsState.setAccentIndex(i)
    fun setGlobalColoring(v: Boolean)      = prefsState.setGlobalColoring(v)
    fun setGranularColoring(v: Boolean)    = prefsState.setGranularColoring(v)
    fun setCurrencyCode(code: String)      = prefsState.setCurrencyCode(code)
    fun setChartGranularity(value: String) = prefsState.setChartGranularity(value)
    fun setAiMode(enabled: Boolean)        = prefsState.setAiMode(enabled)
    fun setCustomAccentHex(hex: String)    = prefsState.setCustomAccentHex(hex)

    private companion object {
        // Pre-built lookup avoids a linear scan of AllCurrencies on every currencyCode emission.
        val currencyCodeToSymbol: Map<String, String> by lazy {
            AllCurrencies.associate { it.code to it.symbol }
        }
    }

    class Factory(
        private val repo: TransactionRepository,
        private val prefs: PreferencesRepository,
        private val catRepo: CategoryRepository,
        private val recurringRepo: RecurringTransactionRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return TransactionViewModel(repo, prefs, catRepo, recurringRepo) as T
        }
    }
}
