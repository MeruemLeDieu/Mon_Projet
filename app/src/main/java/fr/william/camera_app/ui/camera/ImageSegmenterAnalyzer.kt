package fr.william.camera_app.ui.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import fr.william.camera_app.data.SegmentationResult
import fr.william.camera_app.data.TfImageSegmentationHelper

class ImageSegmenterAnalyzer(
    private val segmenter: TfImageSegmentationHelper,
    private val onResult: (SegmentationResult) -> Unit
) : ImageAnalysis.Analyzer {
    private var frameSkipCounter = 0

    override fun analyze(image: ImageProxy) {

        val rotationDegrees = image.imageInfo.rotationDegrees
        val bitmap = image.toBitmap()

        val results = segmenter.segment(bitmap, rotationDegrees)
        onResult(results)


        image.close()
    }
}