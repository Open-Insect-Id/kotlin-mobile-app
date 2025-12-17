package org.openinsectid.app.utils

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AlphaLLM {
    private const val BASE_URL = "http://de5.azurhosts.com:25692"

    suspend fun generateText(
        prompt: String,
        apiKey: String,
        model: String = "auto",
        userId: Int? = null,
        conversationId: Int? = null,
    ): String? = withContext(Dispatchers.IO) {
        try {
            val encodedPrompt = URLEncoder.encode(prompt, "UTF-8")
            val encodedApiKey = URLEncoder.encode(apiKey, "UTF-8")

            val url = "$BASE_URL/generate/text?" +
                    "prompt=$encodedPrompt&" +
                    "model=$model&" +
                    "api_key=$encodedApiKey" +
                    (userId?.let { "&user_id=$it" } ?: "") +
                    (conversationId?.let { "&conversation_id=$it" } ?: "")
            Log.d("AlphaLLM", "Query: $url")

            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 300000 // I set big timeouts cause Yoan's api is kinda slow
            connection.readTimeout = 300000

            val responseCode = connection.responseCode
            return@withContext if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                Log.e("AlphaLLM", "HTTP $responseCode: ${connection.responseMessage}")
                when (responseCode) {
                    401 -> "401 Unauthorized (check api key)"
                    403 -> "403 Forbidden (check api key)"
                    404 -> "404 Not Found"
                    418 -> "I'm a teapot"
                    else -> "$responseCode Unknown Error"
                }
            }
        }
        catch (e: Exception) {
            Log.e("AlphaLLM", "Text generation failed", e)
            when (e) {
                is java.net.SocketTimeoutException -> "Timeout"
                else -> null
            }
        }
    }
}
