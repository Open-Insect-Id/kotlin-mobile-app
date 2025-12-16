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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.openinsectid.app.data.FetchedImage
import org.openinsectid.app.data.ImageSearchError
import org.openinsectid.app.data.ImageSearchState
import org.openinsectid.app.hasInternet
import org.openinsectid.app.utils.searchGoogleImages
import org.openinsectid.app.utils.searchInaturalistImages
import org.openinsectid.app.webSearch


@Composable
fun InaturalistImageSearch(
    query: String,
    onImageSelected: (FetchedImage) -> Unit
) {
    val ctx = LocalContext.current
    var state by remember { mutableStateOf<ImageSearchState>(ImageSearchState.Idle) }

    val normalQuery = normalizeTaxonQuery(query)


    LaunchedEffect(query) {
        if (query.isBlank()) return@LaunchedEffect

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
                Log.e("query", "QUERY: $normalQuery")

                searchInaturalistImages(normalQuery)
            }

            Log.d("ImageSearch", "SUCCESS query='$query' results=${images.size}")
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
        onClick = { webSearch(ctx, query) }
    ) {
        Text(
            text = "Search web",
            color = MaterialTheme.colorScheme.onBackground,
            textDecoration =  TextDecoration.Underline
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
                ImageResultsGrid(
                    images = s.images,
                    onImageClick = onImageSelected
                )
            }
        }
    }
}




@Composable
fun GoogleImageSearch(
    query: String,
    apiKey: String,
    cx: String,
    onImageSelected: (FetchedImage) -> Unit
) {
    var state by remember { mutableStateOf<ImageSearchState>(ImageSearchState.Idle) }

    LaunchedEffect(query) {
        if (query.isBlank()) return@LaunchedEffect

        state = ImageSearchState.Loading

        try {
            val images = withContext(Dispatchers.IO) {
                searchGoogleImages(query, apiKey, cx)
            }
            state = ImageSearchState.Success(images)
        } catch (t: Throwable) {
            state = ImageSearchState.Error(ImageSearchError.Unknown(t))
        }
    }

    when (val s = state) {
        ImageSearchState.Idle -> Unit
        ImageSearchState.Loading -> Box(
            Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }
        is ImageSearchState.Error -> Column {
            Text(s.error.userMessage, color = MaterialTheme.colorScheme.error)
            Text(s.error.debugMessage, style = MaterialTheme.typography.bodySmall)
        }
        is ImageSearchState.Success -> {
            if (s.images.isEmpty()) {
                Text("No images found\nQuery: $query")
            } else {
                ImageResultsGrid(s.images, onImageClick = onImageSelected)
            }
        }
    }
}


fun normalizeTaxonQuery(raw: String): String =
    raw.split(" ")
        .firstOrNull { it.length > 3 } ?: raw
