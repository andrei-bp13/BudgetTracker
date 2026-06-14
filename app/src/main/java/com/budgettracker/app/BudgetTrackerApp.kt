package com.pardonmyspending.app

import android.app.Application
import com.pardonmyspending.app.data.csv.CsvManager
import com.pardonmyspending.app.data.db.AppDatabase
import com.pardonmyspending.app.data.repository.CategoryRepository
import com.pardonmyspending.app.data.repository.PreferencesRepository
import com.pardonmyspending.app.data.repository.RecurringTransactionRepository
import com.pardonmyspending.app.data.repository.TransactionRepository

class BudgetTrackerApp : Application() {
    val csvManager by lazy { CsvManager(this) }
    val database by lazy { AppDatabase.getInstance(this) }
    val transactionRepository  by lazy { TransactionRepository(csvManager, database.transactionDao()) }
    val recurringRepository    by lazy { RecurringTransactionRepository(database.recurringTransactionDao()) }
    val preferencesRepository  by lazy { PreferencesRepository(this) }
    val categoryRepository     by lazy { CategoryRepository(this) }
}
