package com.budgettracker.app

import com.budgettracker.app.data.db.TransactionEntity
import com.budgettracker.app.data.db.toEntity
import com.budgettracker.app.data.db.toTransaction
import com.budgettracker.app.data.model.PaymentType
import com.budgettracker.app.data.model.Transaction
import com.budgettracker.app.data.model.TransactionType
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals

class TransactionEntityTest {

    private val sample = Transaction(
        id          = "abc-123",
        title       = "Groceries",
        amount      = 42.50,
        type        = TransactionType.EXPENSE,
        category    = "Food",
        date        = LocalDate.of(2025, 6, 15),
        note        = "weekly shop",
        paymentType = PaymentType.CREDIT,
        time        = LocalTime.of(14, 30)
    )

    @Test fun `toEntity preserves all fields`() {
        val entity = sample.toEntity()
        assertEquals("abc-123",                    entity.id)
        assertEquals("Groceries",                  entity.title)
        assertEquals(42.50,                        entity.amount)
        assertEquals("EXPENSE",                    entity.type)
        assertEquals("Food",                       entity.category)
        assertEquals(LocalDate.of(2025, 6, 15).toEpochDay(), entity.dateEpochDay)
        assertEquals("weekly shop",                entity.note)
        assertEquals("CREDIT",                     entity.paymentType)
        assertEquals(LocalTime.of(14, 30).toSecondOfDay(), entity.timeSecondOfDay)
    }

    @Test fun `toTransaction round-trips through toEntity`() {
        val roundTripped = sample.toEntity().toTransaction()
        assertEquals(sample, roundTripped)
    }

    @Test fun `toTransaction with income type round-trips`() {
        val income = sample.copy(type = TransactionType.INCOME, paymentType = PaymentType.DEBIT)
        assertEquals(income, income.toEntity().toTransaction())
    }

    @Test fun `toTransaction with cash payment type round-trips`() {
        val cash = sample.copy(paymentType = PaymentType.CASH)
        assertEquals(cash, cash.toEntity().toTransaction())
    }

    @Test fun `toTransaction defaults unknown type to EXPENSE`() {
        val entity = sample.toEntity().copy(type = "UNKNOWN")
        assertEquals(TransactionType.EXPENSE, entity.toTransaction().type)
    }

    @Test fun `toTransaction defaults unknown paymentType to DEBIT`() {
        val entity = sample.toEntity().copy(paymentType = "UNKNOWN")
        assertEquals(PaymentType.DEBIT, entity.toTransaction().paymentType)
    }

    @Test fun `toEntity stores date as epoch day`() {
        val date   = LocalDate.of(2024, 1, 1)
        val entity = sample.copy(date = date).toEntity()
        assertEquals(date, LocalDate.ofEpochDay(entity.dateEpochDay))
    }

    @Test fun `TransactionEntity data class equality works`() {
        val e1 = sample.toEntity()
        val e2 = sample.toEntity()
        assertEquals(e1, e2)
    }
}
