package com.budgettracker.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.budgettracker.app.data.model.*
import java.time.LocalDate

@Entity(tableName = "recurring_transactions")
data class RecurringTransactionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val amount: Double,
    val type: String,
    val category: String,
    val paymentType: String,
    val note: String,
    val frequency: String,
    val interval: Int,
    val intervalUnit: String,
    val startDateEpochDay: Long,
    val nextDueDateEpochDay: Long,
    val status: String,
    val endCondition: String,
    val endDateEpochDay: Long?,
    val endAfterOccurrences: Int?,
    val occurrencesGenerated: Int
)

fun RecurringTransaction.toEntity() = RecurringTransactionEntity(
    id                   = id,
    title                = title,
    amount               = amount,
    type                 = type.name,
    category             = category,
    paymentType          = paymentType.name,
    note                 = note,
    frequency            = frequency.name,
    interval             = interval,
    intervalUnit         = intervalUnit.name,
    startDateEpochDay    = startDate.toEpochDay(),
    nextDueDateEpochDay  = nextDueDate.toEpochDay(),
    status               = status.name,
    endCondition         = endCondition.name,
    endDateEpochDay      = endDate?.toEpochDay(),
    endAfterOccurrences  = endAfterOccurrences,
    occurrencesGenerated = occurrencesGenerated
)

fun RecurringTransactionEntity.toRecurring() = RecurringTransaction(
    id                   = id,
    title                = title,
    amount               = amount,
    type                 = runCatching { TransactionType.valueOf(type) }.getOrDefault(TransactionType.EXPENSE),
    category             = category,
    paymentType          = runCatching { PaymentType.valueOf(paymentType) }.getOrDefault(PaymentType.DEBIT),
    note                 = note,
    frequency            = runCatching { RecurringFrequency.valueOf(frequency) }.getOrDefault(RecurringFrequency.MONTHLY),
    interval             = interval,
    intervalUnit         = runCatching { IntervalUnit.valueOf(intervalUnit) }.getOrDefault(IntervalUnit.MONTHS),
    startDate            = LocalDate.ofEpochDay(startDateEpochDay),
    nextDueDate          = LocalDate.ofEpochDay(nextDueDateEpochDay),
    status               = runCatching { RecurringStatus.valueOf(status) }.getOrDefault(RecurringStatus.ACTIVE),
    endCondition         = runCatching { EndCondition.valueOf(endCondition) }.getOrDefault(EndCondition.NEVER),
    endDate              = endDateEpochDay?.let { LocalDate.ofEpochDay(it) },
    endAfterOccurrences  = endAfterOccurrences,
    occurrencesGenerated = occurrencesGenerated
)
