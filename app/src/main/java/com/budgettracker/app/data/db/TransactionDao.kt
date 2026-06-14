package com.pardonmyspending.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY dateEpochDay DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<TransactionEntity>)

    @Update
    suspend fun update(entity: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(entities: List<TransactionEntity>) {
        deleteAll()
        insertAll(entities)
    }

    @Query("SELECT * FROM transactions ORDER BY dateEpochDay DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getCount(): Int
}
