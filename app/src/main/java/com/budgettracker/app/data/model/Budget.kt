package com.pardonmyspending.app.data.model

enum class BudgetPeriod { ONE_TIME, WEEKLY, MONTHLY, YEARLY }

data class Budget(
    val id: String,
    val title: String,
    val categoryName: String,
    val amount: Double,
    val period: BudgetPeriod,
    val createdEpochDay: Long,
    val sortOrder: Int
)
