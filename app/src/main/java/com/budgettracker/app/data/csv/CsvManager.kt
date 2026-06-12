package com.budgettracker.app.data.csv

import android.content.Context
import android.net.Uri
import com.budgettracker.app.data.model.PaymentType
import com.budgettracker.app.data.model.Transaction
import com.budgettracker.app.data.model.TransactionType
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.time.LocalDate
import java.util.UUID

class CsvManager(private val context: Context) {

    private val csvFile: File
        get() = File(context.filesDir, "transactions.csv")

    fun ensureInitialized() {
        if (!csvFile.exists()) {
            context.assets.open("transactions.csv").use { input ->
                csvFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }

    fun readAll(): List<Transaction> {
        if (!csvFile.exists()) ensureInitialized()
        return csvFile.bufferedReader().use { parseReader(it) }
    }

    fun writeAll(transactions: List<Transaction>) {
        csvFile.bufferedWriter().use { writer ->
            writer.write("id,title,amount,type,category,date,note,payment_type\n")
            transactions.forEach { t ->
                writer.write("${t.id},${escapeCsv(t.title)},${t.amount},${t.type.name},${escapeCsv(t.category)},${t.date},${escapeCsv(t.note)},${t.paymentType.name}\n")
            }
        }
    }

    fun exportTo(uri: Uri) {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            csvFile.inputStream().use { it.copyTo(out) }
        }
    }

    fun importFrom(uri: Uri): List<Transaction> {
        val reader = context.contentResolver.openInputStream(uri)
            ?.bufferedReader() ?: return emptyList()
        return reader.use { parseReader(it) }
    }

    private fun parseReader(reader: BufferedReader): List<Transaction> {
        val result = mutableListOf<Transaction>()
        reader.readLine() // skip header
        reader.forEachLine { line ->
            parseLine(line)?.let { result.add(it) }
        }
        return result
    }

    private fun parseLine(line: String): Transaction? = runCatching {
        val cols = splitCsvLine(line)
        if (cols.size < 6) return null
        Transaction(
            id          = cols[0].ifBlank { UUID.randomUUID().toString() },
            title       = cols[1],
            amount      = cols[2].toDouble(),
            type        = TransactionType.valueOf(cols[3]),
            category    = cols[4],
            date        = LocalDate.parse(cols[5]),
            note        = if (cols.size > 6) cols[6] else "",
            paymentType = if (cols.size > 7) runCatching { PaymentType.valueOf(cols[7]) }.getOrDefault(PaymentType.DEBIT) else PaymentType.DEBIT
        )
    }.getOrNull()

    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> { result.add(sb.toString()); sb.clear() }
                else -> sb.append(ch)
            }
        }
        result.add(sb.toString())
        return result
    }

    private fun escapeCsv(value: String): String =
        if (value.contains(',') || value.contains('"')) "\"${value.replace("\"", "\"\"")}\"" else value
}
