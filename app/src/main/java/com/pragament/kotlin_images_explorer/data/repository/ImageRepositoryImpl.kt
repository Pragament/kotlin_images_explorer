package com.pragament.kotlin_images_explorer.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.pragament.kotlin_images_explorer.ImageClassifier
import com.pragament.kotlin_images_explorer.data.local.dao.ImageInfoDao
import com.pragament.kotlin_images_explorer.data.local.entity.ImageInfoEntity
import com.pragament.kotlin_images_explorer.domain.model.ImageInfo
import com.pragament.kotlin_images_explorer.domain.model.Tag
import com.pragament.kotlin_images_explorer.domain.repository.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ImageRepositoryImpl(
    private val context: Context,
    private val contentResolver: ContentResolver,
    private val imageDao: ImageInfoDao
) : ImageRepository {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun getAllImages(): Flow<List<ImageInfo>> {
        return imageDao.getAllImages().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getImagesByTag(tag: String): Flow<List<ImageInfo>> {
        return imageDao.getImagesByTag(tag).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getAllTags(): Flow<List<Tag>> {
        return imageDao.getAllTags().map { tagCounts ->
            tagCounts.map { Tag(it.word, it.frequency) }
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

            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val dateAdded = cursor.getLong(dateColumn)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

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
                }
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
                    "$label $confidence"
                } ?: "No classification result"

                // Update the image entity with the new details
                val image = imageDao.getImageById(imageId) // Assuming you have a function to fetch the image by id
                image?.let {
                    val updatedImage = it.copy(
                        extractedText = textResult,
                        label = classificationResult?.first,
                        confidence = classificationResult?.second,
                        modelName = modelName
                    )
                    imageDao.updateImage(updatedImage) // Assuming you have an update function
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
                label = image.label ,
                confidence = image.confidence ,
                modelName = image.modelName
            )
        )
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
}

private fun getModelPath(modelName: String): String {
    return when (modelName) {
        "mobilenet_v1" -> "model1.tflite"
        "mobilenet_v2" -> "model2.tflite"
        else -> throw IllegalArgumentException("Unsupported model: $modelName")
    }
}
