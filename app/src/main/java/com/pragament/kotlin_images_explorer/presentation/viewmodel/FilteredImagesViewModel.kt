package com.pragament.kotlin_images_explorer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pragament.kotlin_images_explorer.domain.model.ImageInfo
import com.pragament.kotlin_images_explorer.domain.model.Tag
import com.pragament.kotlin_images_explorer.domain.repository.ImageRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FilteredImagesState(
    val selectedTags: List<Tag> = emptyList(),
    val filteredImages: List<ImageInfo> = emptyList(),
    val relatedTags: List<Tag> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class FilteredImagesEvent {
    data class SelectTag(val tag: Tag) : FilteredImagesEvent()
    data class RemoveTag(val tag: Tag) : FilteredImagesEvent()
    object ClearTags : FilteredImagesEvent()
}

class FilteredImagesViewModel(
    private val repository: ImageRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FilteredImagesState())
    val state: StateFlow<FilteredImagesState> = _state.asStateFlow()

    fun onEvent(event: FilteredImagesEvent) {
        when (event) {
            is FilteredImagesEvent.SelectTag -> addTag(event.tag)
            is FilteredImagesEvent.RemoveTag -> removeTag(event.tag)
            FilteredImagesEvent.ClearTags -> clearTags()
        }
    }

    private fun addTag(tag: Tag) {
        val currentTags = _state.value.selectedTags
        if (!currentTags.contains(tag)) {
            _state.update { it.copy(selectedTags = currentTags + tag) }
            updateFilteredImages()
        }
    }

    private fun removeTag(tag: Tag) {
        val currentTags = _state.value.selectedTags
        if (currentTags.contains(tag)) {
            _state.update { it.copy(selectedTags = currentTags - tag) }
            updateFilteredImages()
        }
    }

    private fun clearTags() {
        _state.update { it.copy(selectedTags = emptyList()) }
        updateFilteredImages()
    }

    private fun updateFilteredImages() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                
                // Get all images that contain any of the selected tags
                val selectedTags = _state.value.selectedTags
                val images = if (selectedTags.isEmpty()) {
                    repository.getAllImages().first()
                } else {
                    repository.getAllImages().first().filter { image ->
                        selectedTags.any { tag ->
                            image.extractedText?.contains(tag.word, ignoreCase = true) == true
                        }
                    }
                }

                // Generate related tags from the filtered images
                val relatedTags = generateRelatedTags(images)

                _state.update {
                    it.copy(
                        filteredImages = images,
                        relatedTags = relatedTags,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun generateRelatedTags(images: List<ImageInfo>): List<Tag> {
        val tagFrequency = mutableMapOf<String, Int>()
        val tagSources = mutableMapOf<String, MutableList<Long>>()

        // Count word frequency in filtered images
        images.forEach { image ->
            image.extractedText?.split(Regex("\\s+"))?.forEach { word ->
                if (word.length > 2) { // Skip very short words
                    tagFrequency[word] = (tagFrequency[word] ?: 0) + 1
                    tagSources.getOrPut(word) { mutableListOf() }.add(image.id)
                }
            }
        }

        // Convert to Tag objects, sorted by frequency
        return tagFrequency
            .map { (word, frequency) ->
                Tag(
                    word = word,
                    frequency = frequency,
                    sourceImages = tagSources[word] ?: emptyList()
                )
            }
            .sortedByDescending { it.frequency }
            .take(50) // Limit to top 50 tags
    }

    init {
        updateFilteredImages()
    }
}
