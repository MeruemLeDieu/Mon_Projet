package fr.william.camera_app.domain

import android.graphics.Bitmap
import org.tensorflow.lite.task.vision.segmenter.Segmentation


data class SegmentationResult(
    val results: MutableList<Segmentation>?,
    val inferenceTime: Long,
    val imageHeight: Int,
    val imageWidth: Int
)
interface ImageHelper {
    fun segment(image: Bitmap, imageRotation: Int) : SegmentationResult?

    fun setupImageSegmenter()

    fun clearImageSegmenter()
}