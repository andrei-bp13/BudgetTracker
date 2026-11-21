package com.pardonmyspending.app.data.repository

import com.pardonmyspending.app.data.model.PaymentType
import com.pardonmyspending.app.data.model.Transaction
import com.pardonmyspending.app.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class ParsedTransaction(
    val title: String,
    val amount: Double,
    val category: String,
    val paymentType: PaymentType,
    val type: TransactionType,
    val date: LocalDate,
    val note: String = ""
)

fun ParsedTransaction.toTransaction() = Transaction(
    id          = UUID.randomUUID().toString(),
    title       = title,
    amount      = amount,
    type        = type,
    category    = category,
    date        = date,
    note        = note,
    paymentType = paymentType,
    time        = LocalTime.now().withSecond(0).withNano(0)
)

data class CategorySuggestion(
    val category: String,
    val paymentType: PaymentType,
    val type: TransactionType
)

class AiRepository {

    companion object {
        private const val ENDPOINT = "https://api.mistral.ai/v1/chat/completions"
        private const val MODEL    = "open-mistral-nemo"
    }

    suspend fun parseTransaction(
        apiKey: String,
        input: String,
        expenseCategories: List<String>,
        incomeCategories: List<String>,
        today: LocalDate
    ): Result<ParsedTransaction> = withContext(Dispatchers.IO) {
        runCatching {
            val prompt = """
                Today's date is $today.
                Parse the following transaction description and extract structured data.

                Available expense categories: ${expenseCategories.joinToString(", ")}
                Available income categories: ${incomeCategories.joinToString(", ")}
                Payment types: DEBIT, CREDIT, CASH

                Description: "$input"

                Return a JSON object with these exact fields:
                {
                  "title": "concise title for the transaction",
                  "amount": <exact numeric value, no rounding>,
                  "category": "exact category name from the lists above",
                  "paymentType": "DEBIT" or "CREDIT" or "CASH",
                  "type": "EXPENSE" or "INCOME",
                  "date": "YYYY-MM-DD - resolve relative words like yesterday, last Monday, etc. to an actual date; use today if no date is mentioned",
                  "note": "any extra detail unrelated to amount/category/date, or empty string"
                }
            """.trimIndent()

            val json = callMistral(apiKey, prompt)
            ParsedTransaction(
                title       = json.getString("title"),
                amount      = kotlin.math.abs(json.getDouble("amount")),
                category    = json.getString("category"),
                paymentType = runCatching { PaymentType.valueOf(json.getString("paymentType")) }.getOrDefault(PaymentType.DEBIT),
                type        = runCatching { TransactionType.valueOf(json.getString("type")) }.getOrDefault(TransactionType.EXPENSE),
                date        = runCatching { LocalDate.parse(json.getString("date")) }.getOrDefault(today),
                note        = json.optString("note", "")
            )
        }
    }

    suspend fun suggestCategory(
        apiKey: String,
        title: String,
        expenseCategories: List<String>,
        incomeCategories: List<String>
    ): CategorySuggestion? = withContext(Dispatchers.IO) {
        runCatching {
            val prompt = """
                Transaction title: "$title"
                Available expense categories: ${expenseCategories.joinToString(", ")}
                Available income categories: ${incomeCategories.joinToString(", ")}

                Based on the title, return a JSON object:
                {
                  "category": "exact category name from the lists",
                  "paymentType": "DEBIT" or "CREDIT" or "CASH",
                  "type": "EXPENSE" or "INCOME"
                }
            """.trimIndent()

            val json = callMistral(apiKey, prompt)
            CategorySuggestion(
                category    = json.getString("category"),
                paymentType = runCatching { PaymentType.valueOf(json.getString("paymentType")) }.getOrDefault(PaymentType.DEBIT),
                type        = runCatching { TransactionType.valueOf(json.getString("type")) }.getOrDefault(TransactionType.EXPENSE)
            )
        }.getOrNull()
    }

    private fun callMistral(apiKey: String, prompt: String): JSONObject {
        if (apiKey.isBlank()) throw IllegalStateException("API key is empty")

        val url  = URL(ENDPOINT)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod  = "POST"
            doOutput       = true
            connectTimeout = 15_000
            readTimeout    = 30_000
            setRequestProperty("Content-Type",  "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
        }

        val body = JSONObject().apply {
            put("model", MODEL)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role",    "system")
                    put("content",
                        "You are a financial assistant embedded in a personal budget tracking app. " +
                        "You ONLY perform tasks related to logging transactions, categorizing expenses and income, " +
                        "and analyzing spending data. " +
                        "Refuse any request unrelated to personal finance or this app's operations. " +
                        "Always respond with valid JSON as instructed."
                    )
                })
                put(JSONObject().apply {
                    put("role",    "user")
                    put("content", prompt)
                })
            })
            put("response_format", JSONObject().put("type", "json_object"))
        }.toString()

        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

        val code = conn.responseCode
        if (code != 200) {
            val errBody = runCatching { conn.errorStream?.bufferedReader()?.readText() }.getOrNull() ?: ""
            val msg = runCatching { JSONObject(errBody).getString("message") }.getOrDefault("HTTP $code")
            throw RuntimeException(msg)
        }

        val text = JSONObject(conn.inputStream.bufferedReader().readText())
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()

        return JSONObject(text)
    }
}
