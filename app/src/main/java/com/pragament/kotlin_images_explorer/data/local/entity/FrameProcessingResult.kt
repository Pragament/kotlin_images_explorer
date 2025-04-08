package com.pragament.kotlin_images_explorer.data.local.entity

data class FrameProcessingResult(
    val extractedText: String,
    val label: String?,
    val confidence: Float?,
    val modelName: String
)
