package com.pardonmyspending.app.util

import com.pardonmyspending.app.data.model.Transaction
import com.pardonmyspending.app.data.model.TransactionType
import com.pardonmyspending.app.viewmodel.ChartBar
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

// ── Shared data class for category-period chips ───────────────────────────────

data class CatPeriod(
    val label: String,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate
)

// ── Bar-chart helpers (context-aware, fully injectable for tests) ──────────────

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
    val ym   = YearMonth.of(year, month)
    val dim  = ym.lengthOfMonth()
    val abbr = ym.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    return buildList {
        var day = 1
        while (day <= dim) {
            val endDay = minOf(day + 6, dim)
            val start  = ym.atDay(day)
            val end    = ym.atDay(endDay)
            val lbl    = if (day == endDay) "$day" else "$day–$endDay"
            val total  = transactions.filter {
                it.type == type && !it.date.isBefore(start) && !it.date.isAfter(end)
            }.sumOf { it.amount }
            add(ChartBar(lbl, "$abbr $lbl $year", total, start, end))
            day += 7
        }
    }
}

fun computeYearlyBars(
    transactions: List<Transaction>,
    today: LocalDate = LocalDate.now(),
    type: TransactionType = TransactionType.EXPENSE
): List<ChartBar> {
    val firstYear = transactions.minOfOrNull { it.date.year } ?: today.year
    return (firstYear..today.year).map { y ->
        val total = transactions.filter {
            it.type == type && it.date.year == y
        }.sumOf { it.amount }
        ChartBar("$y", "$y", total, LocalDate.of(y, 1, 1), LocalDate.of(y, 12, 31))
    }
}

// ── Category-card period helpers ──────────────────────────────────────────────

fun catPeriods(
    granularity: String,
    transactions: List<Transaction>,
    contextYear: Int,
    contextMonth: Int,
    today: LocalDate = LocalDate.now()
): List<CatPeriod> = when (granularity) {
    "weekly" -> {
        val ym   = YearMonth.of(contextYear, contextMonth)
        val dim  = ym.lengthOfMonth()
        val abbr = ym.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        buildList {
            var day = 1
            while (day <= dim) {
                val endDay = minOf(day + 6, dim)
                val lbl = if (day == endDay) "$day" else "$day–$endDay"
                add(CatPeriod(lbl, "$abbr $lbl $contextYear", ym.atDay(day), ym.atDay(endDay)))
                day += 7
            }
        }
    }
    "yearly" -> {
        val firstYear = transactions.minOfOrNull { it.date.year } ?: today.year
        (firstYear..today.year).map { y ->
            CatPeriod("$y", "$y", LocalDate.of(y, 1, 1), LocalDate.of(y, 12, 31))
        }
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

// ── ViewModel helpers ─────────────────────────────────────────────────────────

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
    (1..12).map { m ->
        val ym    = YearMonth.of(year, m)
        val start = ym.atDay(1)
        val end   = ym.atEndOfMonth()
        val abbr  = ym.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        val full  = ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        ChartBar(
            label     = abbr,
            title     = "$full $year",
            value     = monthTotal(list, ym, TransactionType.EXPENSE),
            startDate = start,
            endDate   = end
        )
    }

fun weeklyExpensesForMonth(
    list: List<Transaction>,
    year: Int,
    month: Int
): List<ChartBar> {
    val ym          = YearMonth.of(year, month)
    val daysInMonth = ym.lengthOfMonth()
    val monthAbbr   = ym.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    return buildList {
        var day = 1
        while (day <= daysInMonth) {
            val endDay = minOf(day + 6, daysInMonth)
            val start  = ym.atDay(day)
            val end    = ym.atDay(endDay)
            val label  = if (day == endDay) "$day" else "$day-$endDay"
            val total  = list.filter {
                it.type == TransactionType.EXPENSE &&
                !it.date.isBefore(start) && !it.date.isAfter(end)
            }.sumOf { it.amount }
            add(ChartBar(label, "$monthAbbr $label", total, start, end))
            day += 7
        }
    }
}

fun yearlyExpenses(
    list: List<Transaction>,
    currentYear: Int = LocalDate.now().year
): List<ChartBar> {
    val firstYear = list.minOfOrNull { it.date.year } ?: currentYear
    return (firstYear..currentYear).map { y ->
        val total = list.filter { it.type == TransactionType.EXPENSE && it.date.year == y }
                        .sumOf { it.amount }
        ChartBar("$y", "$y", total, LocalDate.of(y, 1, 1), LocalDate.of(y, 12, 31))
    }
}

fun monthTotal(list: List<Transaction>, month: YearMonth, type: TransactionType): Double =
    list.filter { it.type == type && YearMonth.from(it.date) == month }.sumOf { it.amount }

// ── Shared date-range and grouping helpers (single source of truth) ───────────

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
): List<Transaction> = transactions.filter {
    it.type == TransactionType.EXPENSE && !it.date.isBefore(start) && !it.date.isAfter(end)
}

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
