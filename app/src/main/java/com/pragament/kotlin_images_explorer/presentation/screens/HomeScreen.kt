package com.pragament.kotlin_images_explorer.presentation.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pragament.kotlin_images_explorer.data.local.ScanMode
import com.pragament.kotlin_images_explorer.presentation.components.ProcessingIndicator
import com.pragament.kotlin_images_explorer.presentation.components.TagCloud
import com.pragament.kotlin_images_explorer.presentation.viewmodel.HomeEvent
import com.pragament.kotlin_images_explorer.presentation.viewmodel.HomeViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    onNavigateToFilteredImages: () -> Unit
) {
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

        if (hasStoragePermission) {
            viewModel.onEvent(HomeEvent.ScanImages)
        }
    }

    val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onEvent(HomeEvent.ProcessSelectedImages(listOf(uri.toString())))
        }
    }

    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.onEvent(HomeEvent.ProcessSelectedImages(uris.map { it.toString() }))
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onEvent(HomeEvent.ProcessSelectedVideos(listOf(uri.toString())))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Image Explorer") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Processing Indicator
            ProcessingIndicator(
                progress = state.progress,
                isPaused = state.isPaused,
                onPauseClick = { viewModel.onEvent(HomeEvent.PauseProcessing) },
                onResumeClick = { viewModel.onEvent(HomeEvent.ResumeProcessing) },
                onStopClick = { viewModel.onEvent(HomeEvent.StopProcessing) }
            )

            // Main Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
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
                        Text(
                            when (state.scanMode) {
                                ScanMode.ALL_DEVICE_IMAGES -> "Scan All Device Images"
                                ScanMode.MULTIPLE_IMAGES -> "Select Multiple Images"
                                ScanMode.SINGLE_IMAGE -> "Select Single Image"
                            }
                        )
                    }

                    Button(
                        onClick = { videoPickerLauncher.launch("video/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Scan Video")
                    }
                }

                state.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            // Tag Cloud
            if (state.tags.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .heightIn(max = 500.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
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
