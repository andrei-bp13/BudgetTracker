package com.pardonmyspending.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class DriveBackupRepository {

    companion object {
        private const val FILE_NAME    = "pardon_my_spending_backup.json"
        private const val FILES_URL    = "https://www.googleapis.com/drive/v3/files"
        private const val UPLOAD_URL   = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
        private const val CONNECT_MS   = 15_000
        private const val READ_MS      = 30_000
    }

    suspend fun upload(json: String, token: String): Boolean = withContext(Dispatchers.IO) {
        val existingId = findFileId(token)
        if (existingId != null) deleteFile(existingId, token)

        val boundary = "PMS_DriveBackup_Boundary"
        val metadata = """{"name":"$FILE_NAME","parents":["appDataFolder"]}"""
        val body = "--$boundary\r\n" +
                   "Content-Type: application/json; charset=UTF-8\r\n\r\n" +
                   "$metadata\r\n" +
                   "--$boundary\r\n" +
                   "Content-Type: application/json; charset=UTF-8\r\n\r\n" +
                   "$json\r\n" +
                   "--$boundary--"

        val conn = openConn(UPLOAD_URL, "POST", token).apply {
            setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            doOutput = true
        }
        OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(body) }
        val code = conn.responseCode
        conn.disconnect()
        code in 200..299
    }

    suspend fun downloadLatest(token: String): String? = withContext(Dispatchers.IO) {
        val fileId = findFileId(token) ?: return@withContext null
        val conn = openConn("$FILES_URL/$fileId?alt=media", "GET", token)
        if (conn.responseCode !in 200..299) { conn.disconnect(); return@withContext null }
        val text = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
        conn.disconnect()
        text
    }

    private fun findFileId(token: String): String? {
        val q   = java.net.URLEncoder.encode("name='$FILE_NAME'", "UTF-8")
        val conn = openConn("$FILES_URL?spaces=appDataFolder&fields=files(id)&q=$q", "GET", token)
        if (conn.responseCode !in 200..299) { conn.disconnect(); return null }
        val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
        conn.disconnect()
        val files = JSONObject(body).getJSONArray("files")
        return if (files.length() > 0) files.getJSONObject(0).getString("id") else null
    }

    private fun deleteFile(fileId: String, token: String) {
        val conn = openConn("$FILES_URL/$fileId", "DELETE", token)
        conn.responseCode
        conn.disconnect()
    }

    private fun openConn(url: String, method: String, token: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod    = method
            connectTimeout   = CONNECT_MS
            readTimeout      = READ_MS
            setRequestProperty("Authorization", "Bearer $token")
        }
}
