package fr.william.camera_app.ui.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import fr.william.camera_app.data.SegmentationResult
import fr.william.camera_app.data.TfImageSegmentationHelper
import fr.william.camera_app.ui.centerCrop
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.segmenter.Segmentation

class ImageSegmenterAnalyzer(
    private val segmenter: TfImageSegmentationHelper,
    private val onResult: (SegmentationResult) -> Unit
) : ImageAnalysis.Analyzer {
    private var frameSkipCounter = 0

    override fun analyze(image: ImageProxy) {
        if (frameSkipCounter % 5 == 0) {
            val rotationDegrees = image.imageInfo.rotationDegrees
            val bitmap = image
                .toBitmap()

            val results = segmenter.segment(bitmap, rotationDegrees)
            onResult(results)
        }
        frameSkipCounter++

        image.close()
    }
}