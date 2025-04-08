package com.pragament.kotlin_images_explorer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_frames")
data class VideoFrameEntity(
    @PrimaryKey val id: Long,
    val videoUri: String, // URI of the original video
    val frameUri: String, // URI of the extracted frame
    val timestamp: Long, // Timestamp of the frame in the video
    val extractedText: String? = null,
    val label: String?,
    val confidence: Float?,
    val modelName: String?
)