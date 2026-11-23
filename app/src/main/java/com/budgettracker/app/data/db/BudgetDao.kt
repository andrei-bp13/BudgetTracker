package com.pardonmyspending.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets ORDER BY sortOrder ASC LIMIT 1")
    suspend fun getFirst(): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BudgetEntity)

    @Update
    suspend fun update(entity: BudgetEntity)

    @Update
    suspend fun updateAll(entities: List<BudgetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<BudgetEntity>)

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(entities: List<BudgetEntity>) {
        deleteAll()
        insertAll(entities)
    }

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun delete(id: String)
}
