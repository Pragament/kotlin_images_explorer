package com.pragament.kotlin_images_explorer.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pragament.kotlin_images_explorer.domain.model.ImageInfo
import com.pragament.kotlin_images_explorer.domain.model.Tag
import com.pragament.kotlin_images_explorer.domain.repository.ImageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TaggedImagesViewModel(
    private val repository: ImageRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialTag: String = savedStateHandle.get<String>("tag") ?: ""

    private val _state = MutableStateFlow(TaggedImagesState())
    val state: StateFlow<TaggedImagesState> = _state.asStateFlow()

    init {
        loadImages(initialTag)
        observeTags()
    }

    private fun observeTags() {
        viewModelScope.launch {
            repository.getAllTags().collect { tags ->
                _state.update { it.copy(tags = tags) }
            }
        }
    }

    private fun loadImages(tag: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val imagesFlow = if (tag.isEmpty()) {
                repository.getAllImages()
            } else {
                repository.getImagesByTag(tag)
            }

            imagesFlow.collect { images ->
                val processedImages = images.filter { !it.extractedText.isNullOrEmpty() }
                _state.update {
                    it.copy(
                        images = processedImages,
                        isLoading = false,
                        currentTag = tag
                    )
                }
            }
        }
    }

    fun onTagSelected(tag: String) {
        loadImages(tag)
    }
}

data class TaggedImagesState(
    val images: List<ImageInfo> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val isLoading: Boolean = true,
    val currentTag: String = ""
) 