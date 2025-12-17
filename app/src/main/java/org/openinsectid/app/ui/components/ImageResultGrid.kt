package org.openinsectid.app.ui.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.openinsectid.app.R
import org.openinsectid.app.data.FetchedImage


@Composable
fun ImageResultsGrid(
    images: List<FetchedImage>,
    onImageClick: (FetchedImage) -> Unit
) {
    Log.e("debug", "Got there images to show: $images")
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 1000.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(images) { img ->
            AsyncImage(
                model = img.thumbnail,
                contentDescription = img.title,
                modifier = Modifier
                    .padding(6.dp)
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onImageClick(img) },
                placeholder = painterResource(R.drawable.ic_broken_image),
                error = painterResource(R.drawable.ic_broken_image)
            )
        }
    }
}
