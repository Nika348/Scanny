package com.example.scanny.models

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.task.gms.vision.detector.Detection
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

data class ModelConfiguration(
    val imgHeight: Int,
    val imgWight: Int
)

class TFDetector(
    assets: AssetManager,
    private val configuration: ModelConfiguration,
) : Detector {
    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(0f, 255f))
        .build()

    private val tflite : Interpreter
    private val tensorImage = TensorImage(DataType.FLOAT32)
    private val boxesTensor = TensorBuffer.createFixedSize(intArrayOf(1, 1000, 4), DataType.FLOAT32)
    private val detectionsCountTensor = TensorBuffer.createFixedSize(intArrayOf(4), DataType.UINT8)
    private val labelsTensor = TensorBuffer.createFixedSize(intArrayOf(1, 1000), DataType.FLOAT32)
    private val scoresTensor = TensorBuffer.createFixedSize(intArrayOf(1, 1000), DataType.FLOAT32)
    private val outputs = mutableMapOf<Int, Any>(
        0 to boxesTensor.buffer,
        1 to detectionsCountTensor.buffer,
        2 to labelsTensor.buffer,
        3 to scoresTensor.buffer,
    )

    init {
        val tfliteModel = loadTFLiteModelFromAsset(assets, "sku-base-640-480-fp16.tflite")
        val tfliteOptions = Interpreter.Options()
        tflite = Interpreter(tfliteModel, tfliteOptions)
        tflite.allocateTensors()
    }

    private fun loadTFLiteModelFromAsset(
        assetManager: AssetManager,
        modelPath: String
    ): ByteBuffer {
        val assetFileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength).apply {
            inputStream.close()
            fileChannel.close()
        }
    }

    private fun resizeWithPadding(image: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val width = image.width
        val height = image.height

        val scale = Math.min(targetWidth.toFloat() / width, targetHeight.toFloat() / height)

        val matrix = Matrix()
        matrix.postScale(scale, scale)

        return Bitmap.createBitmap(
            targetWidth,
            targetHeight,
            Bitmap.Config.ARGB_8888
        ).apply {
            val canvas = Canvas(this)
            canvas.drawColor(Color.BLACK)
            canvas.drawBitmap(image, matrix, null)
        }
    }

    override fun detect(image: Bitmap): List<Detection> {
        for (buffer in outputs.values) {
            (buffer as ByteBuffer).rewind()
        }

        val paddedImage = resizeWithPadding(image, configuration.imgWight, configuration.imgHeight)
        tensorImage.load(paddedImage)
        val tensorInput = imageProcessor.process(tensorImage)
        tflite.runForMultipleInputsOutputs(arrayOf(tensorInput.buffer), outputs)
        val detections = convert(image.width, image.height, configuration.imgWight, configuration.imgHeight)
        for (detection in detections) {
            val boundingBox = detection.getBoundingBox()
            //Log.d("TFDetector", "Detected bounding box: Left=${boundingBox.left}, Top=${boundingBox.top}, Right=${boundingBox.right}, Bottom=${boundingBox.bottom}")
        }

        return detections
    }


    private fun convert(
        srcWidth: Int,
        srcHeight: Int,
        dstWidth: Int,
        dstHeight: Int
    ): List<Detection> {
        var detectionsCount = 0
        detectionsCountTensor.intArray.forEach { count ->
            detectionsCount += count
            if (count < 255)
                return@forEach
        }
        val boxesTensor = boxesTensor.floatArray
        val scoresTensor = scoresTensor.floatArray
        val detections = ArrayList<Detection>(detectionsCount)

        val srcRatio = 1f * srcWidth / srcHeight
        val dstRatio = 1f * dstWidth / dstHeight
        var ax = 1f
        var bx = 0f
        var ay = 1f
        var by = 0f
        if (dstRatio >= srcRatio) {
            val notScaledDstWith = (srcHeight * dstRatio / dstRatio).toInt()
            ax = 1f * notScaledDstWith / srcWidth
            bx = -ax * ((notScaledDstWith - srcWidth) / 2) / notScaledDstWith
        } else {
            val notScaledDstHeight = (srcHeight * srcRatio / dstRatio).toInt()
            ay = 1f * notScaledDstHeight / srcHeight
            by = -ay * ((notScaledDstHeight - srcHeight) / 2) / notScaledDstHeight
        }

        for (k in 0 until detectionsCount) {
            val det = object : Detection() {
                override fun getBoundingBox(): RectF {
                    val left = ax * boxesTensor[k * 4 + 0] + bx
                    val top = ay * boxesTensor[k * 4 + 1] + by
                    val right = ax * boxesTensor[k * 4 + 2] + bx
                    val bottom = ay * boxesTensor[k * 4 + 3] + by

                    val scaledLeft = left * srcWidth
                    val scaledTop = top * srcHeight
                    val scaledRight = right * srcWidth
                    val scaledBottom = bottom * srcHeight
                    //Log.d("TFDetector", "Object $k - Left: $left, Top: $top, Right: $right, Bottom: $bottom")
                    Log.d("TFDetector", "Object $k - scLeft: $scaledLeft, scTop: $scaledTop, scRight: $scaledRight, scBottom: $scaledBottom")

                    return RectF(scaledLeft, scaledTop, scaledRight, scaledBottom)
                }

                override fun getCategories(): MutableList<Category> {
                    return mutableListOf()
                }

            }
            detections.add(det)
        }
        return detections
    }
}

interface Detector {
    fun detect(image: Bitmap): List<Detection>
}

