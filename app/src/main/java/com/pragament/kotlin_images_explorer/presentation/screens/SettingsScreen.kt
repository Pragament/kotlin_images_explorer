package com.pragament.kotlin_images_explorer.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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