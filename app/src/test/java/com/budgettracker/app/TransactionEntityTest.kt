package com.pardonmyspending.app

import com.pardonmyspending.app.data.db.BudgetEntity
import com.pardonmyspending.app.data.db.TransactionEntity
import com.pardonmyspending.app.data.db.toBudget
import com.pardonmyspending.app.data.db.toEntity
import com.pardonmyspending.app.data.db.toTransaction
import com.pardonmyspending.app.data.model.BudgetPeriod
import com.pardonmyspending.app.data.model.PaymentType
import com.pardonmyspending.app.data.model.Transaction
import com.pardonmyspending.app.data.model.TransactionType
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

    @Test fun `toTransaction clamps out-of-range timeSecondOfDay`() {
        val negative = sample.toEntity().copy(timeSecondOfDay = -50)
        val overflow = sample.toEntity().copy(timeSecondOfDay = 99_999)
        assertEquals(LocalTime.of(0, 0, 0), negative.toTransaction().time)
        assertEquals(LocalTime.of(23, 59, 59), overflow.toTransaction().time)
    }

    @Test fun `BudgetEntity defaults unknown period to MONTHLY`() {
        val entity = BudgetEntity(
            id = "b1", title = "Food", categoryName = "Food",
            amount = 200.0, period = "INVALID", createdEpochDay = 20000L, sortOrder = 0
        )
        assertEquals(BudgetPeriod.MONTHLY, entity.toBudget().period)
    }
}
