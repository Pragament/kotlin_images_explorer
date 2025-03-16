package com.pragament.kotlin_images_explorer.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pragament.kotlin_images_explorer.domain.model.ImageInfo
import com.pragament.kotlin_images_explorer.domain.model.VideoFrame
import com.pragament.kotlin_images_explorer.domain.repository.ImageRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TaggedImagesListViewModel(
    private val repository: ImageRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TaggedImagesListState())
    val state: StateFlow<TaggedImagesListState> = _state.asStateFlow()

    init {
        loadProcessedData()
    }

    private fun loadProcessedData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // Fetch images
            val images = repository.getAllImages().first()
            val processedImages = images.filter { !it.extractedText.isNullOrEmpty() }

            // Fetch video frames
            val videoFrames = repository.getAllVideoFrames().first()
            val processedVideoFrames = videoFrames.filter { !it.extractedText.isNullOrEmpty() }

            _state.update {
                it.copy(
                    images = processedImages,
                    videoFrames = processedVideoFrames, // Update this line
                    isLoading = false
                )
            }
        }
    }

    fun applyFilters(
        minConfidence: Float?,
        maxConfidence: Float?,
        topN: Int?,
        searchQuery: String,
        filterModel: String,
        sortOption: String
    ) {
   //     Log.d("TaggedImagesListViewModel", "Applying filters: $minConfidence, $maxConfidence, $topN, $searchQuery, $filterModel, $sortOption")
        viewModelScope.launch {
            val filteredImages = repository.getAllImages().map { images ->
                images.filter { image ->
            //        Log.d("TaggedImagesListViewModel", "Filtering image: ${image.displayName} with confidence ${image.confidence} and model ${image.modelName} and label ${image.label} and text ${image.extractedText}tt ")
                    (minConfidence == null || image.confidence?.let { it >= minConfidence } == true) &&
                            (maxConfidence == null || image.confidence?.let { it <= maxConfidence } == true) &&
                            (searchQuery.isEmpty() || image.label?.contains(searchQuery, ignoreCase = true) == true) &&
                            (filterModel == "All" || image.modelName == filterModel)
                }.let { images ->
                    when (sortOption) {
                        "(High to Low)" -> images.sortedByDescending { it.confidence }
                        "(Low to High)" -> images.sortedBy { it.confidence }
                        else -> images
                    }
                }.take(topN ?: images.size)
            }.first() // Collect the first value from the Flow

            _state.update { it.copy(images = filteredImages, isLoading = false) }
        }
    }

    fun clearFilters() {
        viewModelScope.launch {
            loadProcessedData() //changed the whole loadProcessedImages() function to loadProcessedData() with addition for videos
        }
    }
}

data class TaggedImagesListState(
    val images: List<ImageInfo> = emptyList(),
    val isLoading: Boolean = true,
    val videoFrames: List<VideoFrame> = emptyList()
)