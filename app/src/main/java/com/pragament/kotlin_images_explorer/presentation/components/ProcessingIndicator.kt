package com.pragament.kotlin_images_explorer.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pragament.kotlin_images_explorer.presentation.viewmodel.ProcessingProgress
import com.pragament.kotlin_images_explorer.presentation.viewmodel.ProcessingType

@Composable
fun ProcessingIndicator(
    progress: ProcessingProgress,
    isPaused: Boolean,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = progress.type != ProcessingType.NONE,
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Processing type and item info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (progress.type) {
                            ProcessingType.IMAGES -> "Processing Images"
                            ProcessingType.VIDEOS -> "Processing Videos"
                            ProcessingType.NONE -> ""
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${progress.current}/${progress.total}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Progress bar
                LinearProgressIndicator(
                    progress = { if (progress.total > 0) progress.current.toFloat() / progress.total else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                )

                // Current item
                if (progress.currentItem.isNotEmpty()) {
                    Text(
                        text = "Processing: ${progress.currentItem}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Control buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isPaused) {
                        FilledTonalButton(
                            onClick = onResumeClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Resume")
                        }
                    } else {
                        FilledTonalButton(
                            onClick = onPauseClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Pause")
                        }
                    }
                    
                    OutlinedButton(
                        onClick = onStopClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Stop")
                    }
                }
            }
        }
    }
}
