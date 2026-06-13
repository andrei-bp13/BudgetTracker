package com.budgettracker.app.data.repository

import android.net.Uri
import com.budgettracker.app.data.csv.CsvManager
import com.budgettracker.app.data.db.TransactionDao
import com.budgettracker.app.data.db.toEntity
import com.budgettracker.app.data.db.toTransaction
import com.budgettracker.app.data.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class TransactionRepository(
    private val csvManager: CsvManager,
    private val dao: TransactionDao
) {

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        // One-time migration: if Room is empty but internal CSV has data, seed Room from it.
        // Self-healing: also covers reinstall scenarios where Room was wiped but CSV survived.
        val csvTransactions = csvManager.readAll()
        if (dao.getCount() == 0 && csvTransactions.isNotEmpty()) {
            dao.insertAll(csvTransactions.map { it.toEntity() })
        }
        // Room is now the primary source of truth.
        _transactions.value = dao.getAll().map { it.toTransaction() }
    }

    suspend fun add(transaction: Transaction) = withContext(Dispatchers.IO) {
        dao.insert(transaction.toEntity())
        _transactions.value = (_transactions.value + transaction).sortedByDescending { it.date }
    }

    suspend fun update(transaction: Transaction) = withContext(Dispatchers.IO) {
        dao.insert(transaction.toEntity())
        _transactions.value = _transactions.value
            .map { if (it.id == transaction.id) transaction else it }
            .sortedByDescending { it.date }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
        _transactions.value = _transactions.value.filter { it.id != id }
    }

    suspend fun exportCsv(uri: Uri) = withContext(Dispatchers.IO) {
        csvManager.exportTo(uri, _transactions.value)
    }

    /** Returns the number of rows that failed to parse (0 = clean import). */
    suspend fun importCsv(uri: Uri): Int = withContext(Dispatchers.IO) {
        val result = csvManager.importFrom(uri)
        val sorted = result.transactions.sortedByDescending { it.date }
        dao.deleteAll()
        dao.insertAll(sorted.map { it.toEntity() })
        _transactions.value = sorted
        result.failedLines.size
    }
}
