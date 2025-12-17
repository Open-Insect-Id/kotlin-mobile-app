package org.openinsectid.app.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.openinsectid.app.data.FetchedImage
import org.openinsectid.app.data.ImageSearchError
import org.openinsectid.app.data.ImageSearchState
import org.openinsectid.app.hasInternet
import org.openinsectid.app.utils.searchImages
import org.openinsectid.app.webSearch

@Composable
fun ImageSearch(
    query: String,
    onImageClick: (FetchedImage) -> Unit
) {
    val ctx = LocalContext.current
    var state by remember { mutableStateOf<ImageSearchState>(ImageSearchState.Idle) }

    val normalQuery = remember(query) {
        query.trim()
    }

    LaunchedEffect(normalQuery) {
        if (normalQuery.isBlank()) return@LaunchedEffect

        if (!ctx.hasInternet()) {
            state = ImageSearchState.Error(
                ImageSearchError.Network(
                    IllegalStateException("No internet connection")
                )
            )
            return@LaunchedEffect
        }

        state = ImageSearchState.Loading

        try {
            val images = withContext(Dispatchers.IO) {
                Log.d("ImageSearch", "QUERY: $normalQuery")
                searchImages(normalQuery)
            }

            Log.d("ImageSearch", "SUCCESS query='$normalQuery' results=${images.size}")
            state = ImageSearchState.Success(images)

        } catch (e: ImageSearchError) {
            Log.e("ImageSearch", e.debugMessage, e)
            state = ImageSearchState.Error(e)

        } catch (t: Throwable) {
            Log.e("ImageSearch", t.message, t)
            state = ImageSearchState.Error(
                ImageSearchError.Unknown(t)
            )
        }
    }

    TextButton(
        onClick = { webSearch(ctx, normalQuery) }
    ) {
        Text(
            text = buildAnnotatedString {
                append("Search web: ")
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic)
                ) {
                    append(normalQuery)
                }
            },
            color = Color(0xFF6F6FFF),
            textDecoration = TextDecoration.Underline
        )
    }

    when (val s = state) {
        ImageSearchState.Idle -> Unit

        ImageSearchState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is ImageSearchState.Error -> {
            Column {
                Text(
                    text = s.error.userMessage,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = s.error.debugMessage,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        is ImageSearchState.Success -> {
            if (s.images.isEmpty()) {
                Text("No images found\nQuery: $normalQuery")
            } else {
                Log.e("debug",s.images.toString())
                ImageResultsGrid(
                    images = s.images,
                    onImageClick = onImageClick
                )
            }
        }
    }
}
