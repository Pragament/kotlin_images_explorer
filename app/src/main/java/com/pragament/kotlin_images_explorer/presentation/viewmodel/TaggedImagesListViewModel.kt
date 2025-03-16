package com.pragament.kotlin_images_explorer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pragament.kotlin_images_explorer.domain.model.ImageInfo
import com.pragament.kotlin_images_explorer.domain.model.VideoFrame
import com.pragament.kotlin_images_explorer.domain.repository.ImageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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
}

data class TaggedImagesListState(
    val images: List<ImageInfo> = emptyList(),
    val isLoading: Boolean = true,
    val videoFrames: List<VideoFrame> = emptyList()
) 