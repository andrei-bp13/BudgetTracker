package com.pardonmyspending.app.data.csv

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.pardonmyspending.app.data.model.Transaction
import java.io.File
import java.time.LocalDate

class CsvManager(private val context: Context) {

    fun exportTo(uri: Uri, transactions: List<Transaction>) {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.bufferedWriter().use { writer ->
                writer.write("$CSV_HEADER\n")
                transactions.forEach { writer.write("${serializeCsvLine(it)}\n") }
            }
        }
    }

    /**
     * Imports transactions from [uri]. Returns a ParseResult; if any rows failed,
     * an error log is written to the Downloads folder.
     */
    fun importFrom(uri: Uri): ParseResult {
        val reader = context.contentResolver.openInputStream(uri)
            ?.bufferedReader() ?: return ParseResult(emptyList(), emptyList())
        val result = reader.use { parseCsvContent(it) }
        if (result.failedLines.isNotEmpty()) writeErrorLog(result.failedLines)
        return result
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun writeErrorLog(failedLines: List<String>) {
        val date     = LocalDate.now().toString()
        val fileName = "PardonMySpending_errorlog_import_$date.txt"
        val content  = buildString {
            appendLine("BudgetTracker Import Error Log — $date")
            appendLine("Failed rows: ${failedLines.size}")
            appendLine()
            appendLine("The following lines could not be parsed and were skipped:")
            appendLine()
            failedLines.forEach { appendLine(it) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            writeToMediaStoreDownloads(fileName, content)
        } else {
            writeToExternalFiles(fileName, content)
        }
    }

    private fun writeToMediaStoreDownloads(fileName: String, content: String) {
        val resolver   = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        // Overwrite: delete any existing file with the same name first
        resolver.query(
            collection,
            arrayOf(MediaStore.Downloads._ID),
            "${MediaStore.Downloads.DISPLAY_NAME} = ?",
            arrayOf(fileName),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                resolver.delete(ContentUris.withAppendedId(collection, id), null, null)
            }
        }
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        resolver.insert(collection, values)?.let { uri ->
            resolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
        }
    }

    private fun writeToExternalFiles(fileName: String, content: String) {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        File(dir, fileName).writeText(content)
    }
}
