package com.pardonmyspending.app

import com.pardonmyspending.app.data.csv.*
import com.pardonmyspending.app.data.model.PaymentType
import com.pardonmyspending.app.data.model.Transaction
import com.pardonmyspending.app.data.model.TransactionType
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CsvParserTest {

    // ── splitCsvLine ──────────────────────────────────────────────────────────

    @Test fun `splitCsvLine splits simple comma-separated line`() {
        val cols = splitCsvLine("a,b,c,d")
        assertEquals(listOf("a", "b", "c", "d"), cols)
    }

    @Test fun `splitCsvLine keeps quoted field with comma intact`() {
        val cols = splitCsvLine("id,\"Food, Dining\",10.0")
        assertEquals(3, cols.size)
        assertEquals("Food, Dining", cols[1])
    }

    @Test fun `splitCsvLine handles empty fields`() {
        val cols = splitCsvLine("a,,c")
        assertEquals(listOf("a", "", "c"), cols)
    }

    @Test fun `splitCsvLine handles trailing comma`() {
        val cols = splitCsvLine("a,b,")
        assertEquals(listOf("a", "b", ""), cols)
    }

    @Test fun `splitCsvLine handles quoted field containing double-quote`() {
        // CSV line: id,"say ""hello""",50  (double-quote escaping inside quoted field)
        val line = "id,\"say \"\"hello\"\"\",50"
        val cols = splitCsvLine(line)
        assertEquals(3, cols.size)
        assertEquals("say \"hello\"", cols[1])
    }

    // ── escapeCsv ─────────────────────────────────────────────────────────────

    @Test fun `escapeCsv returns value unchanged when no special chars`() {
        assertEquals("Groceries", escapeCsv("Groceries"))
    }

    @Test fun `escapeCsv wraps field containing comma in quotes`() {
        assertEquals("\"Food, Drinks\"", escapeCsv("Food, Drinks"))
    }

    @Test fun `escapeCsv wraps field containing double-quote and escapes it`() {
        // say "hi" → "say ""hi"""
        val result = escapeCsv("say \"hi\"")
        assertEquals("\"say \"\"hi\"\"\"", result)
    }

    @Test fun `escapeCsv roundtrips through splitCsvLine`() {
        val original = "Coffee, snacks & \"treats\""
        val escaped  = escapeCsv(original)
        val back     = splitCsvLine(escaped)
        assertEquals(listOf(original), back)
    }

    // ── parseCsvLine ──────────────────────────────────────────────────────────

    @Test fun `parseCsvLine parses a complete valid row`() {
        val line = "abc-123,Lunch,12.50,EXPENSE,Food,2025-06-01,,DEBIT"
        val t    = parseCsvLine(line)
        assertNotNull(t)
        assertEquals("abc-123",               t.id)
        assertEquals("Lunch",                 t.title)
        assertEquals(12.50,                   t.amount)
        assertEquals(TransactionType.EXPENSE,  t.type)
        assertEquals("Food",                   t.category)
        assertEquals(LocalDate.of(2025, 6, 1), t.date)
        assertEquals("",                       t.note)
        assertEquals(PaymentType.DEBIT,        t.paymentType)
    }

    @Test fun `parseCsvLine parses row with note`() {
        val line = "id1,Salary,3000.0,INCOME,Salary,2025-05-31,Monthly salary,CREDIT"
        val t    = parseCsvLine(line)
        assertNotNull(t)
        assertEquals("Monthly salary", t.note)
        assertEquals(PaymentType.CREDIT, t.paymentType)
    }

    @Test fun `parseCsvLine returns null for too-few columns`() {
        assertNull(parseCsvLine("id,title,10.0"))
    }

    @Test fun `parseCsvLine returns null for invalid amount`() {
        assertNull(parseCsvLine("id,title,notanumber,EXPENSE,Food,2025-01-01"))
    }

    @Test fun `parseCsvLine returns null for invalid date`() {
        assertNull(parseCsvLine("id,title,10.0,EXPENSE,Food,not-a-date"))
    }

    @Test fun `parseCsvLine returns null for invalid type`() {
        assertNull(parseCsvLine("id,title,10.0,BADTYPE,Food,2025-01-01"))
    }

    @Test fun `parseCsvLine generates UUID when id is blank`() {
        val line = ",Lunch,10.0,EXPENSE,Food,2025-06-01"
        val t    = parseCsvLine(line)
        assertNotNull(t)
        assertTrue(t.id.isNotBlank())
    }

    @Test fun `parseCsvLine defaults payment type to DEBIT when missing`() {
        val line = "id,Lunch,10.0,EXPENSE,Food,2025-06-01,note"
        val t    = parseCsvLine(line)
        assertNotNull(t)
        assertEquals(PaymentType.DEBIT, t.paymentType)
    }

    @Test fun `parseCsvLine defaults payment type to DEBIT for unknown value`() {
        val line = "id,Lunch,10.0,EXPENSE,Food,2025-06-01,note,UNKNOWN"
        val t    = parseCsvLine(line)
        assertNotNull(t)
        assertEquals(PaymentType.DEBIT, t.paymentType)
    }

    @Test fun `parseCsvLine handles quoted title with comma`() {
        val line = "id,\"Coffee, Snack\",5.0,EXPENSE,Food,2025-06-01"
        val t    = parseCsvLine(line)
        assertNotNull(t)
        assertEquals("Coffee, Snack", t.title)
    }

    // ── serializeCsvLine ──────────────────────────────────────────────────────

    @Test fun `serializeCsvLine round-trips through parseCsvLine`() {
        val original = Transaction(
            id          = "xyz-789",
            title       = "Dinner",
            amount      = 45.0,
            type        = TransactionType.EXPENSE,
            category    = "Food",
            date        = LocalDate.of(2025, 6, 15),
            note        = "anniversary",
            paymentType = PaymentType.CREDIT
        )
        val serialized = serializeCsvLine(original)
        val parsed     = parseCsvLine(serialized)
        assertNotNull(parsed)
        assertEquals(original, parsed)
    }

    @Test fun `serializeCsvLine escapes commas in title`() {
        val t = Transaction(
            id = "1", title = "Food, Drinks", amount = 10.0,
            type = TransactionType.EXPENSE, category = "Food",
            date = LocalDate.of(2025, 1, 1)
        )
        val line = serializeCsvLine(t)
        val back = parseCsvLine(line)
        assertNotNull(back)
        assertEquals("Food, Drinks", back.title)
    }

    @Test fun `serializeCsvLine escapes commas in category`() {
        val t = Transaction(
            id = "1", title = "Test", amount = 5.0,
            type = TransactionType.EXPENSE, category = "Food, Drinks",
            date = LocalDate.of(2025, 1, 1)
        )
        val line = serializeCsvLine(t)
        val back = parseCsvLine(line)
        assertNotNull(back)
        assertEquals("Food, Drinks", back.category)
    }

    // ── CSV_HEADER ────────────────────────────────────────────────────────────

    @Test fun `CSV_HEADER has exactly 9 columns`() {
        assertEquals(9, CSV_HEADER.split(",").size)
    }
}
