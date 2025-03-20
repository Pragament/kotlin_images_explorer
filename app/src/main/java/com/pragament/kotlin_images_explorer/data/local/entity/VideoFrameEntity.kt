package com.pragament.kotlin_images_explorer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_frames")
data class VideoFrameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val videoId: Long,
    val videoUri: String,
    val frameTimestamp: Long,
    val extractedText: String,
    val dateAdded: Long
)