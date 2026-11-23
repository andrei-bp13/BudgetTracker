package com.pardonmyspending.app.widget

import android.content.Context
import com.pardonmyspending.app.data.db.AppDatabase
import com.pardonmyspending.app.data.db.toBudget
import com.pardonmyspending.app.data.db.toTransaction
import com.pardonmyspending.app.data.model.BudgetPeriod
import com.pardonmyspending.app.data.model.PaymentType
import com.pardonmyspending.app.data.model.Transaction
import com.pardonmyspending.app.data.model.TransactionType
import com.pardonmyspending.app.data.repository.PreferencesRepository
import com.pardonmyspending.app.viewmodel.TransactionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.time.temporal.TemporalAdjusters
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
        return TransactionViewModel.currencyCodeToSymbol[code] ?: "$"
    }

    private suspend fun monthTotal(month: YearMonth, type: TransactionType): Double =
        db.transactionDao().sumByTypeAndDateRange(
            type          = type.name,
            startEpochDay = month.atDay(1).toEpochDay(),
            endEpochDay   = month.atEndOfMonth().toEpochDay()
        )

    private suspend fun trendOf(type: TransactionType): TrendValue {
        val now      = YearMonth.now()
        val current  = monthTotal(now, type)
        val previous = monthTotal(now.minusMonths(1), type)
        val pct      = if (previous > 0.01) (current - previous) / previous * 100.0 else null
        return TrendValue(current, pct)
    }

    suspend fun totalSpentThisMonth(): Double = withContext(Dispatchers.IO) {
        monthTotal(YearMonth.now(), TransactionType.EXPENSE)
    }

    suspend fun totalEarnedThisMonth(): Double = withContext(Dispatchers.IO) {
        monthTotal(YearMonth.now(), TransactionType.INCOME)
    }

    suspend fun totalSpentTrend(): TrendValue = withContext(Dispatchers.IO) {
        trendOf(TransactionType.EXPENSE)
    }

    suspend fun totalEarnedTrend(): TrendValue = withContext(Dispatchers.IO) {
        trendOf(TransactionType.INCOME)
    }

    suspend fun totalSavings(): Double = withContext(Dispatchers.IO) {
        db.transactionDao().sumByCategories(listOf("Savings", "Investment"))
    }

    suspend fun highestExpense(): Transaction? = withContext(Dispatchers.IO) {
        db.transactionDao().getHighestByType(TransactionType.EXPENSE.name)?.toTransaction()
    }

    suspend fun highestIncome(): Transaction? = withContext(Dispatchers.IO) {
        db.transactionDao().getHighestByType(TransactionType.INCOME.name)?.toTransaction()
    }

    suspend fun budgetProgress(): Triple<Double, Double, String>? = withContext(Dispatchers.IO) {
        val budget = db.budgetDao().getFirst()?.toBudget() ?: return@withContext null
        val today  = LocalDate.now()
        val dao    = db.transactionDao()
        val spent  = when (budget.period) {
            BudgetPeriod.ONE_TIME -> dao.sumByCategoryFromDate(
                TransactionType.EXPENSE.name, budget.categoryName, budget.createdEpochDay
            )
            BudgetPeriod.WEEKLY -> {
                val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                dao.sumByCategoryAndDateRange(
                    TransactionType.EXPENSE.name, budget.categoryName,
                    weekStart.toEpochDay(), today.toEpochDay()
                )
            }
            BudgetPeriod.MONTHLY -> {
                val ym = YearMonth.from(today)
                dao.sumByCategoryAndDateRange(
                    TransactionType.EXPENSE.name, budget.categoryName,
                    ym.atDay(1).toEpochDay(), ym.atEndOfMonth().toEpochDay()
                )
            }
            BudgetPeriod.YEARLY -> {
                dao.sumByCategoryAndDateRange(
                    TransactionType.EXPENSE.name, budget.categoryName,
                    LocalDate.of(today.year, 1, 1).toEpochDay(),
                    LocalDate.of(today.year, 12, 31).toEpochDay()
                )
            }
        }
        Triple(spent, budget.amount, budget.title)
    }

    suspend fun monthSummary(): MonthSummaryData = withContext(Dispatchers.IO) {
        val now   = YearMonth.now()
        val start = now.atDay(1).toEpochDay()
        val end   = now.atEndOfMonth().toEpochDay()
        val totals = db.transactionDao().getPaymentTypeTotalsInRange(start, end)
        val byKey  = totals.associateBy { it.type to it.paymentType }

        val rows = PaymentType.values().map { pt ->
            val exp = byKey[TransactionType.EXPENSE.name to pt.name]?.total ?: 0.0
            val inc = byKey[TransactionType.INCOME.name to pt.name]?.total ?: 0.0
            PaymentTypeRow(
                label   = pt.name.lowercase().replaceFirstChar { it.titlecase() },
                expense = exp,
                income  = inc
            )
        }
        MonthSummaryData(
            spentTrend  = trendOf(TransactionType.EXPENSE),
            earnedTrend = trendOf(TransactionType.INCOME),
            paymentRows = rows
        )
    }

    suspend fun calendarData(): CalendarWidgetData = withContext(Dispatchers.IO) {
        val now      = YearMonth.now()
        val today    = LocalDate.now()
        val firstDay = now.atDay(1)
        val epochDays = db.transactionDao().getDistinctDateEpochDaysInRange(
            firstDay.toEpochDay(), now.atEndOfMonth().toEpochDay()
        )
        val daysWithTxns = epochDays.mapTo(HashSet(epochDays.size)) { LocalDate.ofEpochDay(it).dayOfMonth }
        CalendarWidgetData(
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
