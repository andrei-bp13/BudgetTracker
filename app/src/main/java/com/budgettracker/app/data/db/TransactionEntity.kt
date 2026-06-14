package com.budgettracker.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.budgettracker.app.data.model.PaymentType
import com.budgettracker.app.data.model.Transaction
import com.budgettracker.app.data.model.TransactionType
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "transactions", indices = [Index(value = ["dateEpochDay"])])
data class TransactionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val amount: Double,
    val type: String,
    val category: String,
    val dateEpochDay: Long,
    val note: String,
    val paymentType: String,
    val timeSecondOfDay: Int = 0
)

fun Transaction.toEntity() = TransactionEntity(
    id              = id,
    title           = title,
    amount          = amount,
    type            = type.name,
    category        = category,
    dateEpochDay    = date.toEpochDay(),
    note            = note,
    paymentType     = paymentType.name,
    timeSecondOfDay = time.toSecondOfDay()
)

fun TransactionEntity.toTransaction() = Transaction(
    id          = id,
    title       = title,
    amount      = amount,
    type        = runCatching { TransactionType.valueOf(type) }.getOrDefault(TransactionType.EXPENSE),
    category    = category,
    date        = LocalDate.ofEpochDay(dateEpochDay),
    note        = note,
    paymentType = runCatching { PaymentType.valueOf(paymentType) }.getOrDefault(PaymentType.DEBIT),
    time        = LocalTime.ofSecondOfDay(timeSecondOfDay.toLong())
)
