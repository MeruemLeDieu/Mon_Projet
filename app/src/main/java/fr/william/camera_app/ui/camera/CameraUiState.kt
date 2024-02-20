package fr.william.camera_app.ui.camera

import android.content.Context
import fr.william.camera_app.data.TfImageSegmentationHelper
import fr.william.camera_app.data.TfObjectDetectionHelper
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.segmenter.Segmentation
import java.time.Instant

sealed interface CameraUiState {
    data class SegmentationResult(
        val segmentation: List<Segmentation>,
        val inferenceTime: Long,
        val imageWidth: Int,
        val imageHeight: Int
    ) : CameraUiState

    data class ObjectDetectionResult(
        val results: MutableList<Detection>?,
        val inferenceTime: Long,
        val imageHeight: Int,
        val imageWidth: Int
    ) : CameraUiState

    data class VideoResult(
        val classification: MutableList<ImageClassifier>?,
        val inferenceTime: Long,
        val imageHeight: Int,
        val imageWidth: Int
    ) : CameraUiState

    data class FormInputState constructor(
        val context: Context,
        val modelFile: String = "movinet_a0_stream_int8.tflite",
        val labelFile: String = "kinetics600_label_map.txt",
        val numThreads: Int = 1,
        val currentDelegate: Int = TfImageSegmentationHelper.DELEGATE_CPU,
        val currentModel: Int = TfObjectDetectionHelper.MODEL_EFFICIENTDETV0,
        val maxResults: Int = 3,
        val interpreter: Interpreter = Interpreter(FileUtil.loadMappedFile(context, modelFile)),
        val labels: List<String> = FileUtil.loadLabels(context, labelFile),
        val segmentationEnabled: Boolean = true,
        val objectDetectionEnabled: Boolean = true,
        val videoEnabled: Boolean = true,
    )
    @JvmInline
    value class Loading(val isLoading: Boolean = false) : CameraUiState

    @JvmInline
    value class Error(val message: String) : CameraUiState

    class LabelAdded(val timestamp: Instant) : CameraUiState
}