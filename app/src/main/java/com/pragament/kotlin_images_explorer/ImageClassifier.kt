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
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ImageClassifier(private val context: Context, private var modelPath: String) {
    private var interpreter: Interpreter? = null
    private var labels = mutableListOf<String>()

    init {
        loadModel()
        labels.addAll(loadLabels())
    }

    fun isModelLoaded(): Boolean {
        return interpreter != null
    }

    // Load the model
    private fun loadModel() {
        try {
            val model = FileUtil.loadMappedFile(context, modelPath)
            interpreter = Interpreter(model)
        } catch (e: Exception) {
            Log.e("ImageClassifier", "Error loading model ($modelPath): ${e.message}")
        }
    }

    // Classify an image and return the label and confidence
    fun classify(bitmap: Bitmap): Pair<String, Float>? {
        if (interpreter == null) return null

        val inputTensor = interpreter?.getInputTensor(0) ?: return null
        val inputShape = inputTensor.shape() // [1, height, width, channels]
        val inputType = inputTensor.dataType()

        val height = inputShape.getOrElse(1) { 224 }
        val width = inputShape.getOrElse(2) { 224 }

        // Resize bitmap to match model input
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)

        // Prepare TensorImage based on input type
        val tensorImage = TensorImage(inputType)
        tensorImage.load(resizedBitmap)
        
        // If Model expects Float32 but we loaded a Bitmap (which is effectively Uint8 data), 
        // we might need an ImageProcessor to convert/normalize.
        // MobileNetV2 (Float) typically expects inputs in range [-1, 1].
        val processedImage = if (inputType == DataType.FLOAT32) {
             val imageProcessor = ImageProcessor.Builder()
                .add(NormalizeOp(127.5f, 127.5f)) // Normalize [0, 255] -> [-1, 1]
                .build()
             imageProcessor.process(tensorImage)
        } else {
             // For UINT8, usually no normalization needed (values 0-255)
             tensorImage
        }

        // Prepare Output Buffer
        val outputTensor = interpreter?.getOutputTensor(0) ?: return null
        val outputShape = outputTensor.shape()
        val outputType = outputTensor.dataType()
        
        // Handle varying output shapes (e.g. [1, 1001] vs [1, 1000])
        val outputSize = outputShape.last() 
        val outputBuffer = TensorBuffer.createFixedSize(outputShape, outputType)

        interpreter?.run(processedImage.buffer, outputBuffer.buffer)

        // Parse Output
        // Parse Output
        val rawConfidenceArray = outputBuffer.floatArray
        
        // Fix for Quantized models (UINT8) that don't auto-dequantize
        // If the scores are big (e.g., 0-255), we normalize them to 0-1
        val maxScore = rawConfidenceArray.maxOrNull() ?: 0f
        val confidenceArray = if (maxScore > 1.0f) {
             rawConfidenceArray.map { it / 255.0f }.toFloatArray()
        } else {
             rawConfidenceArray
        }

        val maxIdx = confidenceArray.indices.maxByOrNull { confidenceArray[it] } ?: return null
        
        // Safety check for label index
        val label = if (labels.isNotEmpty()) {
            if (maxIdx < labels.size) labels[maxIdx] else "Class $maxIdx"
        } else {
            "Class $maxIdx"
        }

        return Pair(label, confidenceArray[maxIdx])
    }
    
    // Deprecated methods redirected to main classify
    fun classifyModel2(bitmap: Bitmap): Pair<String, Float>? {
        return classify(bitmap)
    }



    fun loadLabelsFromJson(context: Context): Map<Int, String> {
        return try {
            val jsonString = context.assets.open("config.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val id2label = jsonObject.getJSONObject("id2label")

            val labelsMap = mutableMapOf<Int, String>()
            id2label.keys().forEach { key ->
                labelsMap[key.toInt()] = id2label.getString(key)
            }
            labelsMap
        } catch (e: Exception) {
             emptyMap()
        }
    }

    fun loadLabels(): List<String> {
        val list = mutableListOf<String>()
        try {
            // Priority 1: standard labels file
            list.addAll(context.assets.open("labels_mobilenet_quant_v1_224.txt").bufferedReader().use { it.readLines() })
        } catch (e: Exception) {
           // Priority 2: config.json (for some models)
           val map = loadLabelsFromJson(context)
           if (map.isNotEmpty()) {
               val maxKey = map.keys.maxOrNull() ?: 0
               for (i in 0..maxKey) {
                   list.add(map[i] ?: "Unknown")
               }
           }
        }
        return list
    }

    // Switch to a different model
    fun switchModel(newModelPath: String) {
        if (modelPath != newModelPath) {
            modelPath = newModelPath
            loadModel()
        }
    }
}
