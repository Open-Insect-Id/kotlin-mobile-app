package org.openinsectid.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.openinsectid.app.data.DdgImage
import org.openinsectid.app.data.ImageSearchState
import org.openinsectid.app.data.searchDuckDuckGoImages

@Composable
fun DuckDuckGoImageSearch(
    query: String,
    onImageSelected: (DdgImage) -> Unit
) {
    var state by remember { mutableStateOf<ImageSearchState>(ImageSearchState.Idle) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(query) {
        if (query.isBlank()) return@LaunchedEffect

        state = ImageSearchState.Loading

        try {
            val images = withContext(Dispatchers.IO) {
                searchDuckDuckGoImages(query)
            }
            state = ImageSearchState.Success(images)
        } catch (_: Exception) {
            state = ImageSearchState.Error("No internet connection")
        }
    }

    when (state) {
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
            Text(
                text = (state as ImageSearchState.Error).message,
                color = MaterialTheme.colorScheme.error
            )
        }

        is ImageSearchState.Success -> {
            val images = (state as ImageSearchState.Success).images
            if (images.isEmpty()) {
                Text("No images found")
            } else {
                ImageResultsGrid(images, onImageSelected)
            }
        }
    }
}
