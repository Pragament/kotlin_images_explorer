package com.pragament.kotlin_images_explorer.domain.repository

import com.pragament.kotlin_images_explorer.domain.model.ImageInfo
import com.pragament.kotlin_images_explorer.domain.model.Tag
import com.pragament.kotlin_images_explorer.domain.model.VideoFrame
import kotlinx.coroutines.flow.Flow

interface ImageRepository {
    suspend fun getAllImages(): Flow<List<ImageInfo>>
    suspend fun getImagesByTag(tag: String): Flow<List<ImageInfo>>
    suspend fun getAllTags(): Flow<List<Tag>>
    suspend fun updateImageText(imageId: Long, extractedText: String)
    suspend fun scanDeviceImages()
    suspend fun processImage(imageId: Long, uri: String,modelName: String): String
    suspend fun insertImage(image: ImageInfo)

    // New methods for videos
    suspend fun scanDeviceVideos()
    suspend fun extractFrames(videoUri: String, intervalMs: Long): List<VideoFrame>
    suspend fun processFrame(frame: VideoFrame): String
    suspend fun insertFrame(frame: VideoFrame)
    suspend fun getAllVideoFrames(): Flow<List<VideoFrame>>
} 