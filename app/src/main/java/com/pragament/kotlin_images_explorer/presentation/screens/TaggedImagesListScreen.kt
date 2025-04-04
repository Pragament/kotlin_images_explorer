package com.pragament.kotlin_images_explorer.presentation.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pragament.kotlin_images_explorer.domain.model.ImageInfo
import com.pragament.kotlin_images_explorer.domain.model.VideoFrame
import com.pragament.kotlin_images_explorer.presentation.viewmodel.TaggedImagesListViewModel
import org.koin.androidx.compose.koinViewModel
import kotlin.math.log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaggedImagesListScreen(
    onTagSelected: (String) -> Unit,
    viewModel: TaggedImagesListViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var minConfidence by remember { mutableStateOf("") }
    var maxConfidence by remember { mutableStateOf("") }
    var topN by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf("None") }
    var filterModel by remember { mutableStateOf("All") }

    var sortDropdownExpanded by remember { mutableStateOf(false) }
    var filterDropdownExpanded by remember { mutableStateOf(false) }

    var isFilterVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scanned Results") },
                actions = {
                    IconButton(onClick = { isFilterVisible = !isFilterVisible }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Toggle Filter"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Conditionally show filter UI based on isFilterVisible
            if (isFilterVisible) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search by label") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = minConfidence,
                            onValueChange = { minConfidence = it },
                            label = { Text("Min Confidence") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                        )
                        TextField(
                            value = maxConfidence,
                            onValueChange = { maxConfidence = it },
                            label = { Text("Max Confidence") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                        )
                    }
                    TextField(
                        value = topN,
                        onValueChange = { topN = it },
                        label = { Text("Top-N Results") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = sortOption,
                                onValueChange = {},
                                label = { Text("Confidence Sort") },
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { sortDropdownExpanded = true }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Dropdown"
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(
                                expanded = sortDropdownExpanded,
                                onDismissRequest = { sortDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(onClick = {
                                    sortOption = "None"
                                    sortDropdownExpanded = false
                                }, text = { Text("None") })

                                DropdownMenuItem(onClick = {
                                    sortOption = "(High to Low)"
                                    sortDropdownExpanded = false
                                }, text = { Text("(High to Low)") })

                                DropdownMenuItem(onClick = {
                                    sortOption = "(Low to High)"
                                    sortDropdownExpanded = false
                                }, text = { Text("(Low to High)") })
                            }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = filterModel,
                                onValueChange = {},
                                label = { Text("Model Filter") },
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { filterDropdownExpanded = true }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Dropdown"
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(
                                expanded = filterDropdownExpanded,
                                onDismissRequest = { filterDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(onClick = {
                                    filterModel = "All"
                                    filterDropdownExpanded = false
                                }, text = { Text("All") })
                                DropdownMenuItem(onClick = {
                                    filterModel = "mobilenet_v1"
                                    filterDropdownExpanded = false
                                }, text = { Text("mobilenet_v1") })
                                DropdownMenuItem(onClick = {
                                    filterModel = "mobilenet_v2"
                                    filterDropdownExpanded = false
                                }, text = { Text("mobilenet_v2") })
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                // Apply filters
                                viewModel.applyFilters(
                                    minConfidence.toFloatOrNull(),
                                    maxConfidence.toFloatOrNull(),
                                    topN.toIntOrNull(),
                                    searchQuery,
                                    filterModel,
                                    sortOption
                                )
                            }
                        ) {
                            Text("Apply Filters")
                        }
                        Button(
                            onClick = {
                                // Clear filters
                                searchQuery = ""
                                minConfidence = ""
                                maxConfidence = ""
                                topN = ""
                                sortOption = "Confidence (High to Low)"
                                filterModel = "All"
                                viewModel.clearFilters()
                            }
                        ) {
                            Text("Clear Filters")
                        }
                    }
                }
            }

            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                if (state.images.isEmpty() && state.videoFrames.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No processed images yet",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Process some images in the Home tab to see them here",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Optional: Header for Images
                        if (state.images.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Scanned Images",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        // List of images
                        items(state.images) { image ->
                            Log.d("FRAMES", "Processing image: $image")
                            ScannedImageCard(
                                image = image,
                                onTagClick = onTagSelected
                            )
                        }

                        // Optional: Header for Video Frames
                        if (state.videoFrames.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Video Frames",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        // List of video frames
                        items(state.videoFrames) { frame ->
                            Log.d("FRAMES", "Processing frame: $frame")
                            ScannedVideoFrameCard(
                                frame = frame,
                                onTagClick = onTagSelected
                            )
                        }
                    }



                }
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScannedImageCard(
    image: ImageInfo,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                AsyncImage(
                    model = image.uri,
                    contentDescription = image.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(MaterialTheme.shapes.small)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = image.displayName,
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (!image.extractedText.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))

                        val words = image.extractedText.split(Regex("\\s+"))
                            .filter { it.length > 2 }
                            .distinct()
                            .take(10)

                        if (words.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                words.forEach { word ->
                                    AssistChip(
                                        onClick = { onTagClick(word) },
                                        label = { Text(word) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScannedVideoFrameCard(
    frame: VideoFrame,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            AsyncImage(
                model = frame.frameUri,
                contentDescription = "Video Frame",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f) // Adjust aspect ratio for video frames
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (!frame.extractedText.isNullOrEmpty()) {
                Text(
                    text = frame.extractedText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}