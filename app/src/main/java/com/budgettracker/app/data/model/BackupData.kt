package com.pardonmyspending.app.data.model

data class BackupData(
    val version: Int = 1,
    val exportedAt: String = "",
    val transactions: List<Transaction> = emptyList(),
    val recurringTransactions: List<RecurringTransaction> = emptyList(),
    val categories: List<CategoryItem> = emptyList(),
    val budgets: List<Budget> = emptyList()
)
