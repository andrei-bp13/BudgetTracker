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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class TransactionViewModel(
    private val repo: TransactionRepository,
    private val prefs: PreferencesRepository,
    private val catRepo: CategoryRepository
) : ViewModel() {

    val transactions: StateFlow<List<Transaction>> = repo.transactions

    val themeMode: StateFlow<String> = prefs.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, "system")

    val accentIndex: StateFlow<Int> = prefs.accentColorIndex
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val globalColoring: StateFlow<Boolean> = prefs.globalColoring
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val granularColoring: StateFlow<Boolean> = prefs.granularColoring
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val currencyCode: StateFlow<String> = prefs.currencyCode
        .stateIn(viewModelScope, SharingStarted.Eagerly, "USD")

    val currencySymbol: StateFlow<String> = currencyCode
        .map { code -> AllCurrencies.firstOrNull { it.code == code }?.symbol ?: "$" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "$")

    // ── Derived this-month / last-month data for the chart ───────────────────

    val thisMonthDailyExpenses: StateFlow<List<Pair<Int, Double>>> = transactions
        .map { dailyExpenses(it, YearMonth.now()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val lastMonthDailyExpenses: StateFlow<List<Pair<Int, Double>>> = transactions
        .map { dailyExpenses(it, YearMonth.now().minusMonths(1)) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── Recent transactions (last 5 expenses) ────────────────────────────────

    val recentExpenses: StateFlow<List<Transaction>> = transactions
        .map { it.filter { t -> t.type == TransactionType.EXPENSE }.take(5) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── Top categories this month ─────────────────────────────────────────────

    val topCategories: StateFlow<List<Pair<String, Double>>> = transactions
        .map { topCats(it, YearMonth.now()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── Reports: monthly expense totals for last 6 months ────────────────────

    val monthlyTotals: StateFlow<List<Pair<String, Double>>> = transactions
        .map { monthlyExpenses(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val categoryTotalsThisMonth: StateFlow<List<Pair<String, Double>>> = transactions
        .map { topCats(it, YearMonth.now(), limit = 10) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── Totals ────────────────────────────────────────────────────────────────

    val thisMonthExpense: StateFlow<Double> = transactions
        .map { monthTotal(it, YearMonth.now(), TransactionType.EXPENSE) }
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

    init {
        viewModelScope.launch { repo.load() }
    }

    fun addTransaction(t: Transaction) = viewModelScope.launch { repo.add(t) }
    fun updateTransaction(t: Transaction) = viewModelScope.launch { repo.update(t) }
    fun deleteTransaction(id: String) = viewModelScope.launch { repo.delete(id) }

    fun exportCsv(uri: Uri) = viewModelScope.launch { repo.exportCsv(uri) }
    fun importCsv(uri: Uri) = viewModelScope.launch { repo.importCsv(uri) }

    val categories: StateFlow<List<CategoryItem>> = catRepo.categories
        .stateIn(viewModelScope, SharingStarted.Eagerly, DefaultCategories)

    fun saveCategory(item: CategoryItem) = viewModelScope.launch {
        val current = categories.value.toMutableList()
        val idx = current.indexOfFirst { it.id == item.id }
        if (idx >= 0) current[idx] = item else current.add(item)
        catRepo.save(current)
    }

    fun deleteCategory(item: CategoryItem) = viewModelScope.launch {
        val updated = categories.value.filter { it.id != item.id }
        catRepo.save(updated)
    }

    fun categoryFor(name: String): CategoryItem? =
        categories.value.firstOrNull { it.name == name }

    fun setThemeMode(mode: String)      = viewModelScope.launch { prefs.setThemeMode(mode) }
    fun setAccentIndex(i: Int)          = viewModelScope.launch { prefs.setAccentColorIndex(i) }
    fun setGlobalColoring(v: Boolean)   = viewModelScope.launch { prefs.setGlobalColoring(v) }
    fun setGranularColoring(v: Boolean) = viewModelScope.launch { prefs.setGranularColoring(v) }
    fun setCurrencyCode(code: String)   = viewModelScope.launch { prefs.setCurrencyCode(code) }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun dailyExpenses(list: List<Transaction>, month: YearMonth): List<Pair<Int, Double>> {
        val days = month.lengthOfMonth()
        val byDay = list
            .filter { it.type == TransactionType.EXPENSE && YearMonth.from(it.date) == month }
            .groupBy { it.date.dayOfMonth }
            .mapValues { (_, v) -> v.sumOf { it.amount } }

        var cumulative = 0.0
        return (1..days).map { d ->
            cumulative += byDay[d] ?: 0.0
            d to cumulative
        }
    }

    private fun topCats(list: List<Transaction>, month: YearMonth, limit: Int = 5): List<Pair<String, Double>> =
        list.filter { it.type == TransactionType.EXPENSE && YearMonth.from(it.date) == month }
            .groupBy { it.category }
            .mapValues { (_, v) -> v.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value }

    private fun monthlyExpenses(list: List<Transaction>): List<Pair<String, Double>> {
        val now = YearMonth.now()
        return (5 downTo 0).map { offset ->
            val month = now.minusMonths(offset.toLong())
            val label = "${month.month.name.take(3)} ${month.year}"
            label to monthTotal(list, month, TransactionType.EXPENSE)
        }
    }

    private fun monthTotal(list: List<Transaction>, month: YearMonth, type: TransactionType): Double =
        list.filter { it.type == type && YearMonth.from(it.date) == month }.sumOf { it.amount }

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
