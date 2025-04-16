package com.pragament.kotlin_images_explorer.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.pragament.kotlin_images_explorer.ImageClassifier
import com.pragament.kotlin_images_explorer.data.local.SettingsDataStore
import com.pragament.kotlin_images_explorer.data.local.dao.ImageInfoDao
import com.pragament.kotlin_images_explorer.data.local.dao.VideoFrameDao
import com.pragament.kotlin_images_explorer.data.local.entity.FrameProcessingResult
import com.pragament.kotlin_images_explorer.data.local.entity.ImageInfoEntity
import com.pragament.kotlin_images_explorer.data.local.entity.VideoFrameEntity
import com.pragament.kotlin_images_explorer.domain.model.ImageInfo
import com.pragament.kotlin_images_explorer.domain.model.Tag
import com.pragament.kotlin_images_explorer.domain.model.VideoFrame
import com.pragament.kotlin_images_explorer.domain.repository.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URI

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
                .map { 
                    val cleanWord = it.word.trim()
                        .replace(Regex("[^a-zA-Z0-9]"), "")
                        .replace(Regex("\\s+"), "")
                    Tag(cleanWord, it.frequency) 
                }
                .filter { it.word.isNotBlank() }
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

                            try {
                                context.contentResolver.takePersistableUriPermission(
                                    contentUri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            } catch (e: SecurityException) {
                                println("DEBUG: Could not take persistable permission for $contentUri")
                            }

                            println("DEBUG: Processing image: $name")

                            // Create and insert image entity with persistable URI
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
                    "Classification: $label\nConfidence: ${"%.2f".format(confidence)}"
                } ?: "No classification"

                val cleanedText = textResult.split(Regex("[\\s,.;:!?]+"))
                    .asSequence()
                    .map { it.trim() }
                    .filter { it.length > 2 }
                    .map { it.replace(Regex("[^a-zA-Z0-9]"), "") }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .joinToString(" ")

                imageDao.getImageById(imageId)?.let { image ->
                    val updatedImage = image.copy(
                        extractedText = cleanedText,
                        label = classificationResult?.first,
                        confidence = classificationResult?.second,
                        modelName = modelName
                    )
                    imageDao.updateImage(updatedImage)
                }

                "$classificationText\nModelName: $modelName\nText: $cleanedText"
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
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATE_ADDED
            )

            val selection = "${MediaStore.Video.Media.MIME_TYPE} LIKE ?"
            val selectionArgs = arrayOf("video/%")
            val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

            contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val dateAdded = cursor.getLong(dateColumn)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val frames = extractFrames(contentUri.toString(),
                        1000) // Extract frames every 1 seconds
                    frames.forEach { frame ->
                        val extractedText = processFrame(frame, "mobilenet_v1")
                        insertFrame(frame.copy(extractedText = extractedText))
                    }
                }
            }
        }
    }

    override suspend fun getAllVideoFrames(): Flow<List<VideoFrame>> {
        return videoFrameDao.getAllFrames().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    // New methods for videos
    override suspend fun extractFrames(videoUri: String, intervalMs: Long): List<VideoFrame> {
        return withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, Uri.parse(videoUri))

            val duration = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLong() ?: 0

            println("Video duration: $duration ms") // Log the video duration

            val frames = mutableListOf<VideoFrame>()
            var timestamp = 0L

            while (timestamp < duration) {
                val frame = retriever.getFrameAtTime(timestamp * 1000) // Convert to microseconds
                if (frame != null) {
                    frames.add(VideoFrame(
                        id = System.currentTimeMillis() + timestamp,
                        videoUri = videoUri,
                        frameUri = saveFrameToStorage(frame),
                        timestamp = timestamp,
                        extractedText = null,
                        label = null,
                        confidence = null,
                        modelName = null

                    ))
                }
                timestamp += intervalMs
            }

            retriever.release()
            println("Extracted ${frames.size} " +
                    "frames from video: $videoUri") // Log the number of frames extracted
            frames
        }
    }

    override suspend fun processFrame(frame: VideoFrame, modelName: String): String = withContext(Dispatchers.IO) {
        try {
            val bitmap = loadFrameFromStorage(frame.frameUri)
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            val textResult = textRecognizer.process(inputImage).await().text
            val modelPath = getModelPath(modelName)
            val classifier = ImageClassifier(context, modelPath)

            val classificationResult = if (modelName == "mobilenet_v1") {
                classifier.classify(bitmap) ?: classifier.classifyModel2(bitmap)
            } else {
                classifier.classifyModel2(bitmap)
            }

            val (label, confidence) = classificationResult ?: (null to null)

            val confidenceFormatted = confidence?.let { "%.2f".format(it) } ?: "N/A"
            return@withContext "$label|$confidenceFormatted|$modelName|$textResult"
        } catch (e: Exception) {
            e.printStackTrace()
            "ERROR|0.0|$modelName|${e.message ?: "Unknown error"}"
        }
    }

    override suspend fun insertFrame(frame: VideoFrame) {
        videoFrameDao.insertFrame(
            VideoFrameEntity(
                id = frame.id,
                videoUri = frame.videoUri,
                frameUri = frame.frameUri,
                timestamp = frame.timestamp,
                extractedText = frame.extractedText,
                label = frame.label,
                confidence = frame.confidence,
                modelName = frame.modelName

            )
        )
    }

    private suspend fun saveFrameToStorage(frame: Bitmap): String {
        // Save the frame to internal storage and return its URI
        // Implementation depends on your storage strategy
        return withContext(Dispatchers.IO) {
            val fileName = "frame_${System.currentTimeMillis()}.jpg"
            val file = File(context.cacheDir, fileName) // Save to app's cache directory
            val outputStream = FileOutputStream(file)
            frame.compress(Bitmap.CompressFormat.JPEG, 90, outputStream) // Compress and save the bitmap
            outputStream.flush()
            outputStream.close()
            file.toURI().toString() // Return the URI as a string
        }
    }

    private suspend fun loadFrameFromStorage(frameUri: String): Bitmap {
        // Load the frame from internal storage
        // Implementation depends on your storage strategy
        return withContext(Dispatchers.IO) {
            val file = File(URI(frameUri)) // Convert URI string to File
            val inputStream = FileInputStream(file)
            BitmapFactory.decodeStream(inputStream) // Decode the file into a Bitmap
        }
    }

    private fun VideoFrameEntity.toDomainModel() = VideoFrame(
        id = id,
        videoUri = videoUri,
        frameUri = frameUri,
        timestamp = timestamp,
        extractedText = extractedText,
        label = label,
        confidence = confidence,
        modelName = modelName
    )

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

}

private fun getModelPath(modelName: String): String {
    return when (modelName) {
        "mobilenet_v1" -> "model1.tflite"
        "mobilenet_v2" -> "model2.tflite"
        else -> throw IllegalArgumentException("Unsupported model: $modelName")
    }
}
