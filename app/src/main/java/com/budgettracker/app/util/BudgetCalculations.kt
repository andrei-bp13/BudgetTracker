package com.pardonmyspending.app.util

import com.pardonmyspending.app.data.model.Budget
import com.pardonmyspending.app.data.model.BudgetPeriod
import com.pardonmyspending.app.data.model.Transaction
import com.pardonmyspending.app.data.model.TransactionType
import com.pardonmyspending.app.viewmodel.ChartBar
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

// â"€â"€ Shared data class for category-period chips â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

data class CatPeriod(
    val label: String,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate
)

// â"€â"€ Bar-chart helpers (context-aware, fully injectable for tests) â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

/** Mon-Sun-ish 7-day windows covering a month: (label, start, end). Shared by computeWeeklyBars and catPeriods. */
private fun weekWindows(year: Int, month: Int): List<Triple<String, LocalDate, LocalDate>> {
    val ym  = YearMonth.of(year, month)
    val dim = ym.lengthOfMonth()
    return buildList {
        var day = 1
        while (day <= dim) {
            val endDay = minOf(day + 6, dim)
            val label  = if (day == endDay) "$day" else "$day-$endDay"
            add(Triple(label, ym.atDay(day), ym.atDay(endDay)))
            day += 7
        }
    }
}

/** Earliest transaction year through [today]'s year. Shared by computeYearlyBars and catPeriods. */
private fun yearRange(transactions: List<Transaction>, today: LocalDate): IntRange {
    val firstYear = transactions.minOfOrNull { it.date.year } ?: today.year
    return firstYear..today.year
}

fun computeMonthlyBars(
    transactions: List<Transaction>,
    year: Int,
    type: TransactionType = TransactionType.EXPENSE
): List<ChartBar> =
    (1..12).map { m ->
        val ym    = YearMonth.of(year, m)
        val label = ym.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        val total = transactions.filter {
            it.type == type && YearMonth.from(it.date) == ym
        }.sumOf { it.amount }
        ChartBar(label, "$label $year", total, ym.atDay(1), ym.atEndOfMonth())
    }

fun computeWeeklyBars(
    transactions: List<Transaction>,
    year: Int,
    month: Int,
    type: TransactionType = TransactionType.EXPENSE
): List<ChartBar> {
    val abbr = YearMonth.of(year, month).month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    return weekWindows(year, month).map { (label, start, end) ->
        val total = transactions.filter {
            it.type == type && !it.date.isBefore(start) && !it.date.isAfter(end)
        }.sumOf { it.amount }
        ChartBar(label, "$abbr $label $year", total, start, end)
    }
}

fun computeYearlyBars(
    transactions: List<Transaction>,
    today: LocalDate = LocalDate.now(),
    type: TransactionType = TransactionType.EXPENSE
): List<ChartBar> =
    yearRange(transactions, today).map { y ->
        val total = transactions.filter {
            it.type == type && it.date.year == y
        }.sumOf { it.amount }
        ChartBar("$y", "$y", total, LocalDate.of(y, 1, 1), LocalDate.of(y, 12, 31))
    }

// â"€â"€ Category-card period helpers â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

fun catPeriods(
    granularity: String,
    transactions: List<Transaction>,
    contextYear: Int,
    contextMonth: Int,
    today: LocalDate = LocalDate.now()
): List<CatPeriod> = when (granularity) {
    "weekly" -> {
        val abbr = YearMonth.of(contextYear, contextMonth).month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        weekWindows(contextYear, contextMonth).map { (label, start, end) ->
            CatPeriod(label, "$abbr $label $contextYear", start, end)
        }
    }
    "yearly" -> yearRange(transactions, today).map { y ->
        CatPeriod("$y", "$y", LocalDate.of(y, 1, 1), LocalDate.of(y, 12, 31))
    }
    else -> (1..12).map { m ->
        val ym  = YearMonth.of(contextYear, m)
        val lbl = ym.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        CatPeriod(lbl, "$lbl $contextYear", ym.atDay(1), ym.atEndOfMonth())
    }
}

fun defaultCatPeriodIndex(
    granularity: String,
    periods: List<CatPeriod>,
    contextYear: Int,
    contextMonth: Int,
    today: LocalDate = LocalDate.now()
): Int = when (granularity) {
    "yearly"  -> periods.indexOfFirst { it.startDate.year == contextYear }.coerceAtLeast(0)
    "monthly" -> periods.indexOfFirst { it.startDate.monthValue == contextMonth }.coerceAtLeast(0)
    "weekly"  ->
        if (contextYear == today.year && contextMonth == today.monthValue)
            periods.indexOfFirst { !today.isBefore(it.startDate) && !today.isAfter(it.endDate) }
                .coerceAtLeast(0)
        else 0
    else -> 0
}

