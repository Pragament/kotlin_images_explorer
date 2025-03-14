package com.pragament.kotlin_images_explorer


import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import org.json.JSONObject
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ImageClassifier(private val context: Context, private var modelPath: String) {
    private var interpreter: Interpreter? = null

    init {
        loadModel()
    }

    // Load the model
    private fun loadModel() {
        val model = FileUtil.loadMappedFile(context, modelPath)
        interpreter = Interpreter(model)
    }

    // Classify an image and return the label and confidence
    fun classify(bitmap: Bitmap): Pair<String, Float>? {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true) // Resize to model input size
        val inputImage = TensorImage(DataType.UINT8)
        inputImage.load(resizedBitmap)

        val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 1001), DataType.UINT8)

        interpreter?.run(inputImage.buffer, outputBuffer.buffer)

        val rawOutput = outputBuffer.intArray // Get uint8 output

        // Apply dequantization: confidence = 0.00390625 * q
        val confidenceArray = rawOutput.map { it * 0.00390625f }

        val maxIdx = confidenceArray.indices.maxByOrNull { confidenceArray[it] } ?: return null
        val labels = loadLabels()

        if (maxIdx >= labels.size) return null

        return Pair(labels[maxIdx], confidenceArray[maxIdx])
    }



    fun classifyModel2(bitmap: Bitmap): Pair<String, Float>? {
        // Create an input tensor buffer with correct shape
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val inputTensorBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)

        // Allocate a ByteBuffer for input
        val inputByteBuffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3).order(ByteOrder.nativeOrder())

        // Extract pixels and normalize (0-255 → 0-1)
        val intValues = IntArray(224 * 224)
        resizedBitmap.getPixels(intValues, 0, 224, 0, 0, 224, 224)

        for (pixel in intValues) {
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f
            inputByteBuffer.putFloat(r)
            inputByteBuffer.putFloat(g)
            inputByteBuffer.putFloat(b)
        }

        inputTensorBuffer.loadBuffer(inputByteBuffer)

        val outputShape = interpreter?.getOutputTensor(0)?.shape() ?: return null
        val outputBuffer = TensorBuffer.createFixedSize(outputShape, DataType.FLOAT32)

        interpreter?.run(inputTensorBuffer.buffer, outputBuffer.buffer)

        val confidenceArray = outputBuffer.floatArray

        // Get the highest confidence index
        val maxIdx = confidenceArray.indices.maxByOrNull { confidenceArray[it] } ?: return null
        val confidence = confidenceArray[maxIdx]

        val labels = loadLabelsFromJson(context)
        val label = labels[maxIdx] ?: "Unknown"

        return Pair(label, confidence)


        //   return Pair("label_$maxIdx", confidence)
    }


    fun loadLabelsFromJson(context: Context): Map<Int, String> {
        val jsonString = context.assets.open("config.json").bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(jsonString)
        val id2label = jsonObject.getJSONObject("id2label")

        val labels = mutableMapOf<Int, String>()
        id2label.keys().forEach { key ->
            labels[key.toInt()] = id2label.getString(key)
        }
        return labels
    }

    fun loadLabels(): List<String> {
        return try {
            context.assets.open("labels_mobilenet_quant_v1_224.txt").bufferedReader().use { it.readLines() }
        } catch (e: Exception) {
            Log.e("Model", "Error loading labels: ${e.message}")
            emptyList()
        }
    }




    // Switch to a different model
    fun switchModel(newModelPath: String) {
        modelPath = newModelPath
        loadModel()
    }
}
