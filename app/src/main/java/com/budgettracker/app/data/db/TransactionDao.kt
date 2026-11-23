package com.pardonmyspending.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class PaymentTypeTotalRow(
    val type: String,
    val paymentType: String,
    val total: Double
)

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

    // ── Targeted aggregate queries for widgets ────────────────────────────────

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = :type AND dateEpochDay BETWEEN :startEpochDay AND :endEpochDay")
    suspend fun sumByTypeAndDateRange(type: String, startEpochDay: Long, endEpochDay: Long): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = :type AND category = :category AND dateEpochDay BETWEEN :startEpochDay AND :endEpochDay")
    suspend fun sumByCategoryAndDateRange(type: String, category: String, startEpochDay: Long, endEpochDay: Long): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = :type AND category = :category AND dateEpochDay >= :startEpochDay")
    suspend fun sumByCategoryFromDate(type: String, category: String, startEpochDay: Long): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE category IN (:categories)")
    suspend fun sumByCategories(categories: List<String>): Double

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY amount DESC LIMIT 1")
    suspend fun getHighestByType(type: String): TransactionEntity?

    @Query("SELECT DISTINCT dateEpochDay FROM transactions WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay")
    suspend fun getDistinctDateEpochDaysInRange(startEpochDay: Long, endEpochDay: Long): List<Long>

    @Query("SELECT type, paymentType, COALESCE(SUM(amount), 0.0) AS total FROM transactions WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay GROUP BY type, paymentType")
    suspend fun getPaymentTypeTotalsInRange(startEpochDay: Long, endEpochDay: Long): List<PaymentTypeTotalRow>
}
