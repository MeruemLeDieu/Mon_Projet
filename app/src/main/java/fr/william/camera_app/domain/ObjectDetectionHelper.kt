package fr.william.camera_app.domain

import android.graphics.Bitmap
import org.tensorflow.lite.task.vision.detector.Detection

data class ObjectDetectionResult(
    val results: MutableList<Detection>?,
    val inferenceTime: Long,
    val imageHeight: Int,
    val imageWidth: Int
)
interface ObjectDetectionHelper {
    fun detectObject(image: Bitmap, imageRotation: Int) : ObjectDetectionResult?

    fun setupObjectDetector()

    fun clearObjectDetector()
}