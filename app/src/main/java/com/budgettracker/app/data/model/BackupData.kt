package com.pardonmyspending.app.data.model

data class BackupData(
    val version: Int = 2,
    val exportedAt: String = "",
    val transactions: List<Transaction> = emptyList(),
    val recurringTransactions: List<RecurringTransaction> = emptyList(),
    val categories: List<CategoryItem> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    // Present when hasSettings = true; defaults match app defaults so old backups are safe
    val hasSettings: Boolean = false,
    val themeMode: String = "system",
    val accentIndex: Int = 0,
    val customAccentHex: String = "2196F3",
    val globalColoring: Boolean = false,
    val granularColoring: Boolean = true,
    val currencyCode: String = "USD",
    val chartGranularity: String = "monthly",
    val aiMode: Boolean = false,
    val cardOrder: List<String> = emptyList(),
    val cardVisible: Map<String, Boolean> = emptyMap()
)
