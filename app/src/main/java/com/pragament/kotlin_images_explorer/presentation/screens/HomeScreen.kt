package com.pragament.kotlin_images_explorer.presentation.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pragament.kotlin_images_explorer.data.local.ScanMode
import com.pragament.kotlin_images_explorer.presentation.components.ProcessingStatus
import com.pragament.kotlin_images_explorer.presentation.viewmodel.HomeEvent
import com.pragament.kotlin_images_explorer.presentation.viewmodel.HomeViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!state.isScanning && !state.isProcessing && !state.isPaused) {
            Button(
                onClick = {
                    when (state.scanMode) {
                        ScanMode.ALL_DEVICE_IMAGES -> viewModel.onEvent(HomeEvent.ScanImages)
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
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { videoPickerLauncher.launch("video/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Scan Video")
            }
        }

        // Show a loading message when scanning is in progress
        if (state.isScanning) {
            Text("Scanning and processing video...")
        }

        if (state.scanMode == ScanMode.ALL_DEVICE_IMAGES) {
            ProcessingStatus(
                progress = state.progress,
                isProcessing = state.isProcessing,
                isPaused = state.isPaused,
                onStartProcessing = { viewModel.onEvent(HomeEvent.StartProcessing) },
                onPauseProcessing = { viewModel.onEvent(HomeEvent.PauseProcessing) },
                onResumeProcessing = { viewModel.onEvent(HomeEvent.ResumeProcessing) },
                onStopProcessing = { viewModel.onEvent(HomeEvent.StopProcessing) }
            )
        } else {
            if (state.isProcessing || state.isPaused) {
                ProcessingStatus(
                    progress = state.progress,
                    isProcessing = state.isProcessing,
                    isPaused = state.isPaused,
                    onPauseProcessing = { viewModel.onEvent(HomeEvent.PauseProcessing) },
                    onResumeProcessing = { viewModel.onEvent(HomeEvent.ResumeProcessing) },
                    onStopProcessing = { viewModel.onEvent(HomeEvent.StopProcessing) }
                )
            }
        }
    }
}
