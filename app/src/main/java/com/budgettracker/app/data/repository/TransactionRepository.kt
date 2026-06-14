package com.pardonmyspending.app.data.repository

import android.net.Uri
import com.pardonmyspending.app.data.csv.CsvManager
import com.pardonmyspending.app.data.db.TransactionDao
import com.pardonmyspending.app.data.db.toEntity
import com.pardonmyspending.app.data.db.toTransaction
import com.pardonmyspending.app.data.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TransactionRepository(
    private val csvManager: CsvManager,
    private val dao: TransactionDao
) {
    // Room drives updates reactively — no manual in-memory mirror needed.
    val transactions: Flow<List<Transaction>> = dao.observeAll()
        .map { entities -> entities.map { it.toTransaction() } }

    suspend fun add(transaction: Transaction) = withContext(Dispatchers.IO) {
        dao.insert(transaction.toEntity())
    }

    suspend fun update(transaction: Transaction) = withContext(Dispatchers.IO) {
        dao.update(transaction.toEntity())
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun replaceAll(transactions: List<Transaction>) = withContext(Dispatchers.IO) {
        val sorted = transactions.sortedByDescending { it.date.atTime(it.time) }
        dao.replaceAll(sorted.map { it.toEntity() })
    }

    suspend fun addAll(transactions: List<Transaction>) = withContext(Dispatchers.IO) {
        if (transactions.isEmpty()) return@withContext
        dao.insertAll(transactions.map { it.toEntity() })
    }

    suspend fun exportCsv(uri: Uri) = withContext(Dispatchers.IO) {
        csvManager.exportTo(uri, dao.getAll().map { it.toTransaction() })
    }

    /** Returns the number of rows that failed to parse (0 = clean import). */
    suspend fun importCsv(uri: Uri): Int = withContext(Dispatchers.IO) {
        val result = csvManager.importFrom(uri)
        val sorted = result.transactions.sortedByDescending { it.date.atTime(it.time) }
        dao.replaceAll(sorted.map { it.toEntity() })
        result.failedLines.size
    }
}
