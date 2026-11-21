package com.pardonmyspending.app

import com.pardonmyspending.app.data.model.Budget
import com.pardonmyspending.app.data.model.BudgetPeriod
import com.pardonmyspending.app.data.model.PaymentType
import com.pardonmyspending.app.data.model.Transaction
import com.pardonmyspending.app.data.model.TransactionType
import com.pardonmyspending.app.util.*
import com.pardonmyspending.app.viewmodel.ChartBar
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BudgetCalculationsTest {

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun expense(amount: Double, dateStr: String, category: String = "Food") =
        Transaction(
            id          = dateStr,
            title       = "Test",
            amount      = amount,
            type        = TransactionType.EXPENSE,
            category    = category,
            date        = LocalDate.parse(dateStr),
            paymentType = PaymentType.DEBIT
        )

    private fun income(amount: Double, dateStr: String) =
        Transaction(
            id          = dateStr + "i",
            title       = "Income",
            amount      = amount,
            type        = TransactionType.INCOME,
            category    = "Salary",
            date        = LocalDate.parse(dateStr),
            paymentType = PaymentType.DEBIT
        )

    // ── monthTotal ────────────────────────────────────────────────────────────

    @Test fun `monthTotal sums only EXPENSE for the given month`() {
        val txns = listOf(
            expense(100.0, "2025-01-05"),
            expense(50.0,  "2025-01-20"),
            income(200.0,  "2025-01-10"),
            expense(30.0,  "2025-02-01"),
        )
        val total = monthTotal(txns, YearMonth.of(2025, 1), TransactionType.EXPENSE)
        assertEquals(150.0, total)
    }

    @Test fun `monthTotal returns 0 for empty month`() {
        val total = monthTotal(emptyList(), YearMonth.of(2025, 1), TransactionType.EXPENSE)
        assertEquals(0.0, total)
    }

    @Test fun `monthTotal can sum INCOME`() {
        val txns = listOf(income(500.0, "2025-03-10"), expense(100.0, "2025-03-10"))
        val total = monthTotal(txns, YearMonth.of(2025, 3), TransactionType.INCOME)
        assertEquals(500.0, total)
    }

    // ── computeMonthlyBars ────────────────────────────────────────────────────

    @Test fun `computeMonthlyBars produces 12 bars`() {
        val bars = computeMonthlyBars(emptyList(), 2025)
        assertEquals(12, bars.size)
    }

    @Test fun `computeMonthlyBars start and end dates match the year`() {
        val bars = computeMonthlyBars(emptyList(), 2025)
        assertEquals(LocalDate.of(2025, 1, 1),  bars.first().startDate)
        assertEquals(LocalDate.of(2025, 12, 31), bars.last().endDate)
    }

    @Test fun `computeMonthlyBars sums expenses per month`() {
        val txns = listOf(
            expense(100.0, "2025-03-15"),
            expense(200.0, "2025-03-28"),
            expense(50.0,  "2025-04-01"),
            income(999.0,  "2025-03-10"),
        )
        val bars = computeMonthlyBars(txns, 2025)
        assertEquals(300.0, bars[2].value) // March = index 2
        assertEquals(50.0,  bars[3].value) // April = index 3
        assertEquals(0.0,   bars[0].value) // January = 0
    }

    @Test fun `computeMonthlyBars excludes other years`() {
        val txns = listOf(
            expense(100.0, "2024-06-01"),
            expense(200.0, "2025-06-01"),
        )
        val bars = computeMonthlyBars(txns, 2025)
        assertEquals(200.0, bars[5].value) // June 2025 only
    }

    // ── computeWeeklyBars ─────────────────────────────────────────────────────

    @Test fun `computeWeeklyBars covers entire month with no gaps`() {
        // February 2025 has 28 days: weeks 1-7, 8-14, 15-21, 22-28 → 4 bars
        val bars = computeWeeklyBars(emptyList(), 2025, 2)
        assertEquals(4, bars.size)
        assertEquals(LocalDate.of(2025, 2, 1),  bars.first().startDate)
        assertEquals(LocalDate.of(2025, 2, 28), bars.last().endDate)
    }

    @Test fun `computeWeeklyBars last bar ends on last day of month`() {
        // January has 31 days: 1-7, 8-14, 15-21, 22-28, 29-31 → 5 bars
        val bars = computeWeeklyBars(emptyList(), 2025, 1)
        assertEquals(5, bars.size)
        assertEquals(LocalDate.of(2025, 1, 29), bars.last().startDate)
        assertEquals(LocalDate.of(2025, 1, 31), bars.last().endDate)
    }

    @Test fun `computeWeeklyBars sums expenses in each window`() {
        val txns = listOf(
            expense(10.0, "2025-02-01"), // week 1
            expense(20.0, "2025-02-07"), // week 1
            expense(30.0, "2025-02-08"), // week 2
        )
        val bars = computeWeeklyBars(txns, 2025, 2)
        assertEquals(30.0, bars[0].value)
        assertEquals(30.0, bars[1].value)
    }

    @Test fun `computeWeeklyBars excludes income`() {
        val txns = listOf(income(500.0, "2025-02-05"), expense(50.0, "2025-02-05"))
        val bars = computeWeeklyBars(txns, 2025, 2)
        assertEquals(50.0, bars[0].value)
    }

    // ── computeYearlyBars ─────────────────────────────────────────────────────

    @Test fun `computeYearlyBars always includes today year even with no data`() {
        val today = LocalDate.of(2025, 6, 1)
        val bars  = computeYearlyBars(emptyList(), today)
        assertEquals(1, bars.size)
        assertEquals("2025", bars[0].label)
    }

    @Test fun `computeYearlyBars spans from earliest year to today`() {
        val today = LocalDate.of(2025, 6, 1)
        val txns  = listOf(expense(100.0, "2022-01-01"), expense(200.0, "2024-06-01"))
        val bars  = computeYearlyBars(txns, today)
        assertEquals(4, bars.size) // 2022, 2023, 2024, 2025
        assertEquals("2022", bars.first().label)
        assertEquals("2025", bars.last().label)
    }

    @Test fun `computeYearlyBars sums expenses per year`() {
        val today = LocalDate.of(2025, 1, 1)
        val txns  = listOf(
            expense(100.0, "2024-03-01"),
            expense(200.0, "2024-09-15"),
            expense(50.0,  "2025-01-01"),
        )
        val bars = computeYearlyBars(txns, today)
        val bar2024 = bars.first { it.label == "2024" }
        val bar2025 = bars.first { it.label == "2025" }
        assertEquals(300.0, bar2024.value)
        assertEquals(50.0,  bar2025.value)
    }

    // ── dailyExpenses ─────────────────────────────────────────────────────────

    @Test fun `dailyExpenses returns a list with one entry per day of month`() {
        val result = dailyExpenses(emptyList(), YearMonth.of(2025, 2))
        assertEquals(28, result.size)
        assertEquals(1, result.first().first)
        assertEquals(28, result.last().first)
    }

    @Test fun `dailyExpenses is cumulative`() {
        val txns = listOf(expense(100.0, "2025-01-01"), expense(50.0, "2025-01-03"))
        val result = dailyExpenses(txns, YearMonth.of(2025, 1))
        assertEquals(100.0, result[0].second) // day 1
        assertEquals(100.0, result[1].second) // day 2 (no change)
        assertEquals(150.0, result[2].second) // day 3
        assertEquals(150.0, result.last().second) // rest of month
    }

    @Test fun `dailyExpenses excludes income`() {
        val txns = listOf(income(1000.0, "2025-01-10"), expense(40.0, "2025-01-10"))
        val result = dailyExpenses(txns, YearMonth.of(2025, 1))
        assertEquals(40.0, result[9].second)
    }

    // ── topCats ───────────────────────────────────────────────────────────────

    @Test fun `topCats returns categories sorted by descending amount`() {
        val txns = listOf(
            expense(10.0, "2025-05-01", "Food"),
            expense(50.0, "2025-05-02", "Shopping"),
            expense(30.0, "2025-05-03", "Transport"),
        )
        val result = topCats(txns, YearMonth.of(2025, 5))
        assertEquals("Shopping", result[0].first)
        assertEquals("Transport", result[1].first)
        assertEquals("Food", result[2].first)
    }

    @Test fun `topCats respects limit`() {
        val txns = (1..10).map { expense(it.toDouble(), "2025-05-0${it % 9 + 1}", "Cat$it") }
        val result = topCats(txns, YearMonth.of(2025, 5), limit = 3)
        assertEquals(3, result.size)
    }

    @Test fun `topCats ignores other months`() {
        val txns = listOf(
            expense(100.0, "2025-04-30", "Food"),
            expense(10.0,  "2025-05-01", "Food"),
        )
        val result = topCats(txns, YearMonth.of(2025, 5))
        assertEquals(1, result.size)
        assertEquals(10.0, result[0].second)
    }

    // ── monthlyExpensesLast6 ──────────────────────────────────────────────────

    @Test fun `monthlyExpensesLast6 returns exactly 6 entries`() {
        val result = monthlyExpensesLast6(emptyList(), YearMonth.of(2025, 6))
        assertEquals(6, result.size)
    }

    @Test fun `monthlyExpensesLast6 last entry is the current month`() {
        val now = YearMonth.of(2025, 6)
        val result = monthlyExpensesLast6(emptyList(), now)
        assertTrue(result.last().first.startsWith("JUN"))
    }

    @Test fun `monthlyExpensesLast6 sums correctly`() {
        val txns = listOf(expense(123.0, "2025-06-15"))
        val result = monthlyExpensesLast6(txns, YearMonth.of(2025, 6))
        assertEquals(123.0, result.last().second)
        assertEquals(0.0, result.first().second) // 5 months ago
    }

    // ── monthlyExpensesForYear ────────────────────────────────────────────────

    @Test fun `monthlyExpensesForYear returns 12 bars`() {
        val bars = monthlyExpensesForYear(emptyList(), 2025)
        assertEquals(12, bars.size)
    }

    @Test fun `monthlyExpensesForYear bar date ranges are correct`() {
        val bars = monthlyExpensesForYear(emptyList(), 2025)
        assertEquals(LocalDate.of(2025, 1, 1),  bars.first().startDate)
        assertEquals(LocalDate.of(2025, 12, 31), bars.last().endDate)
    }

    // ── weeklyExpensesForMonth ────────────────────────────────────────────────

    @Test fun `weeklyExpensesForMonth covers full month`() {
        val bars = weeklyExpensesForMonth(emptyList(), 2025, 3) // March = 31 days
        assertEquals(5, bars.size) // 1-7, 8-14, 15-21, 22-28, 29-31
        assertEquals(LocalDate.of(2025, 3, 1),  bars.first().startDate)
        assertEquals(LocalDate.of(2025, 3, 31), bars.last().endDate)
    }

    // ── yearlyExpenses ────────────────────────────────────────────────────────

    @Test fun `yearlyExpenses with empty list returns single bar for current year`() {
        val bars = yearlyExpenses(emptyList(), 2025)
        assertEquals(1, bars.size)
        assertEquals("2025", bars[0].label)
    }

    @Test fun `yearlyExpenses spans from first transaction year to currentYear`() {
        val txns = listOf(expense(1.0, "2021-06-01"))
        val bars = yearlyExpenses(txns, 2025)
        assertEquals(5, bars.size) // 2021..2025
    }

    // ── catPeriods ────────────────────────────────────────────────────────────

    @Test fun `catPeriods monthly returns 12 periods for the context year`() {
        val periods = catPeriods("monthly", emptyList(), 2025, 6)
        assertEquals(12, periods.size)
        assertTrue(periods.all { it.startDate.year == 2025 })
    }

    @Test fun `catPeriods weekly covers full month with no overlap`() {
        val periods = catPeriods("weekly", emptyList(), 2025, 2) // Feb 2025 = 28 days
        assertEquals(4, periods.size)
        assertEquals(LocalDate.of(2025, 2, 1),  periods.first().startDate)
        assertEquals(LocalDate.of(2025, 2, 28), periods.last().endDate)
    }

    @Test fun `catPeriods yearly includes today year`() {
        val today   = LocalDate.of(2025, 6, 1)
        val periods = catPeriods("yearly", emptyList(), 2025, 6, today)
        assertEquals(1, periods.size)
        assertEquals("2025", periods[0].label)
    }

    @Test fun `catPeriods yearly includes all years in transactions`() {
        val today = LocalDate.of(2025, 6, 1)
        val txns  = listOf(expense(1.0, "2022-01-01"), expense(1.0, "2023-06-15"))
        val periods = catPeriods("yearly", txns, 2025, 6, today)
        assertEquals(4, periods.size) // 2022, 2023, 2024, 2025
    }

    // ── defaultCatPeriodIndex ─────────────────────────────────────────────────

    @Test fun `defaultCatPeriodIndex monthly returns index matching contextMonth`() {
        val periods = catPeriods("monthly", emptyList(), 2025, 6)
        val idx = defaultCatPeriodIndex("monthly", periods, 2025, 6)
        assertEquals(5, idx) // June = index 5
    }

    @Test fun `defaultCatPeriodIndex yearly returns index matching contextYear`() {
        val today   = LocalDate.of(2025, 6, 1)
        val txns    = listOf(expense(1.0, "2023-01-01"))
        val periods = catPeriods("yearly", txns, 2025, 6, today)
        val idx     = defaultCatPeriodIndex("yearly", periods, 2025, 6)
        assertEquals(periods.indexOfFirst { it.startDate.year == 2025 }, idx)
    }

    @Test fun `defaultCatPeriodIndex weekly for non-current month returns 0`() {
        val today   = LocalDate.of(2025, 6, 15)
        val periods = catPeriods("weekly", emptyList(), 2025, 3, today) // March, not current
        val idx     = defaultCatPeriodIndex("weekly", periods, 2025, 3, today)
        assertEquals(0, idx)
    }

    @Test fun `defaultCatPeriodIndex weekly for current month finds today's window`() {
        val today   = LocalDate.of(2025, 6, 12) // falls in week 8-14
        val periods = catPeriods("weekly", emptyList(), 2025, 6, today)
        val idx     = defaultCatPeriodIndex("weekly", periods, 2025, 6, today)
        val period  = periods[idx]
        assertTrue(!today.isBefore(period.startDate) && !today.isAfter(period.endDate))
    }

    @Test fun `defaultCatPeriodIndex unknown granularity returns 0`() {
        val idx = defaultCatPeriodIndex("unknown", emptyList(), 2025, 6)
        assertEquals(0, idx)
    }

    // ── dailyExpenses with maxDay ─────────────────────────────────────────────

    @Test fun `dailyExpenses with maxDay clips to that day`() {
        val result = dailyExpenses(emptyList(), YearMonth.of(2025, 1), maxDay = 10)
        assertEquals(10, result.size)
        assertEquals(10, result.last().first)
    }

    @Test fun `dailyExpenses maxDay coerces to month length when too large`() {
        val result = dailyExpenses(emptyList(), YearMonth.of(2025, 2), maxDay = 99)
        assertEquals(28, result.size)
    }

    @Test fun `dailyExpenses maxDay 1 returns single entry`() {
        val txns   = listOf(expense(42.0, "2025-03-01"))
        val result = dailyExpenses(txns, YearMonth.of(2025, 3), maxDay = 1)
        assertEquals(1, result.size)
        assertEquals(42.0, result[0].second)
    }

    // ── budgetSpentAmount ─────────────────────────────────────────────────────

    private fun budget(
        period: BudgetPeriod,
        category: String = "Food",
        createdDate: String = "2025-01-01"
    ) = Budget(
        id              = "b1",
        title           = "Test",
        categoryName    = category,
        amount          = 500.0,
        period          = period,
        createdEpochDay = LocalDate.parse(createdDate).toEpochDay(),
        sortOrder       = 0
    )

    @Test fun `budgetSpentAmount MONTHLY sums only current month expenses`() {
        val ref  = LocalDate.of(2025, 6, 15)
        val txns = listOf(
            expense(100.0, "2025-06-01"),
            expense(50.0,  "2025-06-20"),
            expense(200.0, "2025-05-31"), // previous month — excluded
            income(999.0,  "2025-06-10"), // income — excluded
        )
        val result = budgetSpentAmount(txns, budget(BudgetPeriod.MONTHLY), ref)
        assertEquals(150.0, result)
    }

    @Test fun `budgetSpentAmount WEEKLY sums only current week Mon–today`() {
        // ref = Wednesday 2025-06-11 → week Mon 2025-06-09 .. Wed 2025-06-11
        val ref  = LocalDate.of(2025, 6, 11)
        val txns = listOf(
            expense(10.0, "2025-06-09"), // Monday — in
            expense(20.0, "2025-06-11"), // Wednesday — in
            expense(30.0, "2025-06-08"), // Sunday before — excluded
            expense(40.0, "2025-06-12"), // Thursday after ref — excluded
        )
        val result = budgetSpentAmount(txns, budget(BudgetPeriod.WEEKLY), ref)
        assertEquals(30.0, result)
    }

    @Test fun `budgetSpentAmount YEARLY sums only current year expenses`() {
        val ref  = LocalDate.of(2025, 6, 1)
        val txns = listOf(
            expense(100.0, "2025-01-01"),
            expense(200.0, "2025-12-31"),
            expense(999.0, "2024-12-31"), // previous year — excluded
        )
        val result = budgetSpentAmount(txns, budget(BudgetPeriod.YEARLY), ref)
        assertEquals(300.0, result)
    }

    @Test fun `budgetSpentAmount ONE_TIME sums all expenses since creation`() {
        val created = "2025-03-01"
        val ref     = LocalDate.of(2025, 6, 1)
        val txns    = listOf(
            expense(100.0, "2025-03-01"), // on creation day — in
            expense(200.0, "2025-05-15"), // after — in
            expense(50.0,  "2025-02-28"), // before creation — excluded
        )
        val result = budgetSpentAmount(txns, budget(BudgetPeriod.ONE_TIME, createdDate = created), ref)
        assertEquals(300.0, result)
    }

    @Test fun `budgetSpentAmount ignores different category`() {
        val ref  = LocalDate.of(2025, 6, 1)
        val txns = listOf(
            expense(100.0, "2025-06-01", "Food"),
            expense(999.0, "2025-06-01", "Shopping"),
        )
        val result = budgetSpentAmount(txns, budget(BudgetPeriod.MONTHLY, category = "Food"), ref)
        assertEquals(100.0, result)
    }

    @Test fun `budgetSpentAmount returns 0 for empty transaction list`() {
        val result = budgetSpentAmount(emptyList(), budget(BudgetPeriod.MONTHLY))
        assertEquals(0.0, result)
    }

    @Test fun `budgetSpentAmount picks up WEEKLY and YEARLY budgets too, not just MONTHLY`() {
        // Regression test: WidgetDataRepository.budgetProgress() used to only handle
        // MONTHLY budgets by reimplementing this filter instead of calling budgetSpentAmount.
        val ref = LocalDate.of(2025, 6, 1)
        val txns = listOf(expense(100.0, "2025-06-01"), expense(999.0, "2024-01-01"))
        val weeklyResult = budgetSpentAmount(txns, budget(BudgetPeriod.WEEKLY), ref)
        val yearlyResult = budgetSpentAmount(txns, budget(BudgetPeriod.YEARLY), ref)
        assertEquals(100.0, weeklyResult)
        assertEquals(100.0, yearlyResult)
    }

    // ── filterInDateRange ─────────────────────────────────────────────────────

    @Test fun `filterInDateRange includes both income and expense within range`() {
        val txns = listOf(
            expense(10.0, "2025-01-05"),
            income(20.0,  "2025-01-10"),
            expense(30.0, "2025-02-01"), // outside range
        )
        val result = filterInDateRange(txns, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))
        assertEquals(2, result.size)
    }

    @Test fun `filterInDateRange is inclusive of boundary dates`() {
        val txns = listOf(expense(10.0, "2025-01-01"), expense(20.0, "2025-01-31"))
        val result = filterInDateRange(txns, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))
        assertEquals(2, result.size)
    }

    @Test fun `filterInDateRange excludes dates outside range`() {
        val txns = listOf(expense(10.0, "2024-12-31"), expense(20.0, "2025-02-01"))
        val result = filterInDateRange(txns, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))
        assertTrue(result.isEmpty())
    }

    // ── filterByTypeAndDateRange ──────────────────────────────────────────────

    @Test fun `filterByTypeAndDateRange filters by type and date together`() {
        val txns = listOf(
            expense(10.0, "2025-01-05"),
            income(20.0,  "2025-01-05"),
        )
        val result = filterByTypeAndDateRange(
            txns, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31), TransactionType.INCOME
        )
        assertEquals(1, result.size)
        assertEquals(20.0, result[0].amount)
    }

    @Test fun `filterByTypeAndDateRange returns empty when no match in range`() {
        val txns = listOf(income(20.0, "2025-01-05"))
        val result = filterByTypeAndDateRange(
            txns, LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 28), TransactionType.INCOME
        )
        assertTrue(result.isEmpty())
    }

    // ── filterExpensesByDateRange ─────────────────────────────────────────────

    @Test fun `filterExpensesByDateRange excludes income in the same range`() {
        val txns = listOf(
            expense(10.0, "2025-01-05"),
            income(999.0, "2025-01-05"),
        )
        val result = filterExpensesByDateRange(txns, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))
        assertEquals(1, result.size)
        assertEquals(10.0, result[0].amount)
    }

    @Test fun `filterExpensesByDateRange matches filterByTypeAndDateRange with EXPENSE`() {
        val txns = listOf(expense(10.0, "2025-01-05"), expense(20.0, "2025-01-20"), income(30.0, "2025-01-10"))
        val start = LocalDate.of(2025, 1, 1)
        val end   = LocalDate.of(2025, 1, 31)
        assertEquals(
            filterByTypeAndDateRange(txns, start, end, TransactionType.EXPENSE),
            filterExpensesByDateRange(txns, start, end)
        )
    }

    // ── computeCategoryTotals ─────────────────────────────────────────────────

    @Test fun `computeCategoryTotals groups and sums by category`() {
        val txns = listOf(
            expense(10.0, "2025-01-01", "Food"),
            expense(5.0,  "2025-01-02", "Food"),
            expense(20.0, "2025-01-03", "Shopping"),
        )
        val result = computeCategoryTotals(txns)
        assertEquals(2, result.size)
        assertEquals("Shopping" to 20.0, result[0])
        assertEquals("Food" to 15.0, result[1])
    }

    @Test fun `computeCategoryTotals sorts descending by total`() {
        val txns = listOf(
            expense(1.0, "2025-01-01", "A"),
            expense(5.0, "2025-01-01", "B"),
            expense(3.0, "2025-01-01", "C"),
        )
        val result = computeCategoryTotals(txns)
        assertEquals(listOf("B", "C", "A"), result.map { it.first })
    }

    @Test fun `computeCategoryTotals returns empty list for no transactions`() {
        assertTrue(computeCategoryTotals(emptyList()).isEmpty())
    }

    // ── delegation sanity: monthly-month-summary wrappers vs computeXBars ────

    @Test fun `monthlyExpensesForYear matches computeMonthlyBars with EXPENSE type`() {
        val txns = listOf(expense(100.0, "2025-03-15"), income(999.0, "2025-03-10"))
        assertEquals(computeMonthlyBars(txns, 2025, TransactionType.EXPENSE), monthlyExpensesForYear(txns, 2025))
    }

    @Test fun `weeklyExpensesForMonth matches computeWeeklyBars with EXPENSE type`() {
        val txns = listOf(expense(50.0, "2025-02-03"))
        assertEquals(
            computeWeeklyBars(txns, 2025, 2, TransactionType.EXPENSE),
            weeklyExpensesForMonth(txns, 2025, 2)
        )
    }

    @Test fun `yearlyExpenses matches computeYearlyBars with EXPENSE type`() {
        val txns = listOf(expense(50.0, "2023-02-03"))
        assertEquals(
            computeYearlyBars(txns, LocalDate.of(2025, 1, 1), TransactionType.EXPENSE),
            yearlyExpenses(txns, 2025)
        )
    }
}
