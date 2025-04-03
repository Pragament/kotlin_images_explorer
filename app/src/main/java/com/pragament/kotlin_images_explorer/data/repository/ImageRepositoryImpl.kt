package com.pragament.kotlin_images_explorer.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.pragament.kotlin_images_explorer.ImageClassifier
import com.pragament.kotlin_images_explorer.data.local.dao.ImageInfoDao
import com.pragament.kotlin_images_explorer.data.local.dao.VideoFrameDao
import com.pragament.kotlin_images_explorer.data.local.entity.ImageInfoEntity
import com.pragament.kotlin_images_explorer.data.local.entity.VideoFrameEntity
import com.pragament.kotlin_images_explorer.domain.model.ImageInfo
import com.pragament.kotlin_images_explorer.domain.model.Tag
import com.pragament.kotlin_images_explorer.domain.model.VideoFrame
import com.pragament.kotlin_images_explorer.domain.repository.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ImageRepositoryImpl(
    private val context: Context,
    private val contentResolver: ContentResolver,
    private val imageDao: ImageInfoDao,
    private val videoFrameDao: VideoFrameDao
) : ImageRepository {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun getAllImages(): Flow<List<ImageInfo>> {
        return imageDao.getAllImages().map { entities ->
            entities.map {
                it.toDomainModel()
            }
        }
    }

    override suspend fun getImagesByTag(tag: String): Flow<List<ImageInfo>> {
        return imageDao.getImagesByTag(tag).map { entities ->
            entities.map {
                it.toDomainModel()
            }
        }
    }

    override suspend fun getAllTags(): Flow<List<Tag>> {
        return imageDao.getAllTags().map { tagCounts ->
            tagCounts
                .filter { it.word.isNotBlank() }
                .map { Tag(it.word, it.frequency) }
                .sortedByDescending { it.frequency }
        }
    }

    override suspend fun updateImageText(imageId: Long, extractedText: String) {
        imageDao.updateImageText(imageId, extractedText)
    }

    override suspend fun scanDeviceImages() {
        withContext(Dispatchers.IO) {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED
            )

            val selection = "${MediaStore.Images.Media.MIME_TYPE} LIKE ?"
            val selectionArgs = arrayOf("image/%")
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            try {
                context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                    println("DEBUG: Found ${cursor.count} images")

                    while (cursor.moveToNext()) {
                        try {
                            val id = cursor.getLong(idColumn)
                            val name = cursor.getString(nameColumn)
                            val dateAdded = cursor.getLong(dateColumn)

                            val contentUri = ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id
                            )

                            println("DEBUG: Processing image: $name")

                            // Create and insert image entity
                            val image = ImageInfoEntity(
                                id = id,
                                uri = contentUri.toString(),
                                displayName = name,
                                dateAdded = dateAdded,
                                extractedText = null,
                                label = null,
                                confidence = null,
                                modelName = null
                            )
                            imageDao.insertImage(image)

                            // Process image immediately
                            val extractedText = processImage(id, contentUri.toString(), "mobilenet_v1")
                            if (extractedText.isNotBlank()) {
                                println("DEBUG: Extracted text from image $name: $extractedText")
                                imageDao.updateImageText(id, extractedText)
                            } else {
                                println("DEBUG: No text extracted from image $name")
                            }
                        } catch (e: Exception) {
                            println("DEBUG: Error processing image: ${e.message}")
                            e.printStackTrace()
                            // Continue with next image
                        }
                    }
                }
            } catch (e: Exception) {
                println("DEBUG: Error scanning images: ${e.message}")
                e.printStackTrace()
                throw e
            }
        }
    }



    override suspend fun processImage(imageId: Long, uri: String, modelName: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val parsedUri = Uri.parse(uri)
                val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(parsedUri))
                    ?: return@withContext "Error: Could not decode image."

                val inputImage = InputImage.fromFilePath(context, parsedUri)
                val textResult = textRecognizer.process(inputImage).await().text

                val modelPath = getModelPath(modelName)
                val classifier = ImageClassifier(context, modelPath)

                val classificationResult = if (modelName == "mobilenet_v1") {
                    classifier.classify(bitmap) ?: classifier.classifyModel2(bitmap)
                } else {
                    classifier.classifyModel2(bitmap)
                }

                val classificationText = classificationResult?.let { (label, confidence) ->
                    "$label ${"%.2f".format(confidence)}"
                } ?: "No classification result"

                imageDao.getImageById(imageId)?.let { image ->
                    val updatedImage = image.copy(
                        extractedText = textResult,
                        label = classificationResult?.first,
                        confidence = classificationResult?.second,
                        modelName = modelName
                    )
                    imageDao.updateImage(updatedImage)
                }

                "Classification: $classificationText\nModelName: $modelName\nText: $textResult\n"
            } catch (e: Exception) {
                e.printStackTrace()
                "Error: ${e.message}"
            }
        }
    }



    override suspend fun insertImage(image: ImageInfo) {
        imageDao.insertImage(
            ImageInfoEntity(
                id = image.id,
                uri = image.uri,
                displayName = image.displayName,
                dateAdded = image.dateAdded,
                extractedText = image.extractedText,
                label = image.label,
                confidence = image.confidence,
                modelName = image.modelName
            )
        )
    }

    override suspend fun scanDeviceVideos() {
        withContext(Dispatchers.IO) {
            try {
                val projection = arrayOf(
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.DATE_ADDED,
                    MediaStore.Video.Media.DURATION
                )

                val selection = "${MediaStore.Video.Media.MIME_TYPE} LIKE ?"
                val selectionArgs = arrayOf("video/%")
                val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

                context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                    val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

                    while (cursor.moveToNext()) {
                        try {
                            val id = cursor.getLong(idColumn)
                            val name = cursor.getString(nameColumn)
                            val dateAdded = cursor.getLong(dateColumn)
                            val duration = cursor.getLong(durationColumn)

                            val contentUri = ContentUris.withAppendedId(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                id
                            )

                            println("DEBUG: Processing video: $name")
                            val retriever = MediaMetadataRetriever()

                            try {
                                retriever.setDataSource(context, contentUri)
                                var currentTime = 0L
                                val frameInterval = 1000L // 1 second interval

                                while (currentTime < duration) {
                                    val frame = retriever.getFrameAtTime(
                                        currentTime * 1000, // Convert to microseconds
                                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                                    )

                                    if (frame != null) {
                                        println("DEBUG: Extracted frame at ${currentTime}ms")
                                        val inputImage = InputImage.fromBitmap(frame, 0)
                                        val textResult = textRecognizer.process(inputImage).await()
                                        val extractedText = textResult.text

                                        if (extractedText.isNotBlank()) {
                                            println("DEBUG: Found text in frame: $extractedText")
                                            val videoFrame = VideoFrameEntity(
                                                id = 0, // Auto-generated
                                                videoId = id,
                                                videoUri = contentUri.toString(),
                                                frameTimestamp = currentTime,
                                                extractedText = extractedText,
                                                dateAdded = dateAdded
                                            )
                                            videoFrameDao.insertFrame(videoFrame)
                                        }
                                        frame.recycle()
                                    }
                                    currentTime += frameInterval
                                }
                            } catch (e: Exception) {
                                println("DEBUG: Error processing video frame: ${e.message}")
                                e.printStackTrace()
                            } finally {
                                try {
                                    retriever.release()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        } catch (e: Exception) {
                            println("DEBUG: Error processing video: ${e.message}")
                            e.printStackTrace()
                            // Continue with next video
                        }
                    }
                }
            } catch (e: Exception) {
                println("DEBUG: Error in video scanning: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    override suspend fun extractFrames(videoUri: String, frameIntervalMs: Long): List<VideoFrame> {
        return withContext(Dispatchers.IO) {
            val frames = mutableListOf<VideoFrame>()
            val retriever = MediaMetadataRetriever()

            try {
                retriever.setDataSource(context, Uri.parse(videoUri))
                var currentTime = 0L
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0

                while (currentTime <= duration) {
                    val frame = retriever.getFrameAtTime(
                        currentTime * 1000, // Convert to microseconds
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )

                    if (frame != null) {
                        frames.add(
                            VideoFrame(
                                id = 0,
                                videoUri = videoUri,
                                frameTimestamp = currentTime,
                                bitmap = frame,
                                extractedText = null
                            )
                        )
                        frame.recycle()
                    }
                    currentTime += frameIntervalMs
                }
            } catch (e: Exception) {
                println("DEBUG: Error extracting frames: ${e.message}")
                e.printStackTrace()
            } finally {
                try {
                    retriever.release()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            frames
        }
    }

    override suspend fun processFrame(frame: VideoFrame): String {
        return withContext(Dispatchers.IO) {
            try {
                if (frame.bitmap != null) {
                    val inputImage = InputImage.fromBitmap(frame.bitmap, 0)
                    val result = textRecognizer.process(inputImage).await()
                    result.text
                } else {
                    ""
                }
            } catch (e: Exception) {
                println("DEBUG: Error processing frame: ${e.message}")
                e.printStackTrace()
                ""
            }
        }
    }

    override suspend fun insertFrame(frame: VideoFrame) {
        videoFrameDao.insertFrame(
            VideoFrameEntity(
                id = frame.id,
                videoId = Uri.parse(frame.videoUri).lastPathSegment?.toLong() ?: 0,
                videoUri = frame.videoUri,
                frameTimestamp = frame.frameTimestamp,
                extractedText = frame.extractedText!!,
                dateAdded = System.currentTimeMillis()
            )
        )
    }

    override suspend fun getAllVideoFrames(): Flow<List<VideoFrame>> {
        return videoFrameDao.getAllFrames().map { entities ->
            entities.map { entity ->
                VideoFrame(
                    id = entity.id,
                    videoUri = entity.videoUri,
                    frameTimestamp = entity.frameTimestamp,
                    bitmap = null,
                    extractedText = entity.extractedText
                )
            }
        }
    }

    private fun ImageInfoEntity.toDomainModel() = ImageInfo(
        id = id,
        uri = uri,
        displayName = displayName,
        dateAdded = dateAdded,
        extractedText = extractedText,
        label = label,
        confidence = confidence,
        modelName = modelName
    )

    private fun VideoFrameEntity.toDomainModel() = VideoFrame(
        id = id,
        videoUri = videoUri,
        frameTimestamp = frameTimestamp,
        bitmap = null,
        extractedText = extractedText
    )
}

private fun getModelPath(modelName: String): String {
    return when (modelName) {
        "mobilenet_v1" -> "model1.tflite"
        "mobilenet_v2" -> "model2.tflite"
        else -> throw IllegalArgumentException("Unsupported model: $modelName")
    }
}
