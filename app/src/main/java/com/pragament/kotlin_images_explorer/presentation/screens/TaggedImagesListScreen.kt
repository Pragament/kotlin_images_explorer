package com.pragament.kotlin_images_explorer.presentation.screens

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.pragament.kotlin_images_explorer.domain.model.ImageInfo
import com.pragament.kotlin_images_explorer.presentation.viewmodel.TaggedImagesListViewModel
import com.pragament.kotlin_images_explorer.ui.KotlinImagesExplorerIcons
import org.koin.androidx.compose.koinViewModel

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scanned Results") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter UI
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
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
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
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
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
                      //       Apply filters
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

            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                if (state.images.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = KotlinImagesExplorerIcons.Search,
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
                        items(state.images) { image ->
                            ScannedImageCard(
                                image = image,
                                onTagClick = onTagSelected
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalGlideComposeApi::class)
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
                            .take(15)

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