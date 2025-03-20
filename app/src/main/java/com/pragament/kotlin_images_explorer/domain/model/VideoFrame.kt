package com.pragament.kotlin_images_explorer.domain.model

import android.graphics.Bitmap

data class VideoFrame(
    val id: Long,
    val videoUri: String,
    val frameTimestamp: Long,
    val bitmap: Bitmap?, // Nullable since we don't store bitmaps in database
    val extractedText: String?
)