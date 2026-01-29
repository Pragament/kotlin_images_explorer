package com.pragament.kotlin_images_explorer.presentation.screens

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn // Changed to LazyColumn for scrolling
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pragament.kotlin_images_explorer.data.local.ScanMode
import com.pragament.kotlin_images_explorer.presentation.components.ProcessingIndicator
import com.pragament.kotlin_images_explorer.presentation.components.TagCloud
import com.pragament.kotlin_images_explorer.presentation.viewmodel.HomeEvent
import com.pragament.kotlin_images_explorer.presentation.viewmodel.HomeViewModel
import com.pragament.kotlin_images_explorer.YoloDetector // Import your Yolo classes
import com.pragament.kotlin_images_explorer.BoundingBox
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    onNavigateToFilteredImages: () -> Unit
) {
    // 1. GET DATA FROM VIEWMODEL
    val boxes = viewModel.detectedObjects
    val currentBitmap = viewModel.currentImageBitmap
    val state by viewModel.state.collectAsState()

    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
        }
        if (hasStoragePermission) viewModel.onEvent(HomeEvent.ScanImages)
    }

    val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) viewModel.onEvent(HomeEvent.ProcessSelectedImages(listOf(uri.toString())))
    }

    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) viewModel.onEvent(HomeEvent.ProcessSelectedImages(uris.map { it.toString() }))
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) viewModel.onEvent(HomeEvent.ProcessSelectedVideos(listOf(uri.toString())))
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Image Explorer") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                // Processing Indicator
                ProcessingIndicator(
                    progress = state.progress,
                    isPaused = state.isPaused,
                    onPauseClick = { viewModel.onEvent(HomeEvent.PauseProcessing) },
                    onResumeClick = { viewModel.onEvent(HomeEvent.ResumeProcessing) },
                    onStopClick = { viewModel.onEvent(HomeEvent.StopProcessing) }
                )
            }

            item {
                // Button Section
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!state.isScanning && !state.isProcessing && !state.isPaused) {
                        Button(
                            onClick = {
                                when (state.scanMode) {
                                    ScanMode.ALL_DEVICE_IMAGES -> {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
                                        } else {
                                            permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                                        }
                                    }
                                    ScanMode.MULTIPLE_IMAGES -> multiplePhotoPickerLauncher.launch("image/*")
                                    ScanMode.SINGLE_IMAGE -> singlePhotoPickerLauncher.launch("image/*")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(when (state.scanMode) {
                                ScanMode.ALL_DEVICE_IMAGES -> "Scan All Device Images"
                                ScanMode.MULTIPLE_IMAGES -> "Select Multiple Images"
                                ScanMode.SINGLE_IMAGE -> "Select Single Image"
                            })
                        }

                        Button(
                            onClick = { videoPickerLauncher.launch("video/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Scan Video")
                        }
                    }

                    state.error?.let { error ->
                        Text(text = error, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            item {
                if (currentBitmap != null) {
                    DetectionResultCard(bitmap = currentBitmap, boxes = boxes)
                }
            }

            item {
                // Tag Cloud
                if (state.tags.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .heightIn(max = 500.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "Tags",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            TagCloud(
                                tags = state.tags,
                                onTagClick = { tag ->
                                    viewModel.onEvent(HomeEvent.SelectTag(tag))
                                    onNavigateToFilteredImages()
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
@Composable
fun DetectionResultCard(bitmap: Bitmap, boxes: List<BoundingBox>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(300.dp) // Set a fixed height for preview
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Show the Image
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Detected Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds // Fill the box
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val scaleX = size.width / 640f
                val scaleY = size.height / 640f

                boxes.forEach { box ->
                    // Draw Box
                    drawRect(
                        color = Color.Red,
                        topLeft = Offset(box.x1 * scaleX, box.y1 * scaleY),
                        size = Size(box.w * scaleX, box.h * scaleY),
                        style = Stroke(width = 5f)
                    )
                }
            }
        }
    }
}