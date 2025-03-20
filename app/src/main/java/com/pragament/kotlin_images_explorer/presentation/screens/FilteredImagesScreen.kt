package com.pragament.kotlin_images_explorer.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pragament.kotlin_images_explorer.domain.model.ImageInfo
import com.pragament.kotlin_images_explorer.presentation.components.TagCloud
import com.pragament.kotlin_images_explorer.presentation.viewmodel.FilteredImagesEvent
import com.pragament.kotlin_images_explorer.presentation.viewmodel.FilteredImagesViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilteredImagesScreen(
    viewModel: FilteredImagesViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    val selectedTagsCollapsed = remember { mutableStateOf(false) }
    val relatedTagsCollapsed = remember { mutableStateOf(false) }

    // The height of the TagCloud section
    val tagCloudHeight = LocalDensity.current.run { 50.dp.toPx() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Filtered Images") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Selected Tags Section
            if (state.selectedTags.isNotEmpty()) {
                // Show More/Show Less Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Selected Tags",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (selectedTagsCollapsed.value) "Show More" else "Show Less",
                        modifier = Modifier
                            .clickable { selectedTagsCollapsed.value = !selectedTagsCollapsed.value }
                            .padding(start = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Box(modifier = Modifier.heightIn(max = tagCloudHeight.dp)) {
                    if (!selectedTagsCollapsed.value) {
                        TagCloud(
                            tags = state.selectedTags,
                            onTagClick = { tag -> viewModel.onEvent(FilteredImagesEvent.RemoveTag(tag)) },
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            // Related Tags Section
            if (state.relatedTags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Related Tags",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (relatedTagsCollapsed.value) "Show More" else "Show Less",
                        modifier = Modifier
                            .clickable { relatedTagsCollapsed.value = !relatedTagsCollapsed.value }
                            .padding(start = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Box(modifier = Modifier.heightIn(max = tagCloudHeight.dp)) {
                    if (!relatedTagsCollapsed.value) {
                        TagCloud(
                            tags = state.relatedTags,
                            onTagClick = { tag -> viewModel.onEvent(FilteredImagesEvent.SelectTag(tag)) },
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }
            }

            // Filtered Images Grid
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 128.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.filteredImages) { image ->
                        ImageCard(
                            image = image,
                            onClick = { /*  */ }
                        )
                    }
                }
            }

            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ImageCard(
    image: ImageInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        AsyncImage(
            model = image.uri,
            contentDescription = image.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}
