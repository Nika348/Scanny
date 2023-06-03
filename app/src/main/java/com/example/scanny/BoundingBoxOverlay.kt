package com.example.scanny

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.widget.ImageView
import org.tensorflow.lite.task.gms.vision.detector.Detection

class BoundingBoxOverlay(
    private val imageView: ImageView
) {
    private val paint = Paint().apply {
        color = Color.GRAY
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val transparentPaint = Paint().apply {
        color = Color.parseColor("#80FF0000")
        style = Paint.Style.FILL
    }
    private var lastBitmap: Bitmap? = null

    fun drawBoundingBoxes(image: Bitmap, detections: List<Detection>) {
        val mutableImage = lastBitmap?.let {
            Bitmap.createBitmap(it)
        } ?: image.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableImage)


        for (detection in detections) {
            val boundingBox = detection.getBoundingBox()

            canvas.drawRect(boundingBox, paint)
            canvas.drawRect(boundingBox, transparentPaint)
        }
        lastBitmap = mutableImage

        imageView.postDelayed({
            imageView.setImageBitmap(mutableImage)
        }, 1000)
    }
}