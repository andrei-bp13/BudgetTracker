package com.budgettracker.app.data.csv

import android.content.Context
import android.net.Uri
import com.budgettracker.app.data.model.Transaction
import java.io.BufferedReader
import java.io.File

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
            writer.write("$CSV_HEADER\n")
            transactions.forEach { t -> writer.write("${serializeCsvLine(t)}\n") }
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
        reader.forEachLine { line -> parseCsvLine(line)?.let { result.add(it) } }
        return result
    }
}
