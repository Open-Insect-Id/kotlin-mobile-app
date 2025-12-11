package org.openinsectid.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.openinsectid.app.data.ImageStore
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController

@Suppress("AssignedValueIsNeverRead")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController) {
    val ctx = LocalContext.current
    var items by remember { mutableStateOf(ImageStore.listHistory(ctx)) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("History") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(Color.Black)
        )
    }) { padding ->
        LazyColumn(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            if (items.isNotEmpty()) {
                items(items) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier
                            .weight(1f)
                            .clickable {
                                // open viewer
                                navController.navigate(Screen.ViewImage.createRoute(item.fileName))
                            }) {
                            AsyncImage(
                                model = item.file,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(item.fileName)
                                Text("Tap to view", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Column {
                            IconButton(onClick = {
                                // delete
                                ImageStore.delete(ctx, item.fileName)
                                items = ImageStore.listHistory(ctx)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(460.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No History")
                    }
                }
            }
        }
    }
}
