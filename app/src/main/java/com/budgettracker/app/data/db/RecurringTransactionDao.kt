package com.budgettracker.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {

    @Query("SELECT * FROM recurring_transactions ORDER BY nextDueDateEpochDay ASC")
    fun observeAll(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions ORDER BY nextDueDateEpochDay ASC")
    suspend fun getAll(): List<RecurringTransactionEntity>

    @Query("SELECT * FROM recurring_transactions WHERE status = 'ACTIVE' AND nextDueDateEpochDay <= :todayEpochDay")
    suspend fun getDue(todayEpochDay: Long): List<RecurringTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RecurringTransactionEntity)

    @Update
    suspend fun update(entity: RecurringTransactionEntity)

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteById(id: String)
}
