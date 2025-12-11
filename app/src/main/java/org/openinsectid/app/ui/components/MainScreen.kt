package org.openinsectid.app.ui.components

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

import coil3.compose.AsyncImage

@Composable
fun MainScreen(
    imageUri: Uri?,
    onImageSelected: (Uri?) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var showCamera by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var preview by remember { mutableStateOf<Preview?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showCamera = true
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onImageSelected(uri)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Image Display
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Selected image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No image selected")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gallery Button
            Button(
                onClick = { galleryLauncher.launch("image/*") }
            ) {
                Text("Select from Gallery")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Camera Section
            if (showCamera) {
                // Camera Preview
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            startCameraPreview(
                                context = ctx,
                                lifecycleOwner = lifecycleOwner,
                                previewView = this,
                                onReady = { p, ic ->
                                    preview = p
                                    imageCapture = ic
                                }
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Take Picture Button
                    Button(
                        onClick = {
                            imageCapture?.let { capture ->
                                val photoFile = File(
                                    context.cacheDir,
                                    "capture_${System.currentTimeMillis()}.jpg"
                                )
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                                capture.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onError(exception: ImageCaptureException) {
                                            // Handle error (add logging in production)
                                        }

                                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                            outputFileResults.savedUri?.let { uri ->
                                                onImageSelected(uri)
                                            }
                                        }
                                    }
                                )
                            }
                        },
                        enabled = imageCapture != null
                    ) {
                        Text("Take Picture")
                    }

                    // Close Camera Button
                    Button(
                        onClick = { showCamera = false }
                    ) {
                        Text("Close Camera")
                    }
                }
            } else {
                Button(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            showCamera = true
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                ) {
                    Text("Open Camera")
                }
            }
        }
    }
}

private fun startCameraPreview(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    onReady: (Preview, ImageCapture) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        // Preview use case
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        // Image capture use case
        val imageCapture = ImageCapture.Builder().build()

        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to lifecycle
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )

            onReady(preview, imageCapture)
        } catch (exc: Exception) {
            // Handle camera binding errors
        }
    }, ContextCompat.getMainExecutor(context))
}
