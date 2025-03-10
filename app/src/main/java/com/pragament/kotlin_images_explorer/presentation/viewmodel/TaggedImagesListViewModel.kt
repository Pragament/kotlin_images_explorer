package com.pragament.kotlin_images_explorer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pragament.kotlin_images_explorer.domain.model.ImageInfo
import com.pragament.kotlin_images_explorer.domain.repository.ImageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TaggedImagesListViewModel(
    private val repository: ImageRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TaggedImagesListState())
    val state: StateFlow<TaggedImagesListState> = _state.asStateFlow()

    init {
        loadProcessedImages()
    }

    private fun loadProcessedImages() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.getAllImages().collect { images ->
                val processedImages = images.filter { !it.extractedText.isNullOrEmpty() }
                _state.update { 
                    it.copy(
                        images = processedImages,
                        isLoading = false
                    )
                }
            }
        }
    }
}

data class TaggedImagesListState(
    val images: List<ImageInfo> = emptyList(),
    val isLoading: Boolean = true
) 