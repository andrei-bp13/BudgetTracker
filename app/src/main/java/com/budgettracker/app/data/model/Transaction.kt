package com.budgettracker.app.data.model

import java.time.LocalDate

enum class TransactionType { INCOME, EXPENSE }
enum class PaymentType { DEBIT, CREDIT, CASH }

data class Transaction(
    val id: String,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val date: LocalDate,
    val note: String = "",
    val paymentType: PaymentType = PaymentType.DEBIT
)
