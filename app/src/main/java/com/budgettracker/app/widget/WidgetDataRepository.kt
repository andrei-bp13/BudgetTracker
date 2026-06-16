package com.pardonmyspending.app.widget

import android.content.Context
import com.pardonmyspending.app.data.db.AppDatabase
import com.pardonmyspending.app.data.db.toBudget
import com.pardonmyspending.app.data.db.toTransaction
import com.pardonmyspending.app.data.model.AllCurrencies
import com.pardonmyspending.app.data.model.Budget
import com.pardonmyspending.app.data.model.BudgetPeriod
import com.pardonmyspending.app.data.model.PaymentType
import com.pardonmyspending.app.data.model.Transaction
import com.pardonmyspending.app.data.model.TransactionType
import com.pardonmyspending.app.data.repository.PreferencesRepository
import com.pardonmyspending.app.util.monthTotal
import kotlinx.coroutines.flow.first
import java.time.YearMonth

data class PaymentTypeRow(val label: String, val expense: Double, val income: Double)

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

    suspend fun totalSavings(): Double {
        val savingsCategories = setOf("Savings", "Investment")
        return transactions().filter { it.category in savingsCategories }.sumOf { it.amount }
    }

    suspend fun highestExpense(): Transaction? =
        transactions().filter { it.type == TransactionType.EXPENSE }.maxByOrNull { it.amount }

    suspend fun highestIncome(): Transaction? =
        transactions().filter { it.type == TransactionType.INCOME }.maxByOrNull { it.amount }

    suspend fun budgetProgress(): Triple<Double, Double, String>? {
        val budget = budgets().firstOrNull { it.period == BudgetPeriod.MONTHLY } ?: return null
        val spent  = transactions()
            .filter { it.type == TransactionType.EXPENSE && it.category == budget.categoryName }
            .filter { YearMonth.from(it.date) == YearMonth.now() }
            .sumOf { it.amount }
        return Triple(spent, budget.amount, budget.title)
    }

    // Returns (totalSpent, totalEarned, perPaymentType) for the current month.
    suspend fun monthSummary(): Triple<Double, Double, List<PaymentTypeRow>> {
        val now  = YearMonth.now()
        val txns = transactions().filter { YearMonth.from(it.date) == now }
        val totalSpent  = txns.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val totalEarned = txns.filter { it.type == TransactionType.INCOME  }.sumOf { it.amount }
        val rows = PaymentType.values().map { pt ->
            val exp = txns.filter { it.type == TransactionType.EXPENSE && it.paymentType == pt }.sumOf { it.amount }
            val inc = txns.filter { it.type == TransactionType.INCOME  && it.paymentType == pt }.sumOf { it.amount }
            PaymentTypeRow(
                label   = pt.name.lowercase().replaceFirstChar { it.titlecase() },
                expense = exp,
                income  = inc
            )
        }
        return Triple(totalSpent, totalEarned, rows)
    }
}
