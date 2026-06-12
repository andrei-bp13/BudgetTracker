package com.budgettracker.app.data.repository

import android.net.Uri
import com.budgettracker.app.data.csv.CsvManager
import com.budgettracker.app.data.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class TransactionRepository(private val csvManager: CsvManager) {

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
    }

    suspend fun update(transaction: Transaction) = withContext(Dispatchers.IO) {
        val updated = _transactions.value.map { if (it.id == transaction.id) transaction else it }
            .sortedByDescending { it.date }
        _transactions.value = updated
        csvManager.writeAll(updated)
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        val updated = _transactions.value.filter { it.id != id }
        _transactions.value = updated
        csvManager.writeAll(updated)
    }

    suspend fun exportCsv(uri: Uri) = withContext(Dispatchers.IO) {
        csvManager.exportTo(uri)
    }

    suspend fun importCsv(uri: Uri) = withContext(Dispatchers.IO) {
        val imported = csvManager.importFrom(uri)
        _transactions.value = imported.sortedByDescending { it.date }
        csvManager.writeAll(imported)
    }
}
