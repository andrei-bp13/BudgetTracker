package com.pardonmyspending.app.widget

import android.content.Context
import com.pardonmyspending.app.data.db.AppDatabase
import com.pardonmyspending.app.data.db.toBudget
import com.pardonmyspending.app.data.db.toTransaction
import com.pardonmyspending.app.data.model.AllCurrencies
import com.pardonmyspending.app.data.model.Budget
import com.pardonmyspending.app.data.model.PaymentType
import com.pardonmyspending.app.data.model.Transaction
import com.pardonmyspending.app.data.model.TransactionType
import com.pardonmyspending.app.data.repository.PreferencesRepository
import com.pardonmyspending.app.util.budgetSpentAmount
import com.pardonmyspending.app.util.monthTotal
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

data class PaymentTypeRow(val label: String, val expense: Double, val income: Double)

/** [changePercent] is null when there's no prior-month data to compare against. */
data class TrendValue(val current: Double, val changePercent: Double?)

data class MonthSummaryData(
    val spentTrend: TrendValue,
    val earnedTrend: TrendValue,
    val paymentRows: List<PaymentTypeRow>
)

data class CalendarWidgetData(
    val monthName: String,
    val year: Int,
    val month: Int,
    val firstDayOffset: Int,   // 0 = Monday, 6 = Sunday
    val daysInMonth: Int,
    val todayDay: Int,
    val daysWithTransactions: Set<Int>
)

class WidgetDataRepository(context: Context) {

    private val db    = AppDatabase.getInstance(context)
    private val prefs = PreferencesRepository(context)

    suspend fun currencySymbol(): String {
        val code = prefs.currencyCode.first()
        return AllCurrencies.firstOrNull { it.code == code }?.symbol ?: "$"
    }

    private suspend fun transactions(): List<Transaction> =
        db.transactionDao().observeAll().first().map { it.toTransaction() }

    private suspend fun budgets(): List<Budget> =
        db.budgetDao().observeAll().first().map { it.toBudget() }

    suspend fun totalSpentThisMonth(): Double =
        monthTotal(transactions(), YearMonth.now(), TransactionType.EXPENSE)

    suspend fun totalEarnedThisMonth(): Double =
        monthTotal(transactions(), YearMonth.now(), TransactionType.INCOME)

    private fun trendOf(txns: List<Transaction>, type: TransactionType): TrendValue {
        val now      = YearMonth.now()
        val current  = monthTotal(txns, now, type)
        val previous = monthTotal(txns, now.minusMonths(1), type)
        val pct      = if (previous > 0.01) (current - previous) / previous * 100.0 else null
        return TrendValue(current, pct)
    }

    suspend fun totalSpentTrend(): TrendValue  = trendOf(transactions(), TransactionType.EXPENSE)
    suspend fun totalEarnedTrend(): TrendValue = trendOf(transactions(), TransactionType.INCOME)

    suspend fun totalSavings(): Double {
        val savingsCategories = setOf("Savings", "Investment")
        return transactions().filter { it.category in savingsCategories }.sumOf { it.amount }
    }

    suspend fun highestExpense(): Transaction? =
        transactions().filter { it.type == TransactionType.EXPENSE }.maxByOrNull { it.amount }

    suspend fun highestIncome(): Transaction? =
        transactions().filter { it.type == TransactionType.INCOME }.maxByOrNull { it.amount }

    suspend fun budgetProgress(): Triple<Double, Double, String>? {
        val budget = budgets().firstOrNull() ?: return null
        val spent  = budgetSpentAmount(transactions(), budget)
        return Triple(spent, budget.amount, budget.title)
    }

    suspend fun monthSummary(): MonthSummaryData {
        val all  = transactions()
        val now  = YearMonth.now()
        val txns = all.filter { YearMonth.from(it.date) == now }
        val rows = PaymentType.values().map { pt ->
            val exp = txns.filter { it.type == TransactionType.EXPENSE && it.paymentType == pt }.sumOf { it.amount }
            val inc = txns.filter { it.type == TransactionType.INCOME  && it.paymentType == pt }.sumOf { it.amount }
            PaymentTypeRow(
                label   = pt.name.lowercase().replaceFirstChar { it.titlecase() },
                expense = exp,
                income  = inc
            )
        }
        return MonthSummaryData(
            spentTrend  = trendOf(all, TransactionType.EXPENSE),
            earnedTrend = trendOf(all, TransactionType.INCOME),
            paymentRows = rows
        )
    }

    suspend fun calendarData(): CalendarWidgetData {
        val now          = YearMonth.now()
        val today        = LocalDate.now()
        val firstDay     = now.atDay(1)
        val txns         = transactions().filter { YearMonth.from(it.date) == now }
        val daysWithTxns = txns.map { it.date.dayOfMonth }.toSet()
        return CalendarWidgetData(
            monthName            = now.month.getDisplayName(JavaTextStyle.FULL, Locale.getDefault()),
            year                 = now.year,
            month                = now.monthValue,
            firstDayOffset       = firstDay.dayOfWeek.value - 1,  // Mon=0 … Sun=6
            daysInMonth          = now.lengthOfMonth(),
            todayDay             = if (YearMonth.from(today) == now) today.dayOfMonth else -1,
            daysWithTransactions = daysWithTxns
        )
    }
}
