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

            try {
                repository.getAllImages()
                    .map { images ->
                        images.filter { image ->
                            !image.extractedText.isNullOrBlank()
                        }
                    }
                    .collect { processedImages ->
                        println("DEBUG: Found ${processedImages.size} processed images")
                        processedImages.forEach { image ->
                            println("DEBUG: Image ${image.displayName} has tags: ${image.extractedText}")
                        }
                        
                        val videoFrames = repository.getAllVideoFrames().first()
                        val processedVideoFrames = videoFrames.filter { !it.extractedText.isNullOrEmpty() }

                        _state.update {
                            it.copy(
                                images = processedImages,
                                videoFrames = processedVideoFrames,
                                isLoading = false
                            )
                        }
                    }
            } catch (e: Exception) {
                println("DEBUG: Error loading data: ${e.message}")
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
                val filteredImages = repository.getAllImages()
                    .map { images ->
                        images.filter { image ->
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
                    }
                    .first()

                println("DEBUG: Applied filters, found ${filteredImages.size} images")
                _state.update { it.copy(images = filteredImages, isLoading = false) }
            } catch (e: Exception) {
                println("DEBUG: Error applying filters: ${e.message}")
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearFilters() {
        viewModelScope.launch {
            loadProcessedData()
        }
    }
}

data class TaggedImagesListState(
    val images: List<ImageInfo> = emptyList(),
    val isLoading: Boolean = true,
    val videoFrames: List<VideoFrame> = emptyList()
)