// â"€â"€ ViewModel helpers â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

fun dailyExpenses(list: List<Transaction>, month: YearMonth, maxDay: Int? = null): List<Pair<Int, Double>> {
    val days  = (maxDay ?: month.lengthOfMonth()).coerceIn(1, month.lengthOfMonth())
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

fun topCats(
    list: List<Transaction>,
    month: YearMonth,
    limit: Int = 5
): List<Pair<String, Double>> =
    list.filter { it.type == TransactionType.EXPENSE && YearMonth.from(it.date) == month }
        .groupBy { it.category }
        .mapValues { (_, v) -> v.sumOf { it.amount } }
        .entries.sortedByDescending { it.value }
        .take(limit)
        .map { it.key to it.value }

fun monthlyExpensesLast6(
    list: List<Transaction>,
    now: YearMonth = YearMonth.now()
): List<Pair<String, Double>> =
    (5 downTo 0).map { offset ->
        val month = now.minusMonths(offset.toLong())
        val label = "${month.month.name.take(3)} ${month.year}"
        label to monthTotal(list, month, TransactionType.EXPENSE)
    }

fun monthlyExpensesForYear(list: List<Transaction>, year: Int): List<ChartBar> =
    computeMonthlyBars(list, year, TransactionType.EXPENSE)

fun weeklyExpensesForMonth(
    list: List<Transaction>,
    year: Int,
    month: Int
): List<ChartBar> = computeWeeklyBars(list, year, month, TransactionType.EXPENSE)

fun yearlyExpenses(
    list: List<Transaction>,
    currentYear: Int = LocalDate.now().year
): List<ChartBar> = computeYearlyBars(list, LocalDate.of(currentYear, 1, 1), TransactionType.EXPENSE)

fun monthTotal(list: List<Transaction>, month: YearMonth, type: TransactionType): Double =
    list.filter { it.type == type && YearMonth.from(it.date) == month }.sumOf { it.amount }

// â"€â"€ Shared date-range and grouping helpers (single source of truth) â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

/** Returns transactions whose date falls within [start]..[end] inclusive. */
fun filterInDateRange(
    transactions: List<Transaction>,
    start: LocalDate,
    end: LocalDate
): List<Transaction> = transactions.filter { !it.date.isBefore(start) && !it.date.isAfter(end) }

/** Returns only EXPENSE transactions in [start]..[end] inclusive. */
fun filterExpensesByDateRange(
    transactions: List<Transaction>,
    start: LocalDate,
    end: LocalDate
): List<Transaction> = filterByTypeAndDateRange(transactions, start, end, TransactionType.EXPENSE)

/** Returns transactions of [type] in [start]..[end] inclusive. */
fun filterByTypeAndDateRange(
    transactions: List<Transaction>,
    start: LocalDate,
    end: LocalDate,
    type: TransactionType
): List<Transaction> = transactions.filter {
    it.type == type && !it.date.isBefore(start) && !it.date.isAfter(end)
}

/** Groups transactions by category and sums amounts, sorted descending by total. */
fun computeCategoryTotals(transactions: List<Transaction>): List<Pair<String, Double>> =
    transactions.groupBy { it.category }
        .mapValues { (_, v) -> v.sumOf { it.amount } }
        .entries.sortedByDescending { it.value }
        .map { it.key to it.value }


// â"€â"€ Budget spent â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

fun budgetSpentAmount(
    transactions: List<Transaction>,
    budget: Budget,
    referenceDate: LocalDate = LocalDate.now()
): Double {
    val weekStart = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val nowYM     = YearMonth.from(referenceDate)
    return transactions.filter { t ->
        t.type == TransactionType.EXPENSE && t.category == budget.categoryName &&
        when (budget.period) {
            BudgetPeriod.ONE_TIME -> t.date.toEpochDay() >= budget.createdEpochDay
            BudgetPeriod.WEEKLY  -> !t.date.isBefore(weekStart) && !t.date.isAfter(referenceDate)
            BudgetPeriod.MONTHLY -> YearMonth.from(t.date) == nowYM
            BudgetPeriod.YEARLY  -> t.date.year == referenceDate.year
        }
    }.sumOf { it.amount }
}
