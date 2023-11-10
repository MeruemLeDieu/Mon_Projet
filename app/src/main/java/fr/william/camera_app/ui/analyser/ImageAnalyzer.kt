package fr.william.camera_app.ui.analyser

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import fr.william.camera_app.data.SegmentationResult
import fr.william.camera_app.data.TfImageSegmentationHelper
import fr.william.camera_app.data.TfObjectDetectionHelper
import fr.william.camera_app.domain.ObjectDetectionResult

class ImageAnalyzer(
    private val segmenter: TfImageSegmentationHelper,
    private val objectDetectionHelper: TfObjectDetectionHelper,
    private val onResult: (SegmentationResult, ObjectDetectionResult) -> Unit
) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {

        val rotationDegrees = image.imageInfo.rotationDegrees
        val bitmap = image.toBitmap()

        val results = segmenter.segment(bitmap, rotationDegrees)
        val objectDetectionResult = objectDetectionHelper.detectObject(bitmap, rotationDegrees)
        onResult(results, objectDetectionResult)


        image.close()
    }
}