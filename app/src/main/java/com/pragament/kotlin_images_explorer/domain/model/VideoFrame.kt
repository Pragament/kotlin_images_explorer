package com.pragament.kotlin_images_explorer.domain.model



data class VideoFrame(
    val id: Long,
    val videoUri: String,
    val frameUri: String,
    val timestamp: Long,
    val extractedText: String? = null,
    val label: String?,
    val confidence: Float?,
    val modelName: String?
)