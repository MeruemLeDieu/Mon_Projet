package fr.william.camera_app.domain

import android.graphics.Bitmap
import fr.william.camera_app.data.SegmentationResult
import org.tensorflow.lite.task.vision.segmenter.Segmentation

interface ImageSegmenterHelper {
    fun segment(image: Bitmap, imageRotation: Int) : SegmentationResult?

    fun setupImageSegmenter()

    fun clearImageSegmenter()
}