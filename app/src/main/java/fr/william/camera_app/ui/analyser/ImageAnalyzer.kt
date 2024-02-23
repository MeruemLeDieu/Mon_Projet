package fr.william.camera_app.ui.analyser

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import fr.william.camera_app.data.TfImageSegmentationHelper
import fr.william.camera_app.data.TfObjectDetectionHelper
import fr.william.camera_app.data.VideoClassifier
import fr.william.camera_app.domain.ObjectDetectionResult
import fr.william.camera_app.domain.SegmentationResult
import fr.william.camera_app.domain.VideoClassifierResult

//import fr.william.camera_app.domain.VideoClassifierResult

class ImageAnalyzer(
    private val segmenter: TfImageSegmentationHelper,
    private val objectDetectionHelper: TfObjectDetectionHelper,
    private val videoClassifier: VideoClassifier,
    private val isSegmenterEnabled: Boolean,
    private val isObjectDetectionEnabled: Boolean,
    private val isVideoEnabled: Boolean,
    private val onResult: (SegmentationResult, ObjectDetectionResult, VideoClassifierResult ) -> Unit
) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        val rotationDegrees = image.imageInfo.rotationDegrees
        val bitmap = image.toBitmap()

        val results = isSegmenterEnabled.takeIf { it }?.let {
            segmenter.segment(bitmap, rotationDegrees)
        } ?: SegmentationResult(null, 0, 500, 500)

        val objectDetectionResult = isObjectDetectionEnabled.takeIf { it }?.let {
            objectDetectionHelper.detectObject(bitmap, rotationDegrees)
        } ?: ObjectDetectionResult(null, 0, 500, 500)

        val videoResults = isVideoEnabled.takeIf { it }?.let {
            videoClassifier.classify(bitmap)
        } ?: VideoClassifierResult(null, 0, 500, 500)

        onResult(results, objectDetectionResult, videoResults)
        image.close()
    }
}