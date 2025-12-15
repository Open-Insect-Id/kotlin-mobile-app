package org.openinsectid.app.ui.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openinsectid.app.data.ImageStore
import org.openinsectid.app.utils.InferenceManager


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController) {
    val ctx = LocalContext.current
    var currentImage by remember { mutableStateOf<ImageStore.StoredImage?>(null) }
    var predictions by remember { mutableStateOf<Map<String, String>?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var debugInfo by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()


    // Load last saved image on compose
    LaunchedEffect(Unit) {
        currentImage = ImageStore.getLatest(ctx)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) navController.navigate(Screen.Camera.route)
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // copy to app storage and set as current
            val stored = ImageStore.storeUriToAppImages(ctx, it)
            currentImage = stored
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { /*Text("OpenInsectId")*/ },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.History.route) }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "History")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(Color.Black)
            )
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(10.dp)
            ) {
                FloatingActionButton(onClick = {
                    // open camera after permission
                    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA)
                        == android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        navController.navigate(Screen.Camera.route)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "Camera")
                }

                FloatingActionButton(onClick = { pickImageLauncher.launch("image/*") }) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Pick")
                }
            }
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (currentImage != null) {
                    AsyncImage(
                        model = currentImage!!.file,
                        contentDescription = "Selected image",
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        listOf(
                                            Color(0xFFFF0040),
                                            Color(0xFFFFA000),
                                            Color(0xFF00FF40),
                                            Color(0xFF00B0FF),
                                            Color(0xFF8000FF)
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .clip(CircleShape)
                                .clickable(enabled = !isAnalyzing) {
                                    coroutineScope.launch {
                                        currentImage?.let { image ->
                                            isAnalyzing = true
                                            debugInfo = "Analyzing..."

                                            try {
                                                val result = withContext(Dispatchers.IO) {
                                                    InferenceManager.runInferenceFromUri(ctx, image.file)
                                                }
                                                predictions = result
                                                debugInfo = if (result != null) {
                                                    "Got ${result.size} predictions"
                                                } else {
                                                    "Inference returned NULL"
                                                }
                                            } catch (e: Exception) {
                                                debugInfo = "ERROR: ${e.message}"
                                                e.printStackTrace()
                                            } finally {
                                                isAnalyzing = false
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isAnalyzing) {
                                Text(
                                    "Analyzing...",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    "What insect is that?",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.width(50.dp))

                        IconButton(
                            onClick = {
                                // remove current and delete file
                                ImageStore.delete(ctx, currentImage!!.fileName)
                                currentImage = ImageStore.getLatest(ctx)
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    MaterialTheme.colorScheme.errorContainer,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Discard",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }


                    debugInfo.takeIf { it.isNotEmpty() }?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Debug: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red
                        )
                    }

                    predictions?.takeIf { it.isNotEmpty() }?.let { preds ->
                        Spacer(Modifier.height(16.dp))
                        Text("Predictions:", style = MaterialTheme.typography.headlineSmall)
                        preds.forEach { (level, prediction) ->
                            Text(
                                text = "$level: $prediction",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } ?: predictions?.takeIf { it.isEmpty() }?.let {
                        Spacer(Modifier.height(16.dp))
                        Text("No predictions found", color = Color.Red)
                    }
                } else {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(460.dp), contentAlignment = Alignment.Center) {
                        Text("No image selected")
                    }
                }
            }
        }
    )
}
