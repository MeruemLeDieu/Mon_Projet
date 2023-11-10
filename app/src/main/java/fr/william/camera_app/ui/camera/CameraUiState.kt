package fr.william.camera_app.ui.camera

import fr.william.camera_app.data.TfImageSegmentationHelper
import fr.william.camera_app.data.TfObjectDetectionHelper
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.segmenter.Segmentation

sealed interface CameraUiState {
    data class SegmentationResult(
        val segmentation: List<Segmentation>,
        val inferenceTime : Long,
        val imageWidth : Int,
        val imageHeight : Int
    ) : CameraUiState

    data class ObjectDetectionResult(
        val results: MutableList<Detection>?,
        val inferenceTime: Long,
        val imageHeight: Int,
        val imageWidth: Int
    ) : CameraUiState

    data class FormInputState(
        val numThreads: Int = 2,
        val currentDelegate: Int = TfImageSegmentationHelper.DELEGATE_CPU,
        val currentModel: Int = TfObjectDetectionHelper.MODEL_EFFICIENTDETV0,
        val segmentationEnabled: Boolean = true,
        val objectDetectionEnabled: Boolean = true,
    )
    @JvmInline
    value class Loading(val isLoading: Boolean = false) : CameraUiState
}