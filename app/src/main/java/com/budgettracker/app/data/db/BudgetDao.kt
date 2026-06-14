package com.pardonmyspending.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BudgetEntity)

    @Update
    suspend fun update(entity: BudgetEntity)

    @Update
    suspend fun updateAll(entities: List<BudgetEntity>)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun delete(id: String)
}
