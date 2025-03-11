package com.pragament.kotlin_images_explorer.domain.model

data class ImageInfo(
    val id: Long,
    val uri: String,
    val displayName: String,
    val dateAdded: Long,
    val extractedText: String? = null
) 