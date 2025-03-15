package com.pragament.kotlin_images_explorer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pragament.kotlin_images_explorer.data.local.ScanMode
import com.pragament.kotlin_images_explorer.data.local.SettingsDataStore
import com.pragament.kotlin_images_explorer.domain.model.ImageInfo
import com.pragament.kotlin_images_explorer.domain.model.Tag
import com.pragament.kotlin_images_explorer.domain.repository.ImageRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: ImageRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(HomeViewState())
    val state: StateFlow<HomeViewState> = _state.asStateFlow()

    private var processingJob: Job? = null
    private var currentImageIndex = 0
    private var totalImages = 0
    private var processedImages = mutableListOf<ImageInfo>()

    init {
        observeTags()
        observeRecentScans()
        viewModelScope.launch {
            settingsDataStore.scanMode.collect { mode ->
                _state.update { it.copy(scanMode = mode) }
            }
        }
    }

    private fun observeTags() {
        viewModelScope.launch {
            repository.getAllTags().collect { tags ->
                _state.update { it.copy(tags = tags) }
            }
        }
    }

    private fun observeRecentScans() {
        viewModelScope.launch {
            repository.getAllImages().collect { images ->
                _state.update { it.copy(recentScans = images) }
            }
        }
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.StartProcessing -> startProcessing()
            HomeEvent.PauseProcessing -> pauseProcessing()
            HomeEvent.ResumeProcessing -> resumeProcessing()
            HomeEvent.StopProcessing -> stopProcessing()
            HomeEvent.ScanImages -> scanImages()
            is HomeEvent.ProcessSelectedImages -> processSelectedImages(event.uris)
        }
    }

    private fun scanImages() {
        viewModelScope.launch {
            _state.update { it.copy(isScanning = true) }
            repository.scanDeviceImages()

            try {
                val images = repository.getAllImages().first()
                processedImages = images.filter { it.extractedText == null }.toMutableList()
                totalImages = processedImages.size
                currentImageIndex = 0

                _state.update {
                    it.copy(
                        isScanning = false,
                        isProcessing = false,
                        progress = 0f
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                resetProcessingState()
            }
        }
    }

    private fun startProcessing() {
        if (processingJob?.isActive == true) return

        processingJob = viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, isPaused = false) }

            val selectedModel = settingsDataStore.selectedModel.first() // Get model name

            if (processedImages.isEmpty()) {
                val images = repository.getAllImages().first()
                processedImages = images.filter { it.extractedText == null }.toMutableList()
                totalImages = processedImages.size
                currentImageIndex = 0
            }

            if (processedImages.isNotEmpty()) {
                processedImages.drop(currentImageIndex).forEach { image ->
                    try {
                        val extractedText = repository.processImage(image.id, image.uri, selectedModel)
                        repository.updateImageText(image.id, extractedText)
                        currentImageIndex++
                        updateProgress()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (currentImageIndex >= totalImages) resetProcessingState()
            } else resetProcessingState()
        }
    }

    private fun processSelectedImages(uris: List<String>) {
        viewModelScope.launch {
            _state.update { it.copy(isScanning = true) }

            val selectedModel = settingsDataStore.selectedModel.first() // Get model name

            processedImages = uris.mapIndexed { index, uri ->
                ImageInfo(
                    id = System.currentTimeMillis() + index,
                    uri = uri,
                    displayName = uri.substringAfterLast("/"),
                    dateAdded = System.currentTimeMillis(),
                    extractedText = null,
                    label = "Unknown",
                    confidence = 0.0f,
                    modelName = selectedModel
                )
            }.toMutableList()

            processedImages.forEach { image -> repository.insertImage(image) }
            totalImages = processedImages.size
            currentImageIndex = 0

            _state.update { it.copy(isScanning = false, isProcessing = true) }
            startProcessing()
        }
    }


    private fun pauseProcessing() {
        processingJob?.cancel()
        _state.update {
            it.copy(
                isProcessing = false,
                isPaused = true
            )
        }
    }

    private fun resumeProcessing() {
        startProcessing()
    }

    private fun stopProcessing() {
        processingJob?.cancel()
        resetProcessingState()
    }

    private fun resetProcessingState() {
        processingJob?.cancel() // Cancel any ongoing processing
        currentImageIndex = 0
        processedImages.clear()
        totalImages = 0
        _state.update {
            it.copy(
                isProcessing = false,
                isPaused = false,
                progress = 0f
            )
        }
    }

    private fun updateProgress() {
        if (totalImages > 0) {
            val progress = currentImageIndex.toFloat() / totalImages
            if (progress >= 1f) {
                // If progress is 100%, reset the processing state
                resetProcessingState()
            } else {
                _state.update {
                    it.copy(
                        progress = progress,
                        isProcessing = true,
                        isPaused = false
                    )
                }
            }
        }
    }
}

data class HomeViewState(
    val isScanning: Boolean = false,
    val isProcessing: Boolean = false,
    val isPaused: Boolean = false,
    val progress: Float = 0f,
    val scanMode: ScanMode = ScanMode.ALL_DEVICE_IMAGES,
    val tags: List<Tag> = emptyList(),
    val recentScans: List<ImageInfo> = emptyList()
)

sealed interface HomeEvent {
    data object StartProcessing : HomeEvent
    data object PauseProcessing : HomeEvent
    data object ResumeProcessing : HomeEvent
    data object StopProcessing : HomeEvent
    data object ScanImages : HomeEvent
    data class ProcessSelectedImages(val uris: List<String>) : HomeEvent
} 