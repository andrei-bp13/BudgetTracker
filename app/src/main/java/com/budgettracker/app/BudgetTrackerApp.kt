package com.budgettracker.app

import android.app.Application
import com.budgettracker.app.data.csv.CsvManager
import com.budgettracker.app.data.db.AppDatabase
import com.budgettracker.app.data.repository.CategoryRepository
import com.budgettracker.app.data.repository.PreferencesRepository
import com.budgettracker.app.data.repository.RecurringTransactionRepository
import com.budgettracker.app.data.repository.TransactionRepository

class BudgetTrackerApp : Application() {
    val csvManager by lazy { CsvManager(this) }
    val database by lazy { AppDatabase.getInstance(this) }
    val transactionRepository  by lazy { TransactionRepository(csvManager, database.transactionDao()) }
    val recurringRepository    by lazy { RecurringTransactionRepository(database.recurringTransactionDao()) }
    val preferencesRepository  by lazy { PreferencesRepository(this) }
    val categoryRepository     by lazy { CategoryRepository(this) }
}
