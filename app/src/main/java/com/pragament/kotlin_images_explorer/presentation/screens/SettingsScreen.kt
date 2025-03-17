package com.pragament.kotlin_images_explorer.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pragament.kotlin_images_explorer.data.local.ScanMode
import com.pragament.kotlin_images_explorer.presentation.viewmodel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()) // Enables scrolling
    ) {
        Text(
            text = "Scan Mode Settings",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Choose how to scan images",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                ScanModeOption(
                    title = "All Device Images",
                    description = "Scan and process all images on your device",
                    selected = state.scanMode == ScanMode.ALL_DEVICE_IMAGES,
                    onClick = { viewModel.setScanMode(ScanMode.ALL_DEVICE_IMAGES) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ScanModeOption(
                    title = "Multiple Images",
                    description = "Select multiple images to scan and process",
                    selected = state.scanMode == ScanMode.MULTIPLE_IMAGES,
                    onClick = { viewModel.setScanMode(ScanMode.MULTIPLE_IMAGES) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ScanModeOption(
                    title = "Single Image",
                    description = "Select one image at a time to scan and process",
                    selected = state.scanMode == ScanMode.SINGLE_IMAGE,
                    onClick = { viewModel.setScanMode(ScanMode.SINGLE_IMAGE) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Frame Extraction Interval",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                FrameIntervalOption(
                    title = "0.5 seconds",
                    selected = state.frameInterval == 0.5f,
                    onClick = { viewModel.setFrameInterval(0.5f) }
                )

                Spacer(modifier = Modifier.height(4.dp))

                FrameIntervalOption(
                    title = "1 second",
                    selected = state.frameInterval == 1.0f,
                    onClick = { viewModel.setFrameInterval(1.0f) }
                )

                Spacer(modifier = Modifier.height(4.dp))

                FrameIntervalOption(
                    title = "2 seconds",
                    selected = state.frameInterval == 2.0f,
                    onClick = { viewModel.setFrameInterval(2.0f) }
                )
            }
        }

        // Model Selection Section
        Text(
            text = "Model Selection",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Choose a model for processing images",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                ModelSelectionOption(
                    title = "MobileNet V1",
                    selected = state.selectedModel == "mobilenet_v1",
                    onClick = { viewModel.setModel("mobilenet_v1") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ModelSelectionOption(
                    title = "MobileNet V2",
                    selected = state.selectedModel == "mobilenet_v2",
                    onClick = { viewModel.setModel("mobilenet_v2") }
                )
            }
        }
    }
}

@Composable
private fun ScanModeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            RadioButton(
                selected = selected,
                onClick = onClick
            )
        }
    }
}

@Composable
private fun FrameIntervalOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
            RadioButton(
                selected = selected,
                onClick = onClick
            )
        }
    }
}

@Composable
private fun ModelSelectionOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
            RadioButton(
                selected = selected,
                onClick = onClick
            )
        }
    }
}
