package com.pardonmyspending.app.data.csv

import com.pardonmyspending.app.data.model.PaymentType
import com.pardonmyspending.app.data.model.Transaction
import com.pardonmyspending.app.data.model.TransactionType
import java.io.BufferedReader
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class ParseResult(val transactions: List<Transaction>, val failedLines: List<String>)

fun parseCsvContent(reader: BufferedReader): ParseResult {
    val transactions = mutableListOf<Transaction>()
    val failedLines  = mutableListOf<String>()
    reader.readLine() // skip header
    reader.forEachLine { line ->
        if (line.isBlank()) return@forEachLine
        val txn = parseCsvLine(line)
        if (txn != null) transactions.add(txn) else failedLines.add(line)
    }
    return ParseResult(transactions, failedLines)
}

const val CSV_HEADER = "id,title,amount,type,category,date,note,payment_type,time"

fun splitCsvLine(line: String): List<String> {
    val result   = mutableListOf<String>()
    val sb       = StringBuilder()
    var inQuotes = false
    var i        = 0
    while (i < line.length) {
        when {
            // "" inside a quoted field â†’ literal " character (RFC 4180)
            line[i] == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                sb.append('"'); i += 2
            }
            line[i] == '"'              -> { inQuotes = !inQuotes; i++ }
            line[i] == ',' && !inQuotes -> { result.add(sb.toString()); sb.clear(); i++ }
            else                        -> { sb.append(line[i]); i++ }
        }
    }
    result.add(sb.toString())
    return result
}

fun escapeCsv(value: String): String =
    if (value.contains(',') || value.contains('"'))
        "\"${value.replace("\"", "\"\"")}\"" else value

fun parseCsvLine(line: String): Transaction? = runCatching {
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
        paymentType = if (cols.size > 7)
                          runCatching { PaymentType.valueOf(cols[7]) }.getOrDefault(PaymentType.DEBIT)
                      else PaymentType.DEBIT,
        time        = if (cols.size > 8) runCatching { LocalTime.parse(cols[8]) }.getOrDefault(LocalTime.MIDNIGHT)
                      else LocalTime.MIDNIGHT
    )
}.getOrNull()

fun serializeCsvLine(t: Transaction): String =
    "${t.id},${escapeCsv(t.title)},${t.amount},${t.type.name}," +
    "${escapeCsv(t.category)},${t.date},${escapeCsv(t.note)},${t.paymentType.name},${t.time}"
