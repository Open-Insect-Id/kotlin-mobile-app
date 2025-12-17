package org.openinsectid.app.ui.components

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.openinsectid.app.copyToClipboard
import org.openinsectid.app.data.AlphaResponse
import org.openinsectid.app.data.ImageSearchConfig
import org.openinsectid.app.utils.AlphaLLM

private val gson = Gson()

private fun parseAlphaResponse(raw: String): String {
    Log.d("AlphaLLM", raw)
    return try {
        val alpha = gson.fromJson(raw, AlphaResponse::class.java)

        if (alpha.status == "success" && alpha.response?.response != null) {
            val responseText = alpha.response.response

            responseText
                // Remove markdown reference-style links [[1]](url)
                .replace(Regex("\\[\\[\\d+]]\\([^)]*\\)"), "")
                // Remove generic markdown links [text](url)
                .replace(Regex("\\[[^]]*]\\([^)]*\\)"), "")
                // Remove simple HTML tags if any
                .replace(Regex("<[^>]+>"), "")
                .trim()
        } else {
            "Error: $raw"
        }
    } catch (_: JsonSyntaxException) {
        // Fallback to raw text if JSON parsing fails
        raw
    } catch (_: Exception) {
        raw
    }
}

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlphaQueryAnswer(
    alphaLLMApiKey: String,
    predictions: Map<String, String>?,
    modifier: Modifier = Modifier
) {
    var queryText by remember { mutableStateOf("") }
    var rawResponse by remember { mutableStateOf<String?>(null) }
    var parsedResponse by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    var elapsedTime by remember { mutableFloatStateOf(0f) }



    // Goofy colors
    val infiniteTransition = rememberInfiniteTransition(label = "colorCycle")

    val hue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hue"
    )

    val rainbowColor = Color.hsv(
        hue = hue,
        saturation = 1f,
        value = 1f
    )

    val coroutineScope = rememberCoroutineScope()
    val ctx = LocalContext.current

    // Auto-generate query from predictions and auto-query when predictions change
    LaunchedEffect(predictions) {
        predictions?.takeIf { it.isNotEmpty() }?.let { infos ->
            val insectName = infos.values.joinToString(" ")
            queryText = "Write a short description of this insect: $insectName: characteristics, habitat, and interesting facts"

            // Auto-launch query
            isLoading = true
            rawResponse = AlphaLLM.generateText(
                prompt = queryText,
                model = "Perplexity",
                apiKey = alphaLLMApiKey
            )

            // Parse response
            rawResponse?.let { response ->
                parsedResponse = parseAlphaResponse(response)
            }
            isLoading = false
        }
    }

    // Add timer state management
    var startTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            startTime = System.currentTimeMillis()
            while (isLoading) {
                elapsedTime = (System.currentTimeMillis() - startTime) / 1000f
                delay(100)
            }
        }
    }


    fun resetQuery() {
        predictions?.takeIf { it.isNotEmpty() }?.let { infos ->
            val insectName = infos.values.joinToString(" ")
            queryText =
                "Write a short description of this insect: $insectName: characteristics, habitat, and interesting facts"
        }
    }

    // Reload button handler
    fun reloadQuery() {
        coroutineScope.launch {
            isLoading = true
            elapsedTime = 0f
            rawResponse = AlphaLLM.generateText(
                prompt = queryText,
                model = "Perplexity",
                apiKey = alphaLLMApiKey
            )
            rawResponse?.let { response ->
                parsedResponse = parseAlphaResponse(response)
            }
            isLoading = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit query",
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            if (isEditMode) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent,
                            CircleShape
                        )
                        .clickable { isEditMode = !isEditMode }
                        .padding(4.dp)
                )

                Text(
                    text = "🤖 AlphaLLM",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isLoading) rainbowColor else MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (elapsedTime > 0) {
                    Text(
                        text = String.format("%.1fs", elapsedTime),
                        color = Color.Red,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(start = 4.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                ){
                    if (!isLoading) {
                        IconButton(onClick = { reloadQuery() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        CircularProgressIndicator(
                            modifier = modifier,
                            strokeWidth = 2.dp,
                            color = rainbowColor
                        )
                    }
                }
            }

            if (isEditMode){
                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    label = { Text("Query") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (isEditMode) {
                            IconButton(onClick = { resetQuery() } ) {
                                Icon(Icons.Default.Restore, "Query")
                            }
                        }
                    }
                )
            }

            parsedResponse?.takeIf { it.isNotBlank() }?.let { response ->
                Box {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "AlphaLLM Answer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = response,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    IconButton(
                        onClick = { ctx.copyToClipboard(response) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(35.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy Answer",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
