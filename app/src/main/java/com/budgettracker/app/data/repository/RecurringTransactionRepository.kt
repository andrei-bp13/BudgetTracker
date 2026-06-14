package com.budgettracker.app.data.repository

import com.budgettracker.app.data.db.RecurringTransactionDao
import com.budgettracker.app.data.db.toEntity
import com.budgettracker.app.data.db.toRecurring
import com.budgettracker.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class RecurringTransactionRepository(
    private val dao: RecurringTransactionDao
) {
    private val _recurring = MutableStateFlow<List<RecurringTransaction>>(emptyList())
    val recurring: StateFlow<List<RecurringTransaction>> = _recurring.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        _recurring.value = dao.getAll().map { it.toRecurring() }
    }

    suspend fun add(rec: RecurringTransaction) = withContext(Dispatchers.IO) {
        dao.insert(rec.toEntity())
        _recurring.value = (_recurring.value + rec).sortedBy { it.nextDueDate }
    }

    suspend fun update(rec: RecurringTransaction) = withContext(Dispatchers.IO) {
        dao.update(rec.toEntity())
        _recurring.value = _recurring.value
            .map { if (it.id == rec.id) rec else it }
            .sortedBy { it.nextDueDate }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
        _recurring.value = _recurring.value.filter { it.id != id }
    }

    /**
     * Generates Transaction rows for all active recurring transactions whose
     * nextDueDate is on or before today. Uses a deterministic transaction ID
     * (recurringId + epochDay) so re-processing after a crash is idempotent.
     */
    suspend fun processRecurring(transactionRepo: TransactionRepository) = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val due   = dao.getDue(today.toEpochDay()).map { it.toRecurring() }

        for (rec in due) {
            var nextDue   = rec.nextDueDate
            var count     = rec.occurrencesGenerated
            var newStatus = rec.status
            val generated = mutableListOf<Transaction>()

            while (!nextDue.isAfter(today)) {
                if (rec.endCondition == EndCondition.DATE &&
                    rec.endDate != null && nextDue.isAfter(rec.endDate)) {
                    newStatus = RecurringStatus.FINISHED
                    break
                }
                if (rec.endCondition == EndCondition.OCCURRENCES &&
                    rec.endAfterOccurrences != null && count >= rec.endAfterOccurrences) {
                    newStatus = RecurringStatus.FINISHED
                    break
                }
                generated += Transaction(
                    id          = "${rec.id}_${nextDue.toEpochDay()}",
                    title       = "${rec.title} (R)",
                    amount      = rec.amount,
                    type        = rec.type,
                    category    = rec.category,
                    paymentType = rec.paymentType,
                    note        = rec.note,
                    date        = nextDue,
                    time        = LocalTime.MIDNIGHT
                )
                count++
                nextDue = rec.advanceDate(nextDue)
            }

            if (generated.isNotEmpty()) transactionRepo.addAll(generated)

            dao.update(rec.copy(
                nextDueDate          = nextDue,
                occurrencesGenerated = count,
                status               = newStatus
            ).toEntity())
        }

        _recurring.value = dao.getAll().map { it.toRecurring() }
    }
}
