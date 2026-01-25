package org.openinsectid.app.utils

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AlphaLLM {
    suspend fun generateText(
        prompt: String,
        apiKey: String?,
        baseUrl: String,
        model: String = "auto",
    ): Pair<String, Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/text/generation")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 300_000
                readTimeout = 300_000
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }

            // Encode parameters for body
            val params = buildString {
                append("prompt=").append(URLEncoder.encode(prompt, "UTF-8"))
                append("&model=").append(URLEncoder.encode(model, "UTF-8"))
                append("&user_id=").append(0)
                append("&conv_id=").append(0)
                append("&stream=false")
            }

            connection.outputStream.use { it.write(params.toByteArray()) }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseText = stream?.bufferedReader()?.use { it.readText() } ?: ""

            if (responseCode == HttpURLConnection.HTTP_OK) {
                Pair(responseText, false)
            } else {
                Pair(responseText, true)
            }
        }

        catch (e: Exception) {
            Log.e("AlphaLLM", "Text generation failed", e)
            when (e) {
                is java.net.SocketTimeoutException -> "Timeout"
                is java.net.ConnectException -> "Failed to connect to server: $baseUrl"
                else -> "Unknown Error: $e"
            } to true
        }
    }
}
