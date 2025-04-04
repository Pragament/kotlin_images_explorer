package com.pragament.kotlin_images_explorer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pragament.kotlin_images_explorer.data.local.ScanMode
import com.pragament.kotlin_images_explorer.data.local.SettingsDataStore
import com.pragament.kotlin_images_explorer.domain.model.ImageInfo
import com.pragament.kotlin_images_explorer.domain.model.Tag
import com.pragament.kotlin_images_explorer.domain.model.VideoFrame
import com.pragament.kotlin_images_explorer.domain.repository.ImageRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProcessingProgress(
    val current: Int = 0,
    val total: Int = 0,
    val currentItem: String = "",
    val type: ProcessingType = ProcessingType.NONE
)

enum class ProcessingType {
    NONE,
    IMAGES,
    VIDEOS
}

data class HomeViewState(
    val isScanning: Boolean = false,
    val isProcessing: Boolean = false,
    val isPaused: Boolean = false,
    val progress: ProcessingProgress = ProcessingProgress(),
    val scanMode: ScanMode = ScanMode.ALL_DEVICE_IMAGES,
    val recentScans: List<ImageInfo> = emptyList(),
    val videoFrames: List<VideoFrame> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val error: String? = null
)

sealed class HomeEvent {
    data object StartProcessing : HomeEvent()
    data object PauseProcessing : HomeEvent()
    data object ResumeProcessing : HomeEvent()
    data object StopProcessing : HomeEvent()
    data object ScanImages : HomeEvent()
    data object ScanVideos : HomeEvent()
    data class ProcessSelectedImages(val uris: List<String>) : HomeEvent()
    data class ProcessSelectedVideos(val uris: List<String>) : HomeEvent()
    data class SelectTag(val tag: Tag) : HomeEvent()
}

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
        observeVideoFrames()
        viewModelScope.launch {
            settingsDataStore.scanMode.collect { mode ->
                _state.update { it.copy(scanMode = mode) }
            }
        }
    }

    private fun observeTags() {
        viewModelScope.launch {
            repository.getAllTags()
                .collect { tags ->
                    // Filter out empty tags and sort by frequency
                    val validTags = tags
                        .filter { it.word.isNotBlank() }
                        .sortedByDescending { it.frequency }
                    println("DEBUG: Observing ${validTags.size} valid tags")
                    _state.update { it.copy(tags = validTags) }
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

    private fun observeVideoFrames() {
        viewModelScope.launch {
            repository.getAllVideoFrames().collect { frames ->
                _state.update { it.copy(videoFrames = frames) }
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
            HomeEvent.ScanVideos -> scanVideos()
            is HomeEvent.ProcessSelectedImages -> processSelectedImages(event.uris)
            is HomeEvent.ProcessSelectedVideos -> processSelectedVideos(event.uris)
            is HomeEvent.SelectTag -> navigateToFilteredImages(event.tag)
        }
    }

    private fun scanVideos() {
        viewModelScope.launch {
            try {
                _state.update { 
                    it.copy(
                        isScanning = true,
                        progress = ProcessingProgress(type = ProcessingType.VIDEOS)
                    )
                }
                repository.scanDeviceVideos()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            } finally {
                _state.update { it.copy(isScanning = false) }
            }
        }
    }

    private fun processSelectedVideos(uris: List<String>) {
        viewModelScope.launch {
            try {
                _state.update { 
                    it.copy(
                        isProcessing = true,
                        progress = ProcessingProgress(
                            total = uris.size,
                            type = ProcessingType.VIDEOS
                        )
                    )
                }

                val frameInterval = settingsDataStore.frameInterval.first()

                uris.forEachIndexed { index, videoUri ->
                    if (_state.value.isPaused) {
                        return@forEachIndexed
                    }

                    _state.update {
                        it.copy(progress = it.progress.copy(
                            current = index + 1,
                            currentItem = videoUri.substringAfterLast('/')
                        ))
                    }

                    val frames = repository.extractFrames(videoUri, (frameInterval * 1000).toLong())
                    frames.forEachIndexed { frameIndex, frame ->
                        if (_state.value.isPaused) {
                            return@forEachIndexed
                        }
                        val extractedText = repository.processFrame(frame)
                        repository.insertFrame(frame.copy(extractedText = extractedText))
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            } finally {
                _state.update { 
                    it.copy(
                        isProcessing = false,
                        progress = ProcessingProgress()
                    )
                }
            }
        }
    }

    private fun scanImages() {
        viewModelScope.launch {
            try {
                _state.update { 
                    it.copy(
                        isScanning = true,
                        progress = ProcessingProgress(type = ProcessingType.IMAGES)
                    )
                }
                repository.scanDeviceImages()
                
                val images = repository.getAllImages().first()
                processedImages = images.filter { it.extractedText == null }.toMutableList()
                totalImages = processedImages.size
                currentImageIndex = 0

                if (processedImages.isNotEmpty()) {
                    startProcessing()
                } else {
                    resetProcessingState()
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
                resetProcessingState()
            } finally {
                _state.update { it.copy(isScanning = false) }
            }
        }
    }

    private fun processSelectedImages(uris: List<String>) {
        viewModelScope.launch {
            try {
                _state.update { 
                    it.copy(
                        isProcessing = true,
                        isPaused = false,
                        progress = ProcessingProgress(
                            total = uris.size,
                            type = ProcessingType.IMAGES
                        )
                    )
                }

                val selectedModel = settingsDataStore.selectedModel.first() // Get model name

                uris.forEachIndexed { index, imageUri ->
                    if (_state.value.isPaused) {
                        return@forEachIndexed
                    }

                    _state.update {
                        it.copy(progress = it.progress.copy(
                            current = index + 1,
                            currentItem = imageUri.substringAfterLast('/')
                        ))
                    }

                    try {
                        val imageId = System.currentTimeMillis() + index
                        val image = ImageInfo(
                            id = imageId,
                            uri = imageUri,
                            displayName = imageUri.substringAfterLast('/'),
                            dateAdded = System.currentTimeMillis(),
                            extractedText = null,
                            label = null,
                            confidence = null,
                            modelName = null
                        )

                        repository.insertImage(image)

                        // process it to extract text and generate tags
                        val tags = repository.processImage(imageId, imageUri, selectedModel)
                        if (tags.isNotBlank()) {
                            println("DEBUG: Processing selected image ${image.displayName}, got tags: $tags")
                            repository.updateImageText(imageId, tags)
                        } else {
                            println("DEBUG: No tags found for selected image ${image.displayName}")
                        }
                    } catch (e: Exception) {
                        println("DEBUG: Error processing selected image $imageUri: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                println("DEBUG: Error in processing selected images: ${e.message}")
                _state.update { it.copy(error = e.message) }
            } finally {
                _state.update { 
                    it.copy(
                        isProcessing = false,
                        progress = ProcessingProgress()
                    )
                }
            }
        }
    }

    private fun startProcessing() {
        if (processingJob?.isActive == true) return

        processingJob = viewModelScope.launch {
            try {
                _state.update { 
                    it.copy(
                        isProcessing = true,
                        isPaused = false,
                        progress = ProcessingProgress(
                            total = totalImages,
                            type = ProcessingType.IMAGES
                        )
                    )
                }
                val scanMode = settingsDataStore.scanMode.first()

                while (currentImageIndex < processedImages.size && !_state.value.isPaused) {
                    val image = processedImages[currentImageIndex]
                    
                    _state.update {
                        it.copy(progress = it.progress.copy(
                            current = currentImageIndex + 1,
                            currentItem = image.displayName
                        ))
                    }

                    try {
                        // Process image to extract text and generate tags
                        val tags = repository.processImage(image.id, image.uri, scanMode.name)
                        if (tags.isNotBlank()) {
                            println("DEBUG: Processing image ${image.displayName}, got tags: $tags")
                            repository.updateImageText(image.id, tags)
                        } else {
                            println("DEBUG: No tags found for image ${image.displayName}")
                        }
                    } catch (e: Exception) {
                        println("DEBUG: Error processing image ${image.displayName}: ${e.message}")
                    }

                    currentImageIndex++
                    updateProgress()
                }

                if (currentImageIndex >= processedImages.size) {
                    resetProcessingState()
                }
            } catch (e: Exception) {
                println("DEBUG: Error in processing job: ${e.message}")
                _state.update { it.copy(error = e.message) }
                resetProcessingState()
            }
        }
    }

    private fun pauseProcessing() {
        _state.update { it.copy(isPaused = true) }
    }

    private fun resumeProcessing() {
        _state.update { it.copy(isPaused = false) }
        startProcessing()
    }

    private fun stopProcessing() {
        processingJob?.cancel()
        resetProcessingState()
    }

    private fun resetProcessingState() {
        currentImageIndex = 0
        totalImages = 0
        processedImages.clear()
        _state.update {
            it.copy(
                isProcessing = false,
                isPaused = false,
                progress = ProcessingProgress(),
                error = null
            )
        }
    }

    private fun updateProgress() {
        _state.update {
            it.copy(progress = it.progress.copy(
                current = currentImageIndex
            ))
        }
    }

    private fun navigateToFilteredImages(tag: Tag) {}
}