package com.pragament.kotlin_images_explorer.presentation.viewmodel

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

    fun loadProcessedData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                val images = repository.getAllImages().first()
                val videoFrames = repository.getAllVideoFrames().first()

                val processedImages = images.filter { !it.extractedText.isNullOrBlank() }
                val processedVideoFrames = videoFrames.filter { !it.extractedText.isNullOrBlank() }

                _state.update {
                    it.copy(
                        images = processedImages,
                        videoFrames = processedVideoFrames,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        images = emptyList(),
                        videoFrames = emptyList()
                    )
                }
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
        viewModelScope.launch {
            try {
                val allImages = repository.getAllImages().first()
                val allVideoFrames = repository.getAllVideoFrames().first()

                val filteredImages = allImages.filter { image ->
                    val confidenceMatch = (minConfidence == null || image.confidence?.let { it >= minConfidence } == true) &&
                            (maxConfidence == null || image.confidence?.let { it <= maxConfidence } == true)

                    val modelMatch = filterModel == "All" || image.modelName == filterModel

                    val searchMatch = searchQuery.isEmpty() ||
                            image.extractedText?.contains(searchQuery, ignoreCase = true) == true ||
                            image.label?.contains(searchQuery, ignoreCase = true) == true

                    confidenceMatch && modelMatch && searchMatch
                }.let { filtered ->
                    when (sortOption) {
                        "(High to Low)" -> filtered.sortedByDescending { it.confidence }
                        "(Low to High)" -> filtered.sortedBy { it.confidence }
                        else -> filtered
                    }
                }.take(topN ?: 50)

                val filteredFrames = allVideoFrames.filter { frame ->
                    val confidenceMatch = (minConfidence == null || frame.confidence?.let { it >= minConfidence } == true) &&
                            (maxConfidence == null || frame.confidence?.let { it <= maxConfidence } == true)

                    val modelMatch = filterModel == "All" || frame.modelName == filterModel

                    val searchMatch = searchQuery.isEmpty() ||
                            frame.extractedText?.contains(searchQuery, ignoreCase = true) == true ||
                            frame.label?.contains(searchQuery, ignoreCase = true) == true

                    confidenceMatch && modelMatch && searchMatch
                }.let { filtered ->
                    when (sortOption) {
                        "(High to Low)" -> filtered.sortedByDescending { it.confidence }
                        "(Low to High)" -> filtered.sortedBy { it.confidence }
                        else -> filtered
                    }
                }.take(topN ?: 50)

                _state.update {
                    it.copy(
                        images = filteredImages,
                        videoFrames = filteredFrames,
                        isLoading = false
                    )
                }

            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false)
                }
            }
        }
    }

    fun clearFilters() {
        loadProcessedData()
    }
}

data class TaggedImagesListState(
    val images: List<ImageInfo> = emptyList(),
    val isLoading: Boolean = true,
    val videoFrames: List<VideoFrame> = emptyList()
)
