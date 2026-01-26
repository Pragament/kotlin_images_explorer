package com.pragament.kotlin_images_explorer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.DataType

data class BoundingBox(
    val x1: Float, val y1: Float, val x2: Float, val y2: Float,
    val cx: Float, val cy: Float, val w: Float, val h: Float,
    val cnf: Float, val cls: Int, val clsName: String
)

class YoloDetector(
    private val context: Context,
    private val modelPath: String = "yolov8n.tflite", // MUST match your file name
    private val labelPath: String = "labels.txt"      // MUST match your label file
) {
    private var interpreter: Interpreter? = null
    private var labels = mutableListOf<String>()

    init {
        val model = FileUtil.loadMappedFile(context, modelPath)
        val options = Interpreter.Options()
        interpreter = Interpreter(model, options)
        labels = FileUtil.loadLabels(context, labelPath).toMutableList()
    }

    fun detect(bitmap: Bitmap): List<BoundingBox> {
        if (interpreter == null) return emptyList()

        // 1. Resize image to 640x640 (What YOLO expects)
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(640, 640, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        var tensorImage = TensorImage(DataType.FLOAT32) // Use FLOAT32
        tensorImage.load(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        // 2. Prepare output: YOLO output is [1, 84, 8400]
        val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 84, 8400), DataType.FLOAT32)

        // 3. Run the model
        interpreter?.run(tensorImage.buffer, outputBuffer.buffer)

        // 4. Decode the numbers
        return bestBox(outputBuffer.floatArray)
    }

    private fun bestBox(array: FloatArray): List<BoundingBox> {
        val boxes = mutableListOf<BoundingBox>()
        val numElements = 8400
        val numClasses = labels.size
        // Note: Check your labels.txt size. If less than 80, this might crash if model is standard 80.

        for (c in 0 until numElements) {
            var maxConf = 0f
            var maxClass = -1

            // Find class with highest confidence
            for (j in 0 until numClasses) {
                // YOLOv8 format: [x, y, w, h, class0, class1, ...]
                // So class probabilities start at index 4
                val conf = array[(4 + j) * numElements + c]
                if (conf > maxConf) {
                    maxConf = conf
                    maxClass = j
                }
            }

            if (maxConf > 0.5f) { // Confidence Threshold
                val cx = array[0 * numElements + c]
                val cy = array[1 * numElements + c]
                val w = array[2 * numElements + c]
                val h = array[3 * numElements + c]

                boxes.add(BoundingBox(
                    cx - w/2, cy - h/2, cx + w/2, cy + h/2,
                    cx, cy, w, h, maxConf, maxClass, labels[maxClass]
                ))
            }
        }
        return boxes
    }
}