package com.pragament.kotlin_images_explorer.domain.model

data class Tag(
    val word: String,
    val frequency: Int,
    val weight: Int = 1,  // For tag cloud sizing, calculated based on frequency
    val sourceImages: List<Long> = emptyList() // Image IDs containing this tag
)