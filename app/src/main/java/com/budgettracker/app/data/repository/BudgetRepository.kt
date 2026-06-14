package com.pardonmyspending.app.data.repository

import com.pardonmyspending.app.data.db.BudgetDao
import com.pardonmyspending.app.data.db.toBudget
import com.pardonmyspending.app.data.db.toEntity
import com.pardonmyspending.app.data.model.Budget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class BudgetRepository(private val dao: BudgetDao) {

    val budgets: Flow<List<Budget>> = dao.observeAll().map { it.map { e -> e.toBudget() } }

    suspend fun add(budget: Budget) = withContext(Dispatchers.IO) {
        dao.insert(budget.toEntity())
    }

    suspend fun update(budget: Budget) = withContext(Dispatchers.IO) {
        dao.update(budget.toEntity())
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        dao.delete(id)
    }

    suspend fun updateOrder(ordered: List<Budget>) = withContext(Dispatchers.IO) {
        dao.updateAll(ordered.mapIndexed { i, b -> b.copy(sortOrder = i).toEntity() })
    }
}
