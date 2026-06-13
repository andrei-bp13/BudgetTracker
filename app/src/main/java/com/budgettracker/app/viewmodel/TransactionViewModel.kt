package com.budgettracker.app.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.budgettracker.app.data.model.AllCurrencies
import com.budgettracker.app.data.model.CategoryItem
import com.budgettracker.app.data.model.DefaultCategories
import com.budgettracker.app.data.model.PaymentType
import com.budgettracker.app.data.model.Transaction
import com.budgettracker.app.data.model.TransactionType
import com.budgettracker.app.data.repository.CategoryRepository
import com.budgettracker.app.data.repository.PreferencesRepository
import com.budgettracker.app.data.repository.TransactionRepository
import com.budgettracker.app.util.dailyExpenses
import com.budgettracker.app.util.monthTotal
import com.budgettracker.app.util.monthlyExpensesForYear
import com.budgettracker.app.util.monthlyExpensesLast6
import com.budgettracker.app.util.topCats
import com.budgettracker.app.util.weeklyExpensesForMonth
import com.budgettracker.app.util.yearlyExpenses
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

class TransactionViewModel(
    private val repo: TransactionRepository,
    private val prefs: PreferencesRepository,
    private val catRepo: CategoryRepository
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
        .map { code -> AllCurrencies.firstOrNull { it.code == code }?.symbol ?: "$" }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, "$")

    // ── Transaction data ──────────────────────────────────────────────────────

    val transactions: StateFlow<List<Transaction>> = repo.transactions

    val thisMonthDailyExpenses: StateFlow<List<Pair<Int, Double>>> = transactions
        .map { dailyExpenses(it, YearMonth.now()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val lastMonthDailyExpenses: StateFlow<List<Pair<Int, Double>>> = transactions
        .map { dailyExpenses(it, YearMonth.now().minusMonths(1)) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recentExpenses: StateFlow<List<Transaction>> = transactions
        .map { it.filter { t -> t.type == TransactionType.EXPENSE }.take(5) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recentTransactions: StateFlow<List<Transaction>> = transactions
        .map { it.take(5) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val topCategories: StateFlow<List<Pair<String, Double>>> = transactions
        .map { topCats(it, YearMonth.now()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val expenseChartBars: StateFlow<List<ChartBar>> =
        combine(transactions, chartGranularity) { txns, gran ->
            val today = LocalDate.now()
            when (gran) {
                "weekly"  -> weeklyExpensesForMonth(txns, today.year, today.monthValue)
                "yearly"  -> yearlyExpenses(txns, today.year)
                else      -> monthlyExpensesForYear(txns, today.year)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val monthlyTotals: StateFlow<List<Pair<String, Double>>> = transactions
        .map { monthlyExpensesLast6(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val categoryTotalsThisMonth: StateFlow<List<Pair<String, Double>>> = transactions
        .map { topCats(it, YearMonth.now(), limit = 10) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val thisMonthExpense: StateFlow<Double> = transactions
        .map { monthTotal(it, YearMonth.now(), TransactionType.EXPENSE) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val thisMonthIncome: StateFlow<Double> = transactions
        .map { monthTotal(it, YearMonth.now(), TransactionType.INCOME) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val thisMonthByPaymentType: StateFlow<Map<PaymentType, Double>> = transactions
        .map { list ->
            val now = YearMonth.now()
            PaymentType.values().associateWith { pt ->
                list.filter {
                    it.type == TransactionType.EXPENSE &&
                    YearMonth.from(it.date) == now &&
                    it.paymentType == pt
                }.sumOf { it.amount }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val thisMonthIncomeByPaymentType: StateFlow<Map<PaymentType, Double>> = transactions
        .map { list ->
            val now = YearMonth.now()
            PaymentType.values().associateWith { pt ->
                list.filter {
                    it.type == TransactionType.INCOME &&
                    YearMonth.from(it.date) == now &&
                    it.paymentType == pt
                }.sumOf { it.amount }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    init {
        viewModelScope.launch { repo.load() }
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

    private companion object {
        val incomeNames = setOf("Salary", "Freelance", "Investment", "Other Income")
    }

    val categories: StateFlow<List<CategoryItem>> = catRepo.categories
        .stateIn(viewModelScope, SharingStarted.Eagerly, DefaultCategories)

    val expenseCategories: StateFlow<List<CategoryItem>> = categories
        .map { list -> list.filter { it.name !in incomeNames } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val incomeCategories: StateFlow<List<CategoryItem>> = categories
        .map { list -> list.filter { it.name in incomeNames } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun saveCategory(item: CategoryItem) = viewModelScope.launch {
        catRepo.upsertCategory(categories.value, item)
    }

    fun deleteCategory(item: CategoryItem) = viewModelScope.launch {
        catRepo.save(categories.value.filter { it.id != item.id })
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

    class Factory(
        private val repo: TransactionRepository,
        private val prefs: PreferencesRepository,
        private val catRepo: CategoryRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return TransactionViewModel(repo, prefs, catRepo) as T
        }
    }
}
