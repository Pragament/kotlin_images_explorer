package com.pragament.kotlin_images_explorer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pragament.kotlin_images_explorer.data.local.ScanMode
import com.pragament.kotlin_images_explorer.data.local.SettingsDataStore
import com.pragament.kotlin_images_explorer.domain.model.ImageInfo
import com.pragament.kotlin_images_explorer.domain.repository.ImageRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: ImageRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private var processingJob: Job? = null
    private var currentImageIndex = 0
    private var totalImages = 0
    private var processedImages = mutableListOf<ImageInfo>()

    init {
        viewModelScope.launch {
            settingsDataStore.scanMode.collect { mode ->
                _state.update { it.copy(scanMode = mode) }
            }
        }

        viewModelScope.launch {
            settingsDataStore.frameInterval.collect { interval ->
                _state.update { it.copy(frameInterval = interval) }
            }
        }

        viewModelScope.launch {
            settingsDataStore.selectedModel.collect { model ->
                _state.update { it.copy(selectedModel = model) }
            }
        }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            SettingsEvent.ScanAllDeviceImages -> scanAllDeviceImages()
            is SettingsEvent.ProcessSelectedImages -> processSelectedImages(event.uris)
            SettingsEvent.ToggleProcessing -> toggleProcessing()
        }
    }

    private fun scanAllDeviceImages() {
        viewModelScope.launch {
            _state.update { it.copy(isScanning = true) }
            repository.scanDeviceImages()
            repository.getAllImages().collect { images ->
                processedImages = images.toMutableList()
                totalImages = images.size
                startProcessing()
            }
        }
    }

    private fun processSelectedImages(uris: List<String>) {
        processedImages = uris.mapIndexed { index, uri ->
            ImageInfo(
                id = index.toLong(),
                uri = uri,
                displayName = uri.substringAfterLast("/"),
                dateAdded = System.currentTimeMillis(),
                extractedText = null,
                label = "Unknown",
                confidence = 0.0f,
                modelName = "N/A"
            )
        }.toMutableList()
        totalImages = processedImages.size
        startProcessing()
    }

    private fun startProcessing() {
        if (processingJob?.isActive == true) return

        processingJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    isProcessing = true,
                    isPaused = false
                )
            }

            processedImages.drop(currentImageIndex).forEach { image ->
                try {
                    val extractedText = repository.processImage(image.id, image.uri, _state.value.selectedModel)
                    repository.updateImageText(image.id, extractedText)
                    currentImageIndex++
                    updateProgress()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            _state.update {
                it.copy(
                    isProcessing = false,
                    isPaused = false
                )
            }
        }
    }

    private fun toggleProcessing() {
        if (_state.value.isPaused) {
            startProcessing()
        } else {
            processingJob?.cancel()
            _state.update { it.copy(isPaused = true) }
        }
    }

    private fun updateProgress() {
        if (totalImages > 0) {
            val progress = currentImageIndex.toFloat() / totalImages
            _state.update { it.copy(progress = progress) }
        }
    }

    fun setScanMode(mode: ScanMode) {
        viewModelScope.launch {
            settingsDataStore.setScanMode(mode)
        }
    }

    fun setFrameInterval(interval: Float) {
        viewModelScope.launch {
            settingsDataStore.setFrameInterval(interval)
        }
    }

    fun setModel(model: String) {
        viewModelScope.launch {
            settingsDataStore.setSelectedModel(model)
        }
    }
}

data class SettingsState(
    val isScanning: Boolean = false,
    val isProcessing: Boolean = false,
    val isPaused: Boolean = false,
    val progress: Float = 0f,
    val scanMode: ScanMode = ScanMode.ALL_DEVICE_IMAGES,
    val frameInterval: Float = 1.0f,
    val selectedModel: String = "mobilenet_v1" // Default model
)

sealed interface SettingsEvent {
    object ScanAllDeviceImages : SettingsEvent
    data class ProcessSelectedImages(val uris: List<String>) : SettingsEvent
    object ToggleProcessing : SettingsEvent
}
