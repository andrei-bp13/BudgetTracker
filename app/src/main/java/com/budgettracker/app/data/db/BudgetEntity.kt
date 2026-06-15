package com.pardonmyspending.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pardonmyspending.app.data.model.Budget
import com.pardonmyspending.app.data.model.BudgetPeriod

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
    val title: String,
    val categoryName: String,
    val amount: Double,
    val period: String,
    val createdEpochDay: Long,
    val sortOrder: Int
)

fun BudgetEntity.toBudget() = Budget(
    id              = id,
    title           = title,
    categoryName    = categoryName,
    amount          = amount,
    period          = BudgetPeriod.valueOf(period),
    createdEpochDay = createdEpochDay,
    sortOrder       = sortOrder
)

fun Budget.toEntity() = BudgetEntity(
    id              = id,
    title           = title,
    categoryName    = categoryName,
    amount          = amount,
    period          = period.name,
    createdEpochDay = createdEpochDay,
    sortOrder       = sortOrder
)
