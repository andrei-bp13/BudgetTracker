package com.budgettracker.app.data.repository

import android.net.Uri
import com.budgettracker.app.data.csv.CsvManager
import com.budgettracker.app.data.db.TransactionDao
import com.budgettracker.app.data.db.toEntity
import com.budgettracker.app.data.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class TransactionRepository(
    private val csvManager: CsvManager,
    private val dao: TransactionDao? = null
) {

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        csvManager.ensureInitialized()
        _transactions.value = csvManager.readAll().sortedByDescending { it.date }
    }

    suspend fun add(transaction: Transaction) = withContext(Dispatchers.IO) {
        val updated = (_transactions.value + transaction).sortedByDescending { it.date }
        _transactions.value = updated
        csvManager.writeAll(updated)
        dao?.insert(transaction.toEntity())
    }

    suspend fun update(transaction: Transaction) = withContext(Dispatchers.IO) {
        val updated = _transactions.value.map { if (it.id == transaction.id) transaction else it }
            .sortedByDescending { it.date }
        _transactions.value = updated
        csvManager.writeAll(updated)
        dao?.insert(transaction.toEntity())
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        val updated = _transactions.value.filter { it.id != id }
        _transactions.value = updated
        csvManager.writeAll(updated)
        dao?.deleteById(id)
    }

    suspend fun exportCsv(uri: Uri) = withContext(Dispatchers.IO) {
        csvManager.exportTo(uri)
    }

    /** Returns the number of rows that failed to parse (0 = clean import). */
    suspend fun importCsv(uri: Uri): Int = withContext(Dispatchers.IO) {
        val result = csvManager.importFrom(uri)
        val sorted = result.transactions.sortedByDescending { it.date }
        _transactions.value = sorted
        csvManager.writeAll(sorted)
        dao?.let { d ->
            d.deleteAll()
            d.insertAll(sorted.map { it.toEntity() })
        }
        result.failedLines.size
    }
}
