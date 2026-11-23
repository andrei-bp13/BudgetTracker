package com.pardonmyspending.app.data.repository

import com.pardonmyspending.app.data.db.RecurringTransactionDao
import com.pardonmyspending.app.data.db.toEntity
import com.pardonmyspending.app.data.db.toRecurring
import com.pardonmyspending.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

class RecurringTransactionRepository(
    private val dao: RecurringTransactionDao
) {
    val recurring: Flow<List<RecurringTransaction>> = dao.observeAll()
        .map { entities -> entities.map { it.toRecurring() } }

    suspend fun add(rec: RecurringTransaction) = withContext(Dispatchers.IO) {
        dao.insert(rec.toEntity())
    }

    suspend fun update(rec: RecurringTransaction) = withContext(Dispatchers.IO) {
        dao.update(rec.toEntity())
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun replaceAll(recs: List<RecurringTransaction>) = withContext(Dispatchers.IO) {
        dao.replaceAll(recs.map { it.toEntity() })
    }

    /**
     * Generates Transaction rows for all active recurring transactions whose
     * nextDueDate is on or before today. Uses a deterministic transaction ID
     * (recurringId + epochDay) so re-processing after a crash is idempotent.
     *
     * All generated transactions and recurring-state updates are written in two
     * single batch operations instead of one DB call per recurring rule.
     */
    suspend fun processRecurring(transactionRepo: TransactionRepository) = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val due   = dao.getDue(today.toEpochDay()).map { it.toRecurring() }

        val allGenerated = mutableListOf<Transaction>()
        val updatedRecs  = mutableListOf<com.pardonmyspending.app.data.db.RecurringTransactionEntity>()

        for (rec in due) {
            var nextDue   = rec.nextDueDate
            var count     = rec.occurrencesGenerated
            var newStatus = rec.status
            val generated = mutableListOf<Transaction>()

            var iterations = 0
            while (!nextDue.isAfter(today) && iterations < 10_000) {
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
                iterations++
                val advanced = rec.advanceDate(nextDue)
                if (!advanced.isAfter(nextDue)) break
                nextDue = advanced
            }

            allGenerated += generated
            updatedRecs  += rec.copy(
                nextDueDate          = nextDue,
                occurrencesGenerated = count,
                status               = newStatus
            ).toEntity()
        }

        // Two batch writes instead of N individual writes per recurring rule.
        if (allGenerated.isNotEmpty()) transactionRepo.addAll(allGenerated)
        if (updatedRecs.isNotEmpty())  dao.updateAll(updatedRecs)
        // Room's observeAll() Flow emits the updated recurring list automatically.
    }
}
