package fr.william.camera_app.ui.camera

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.william.camera_app.data.repository.labels.LabelsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.segmenter.Segmentation
import javax.inject.Inject


@HiltViewModel
class CameraViewModel @Inject constructor(
    private val labelsRepository: LabelsRepository,
    @ApplicationContext private val context: Context

) : ViewModel() {
    private companion object {
        private const val TAG = "CameraViewModel"
    }

    private val _uiState = MutableStateFlow<CameraUiState>(CameraUiState.Loading(false))

    val state: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val _segmentation =
        MutableStateFlow(CameraUiState.SegmentationResult(listOf(), 0, 500, 500))
    val segmentation: StateFlow<CameraUiState.SegmentationResult> = _segmentation.asStateFlow()

    private val _objectDetection =
        MutableStateFlow(CameraUiState.ObjectDetectionResult(mutableListOf(), 0, 500, 500))
    val objectDetection: StateFlow<CameraUiState.ObjectDetectionResult> =
        _objectDetection.asStateFlow()

    private val _video =
        MutableStateFlow(CameraUiState.VideoResult(mutableListOf(), 0, 500, 500))
    val video: StateFlow<CameraUiState.VideoResult> =
        _video.asStateFlow()

    fun updateSegmentationResult(
        segmentation: List<Segmentation>,
        inferenceTime: Long,
        imageWidth: Int,
        imageHeight: Int
    ) {
        _segmentation.update {
            it.copy(
                segmentation = segmentation,
                inferenceTime = inferenceTime,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
            )
        }
    }

    fun updateObjectDetectionResult(
        results: MutableList<Detection>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        _objectDetection.update {
            it.copy(
                results = results,
                inferenceTime = inferenceTime,
                imageHeight = imageHeight,
                imageWidth = imageWidth,
            )
        }
    }

    fun updateVideoResult(
        classification: MutableList<Category>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        _video.update {
            it.copy(
                classification = classification,
                inferenceTime = inferenceTime,
                imageHeight = imageHeight,
                imageWidth = imageWidth,
            )
        }
    }

    private val _formInputState = MutableStateFlow(CameraUiState.FormInputState(context))
    val formInputState: StateFlow<CameraUiState.FormInputState>
        get() = _formInputState.asStateFlow()

    /*fun initialize(context: Context) {
        _formInputState.value = CameraUiState.FormInputState(context)
    }*/

    // Additional methods to update formInputState based on user input
    /*fun updateContext(context: Context) {
        _formInputState.value = _formInputState.value.copy(context = context)
    }*/

    fun updateModelFile(modelFile: String) {
        _formInputState.value = _formInputState.value.copy(modelFile = modelFile)
    }

    /*fun updateLabelFile(labelFile: String) {
        _formInputState.value = _formInputState.value.copy(labelFile = labelFile)
    }*/

    fun updateMaxResults(maxResults: Int) {
        _formInputState.value = _formInputState.value.copy(maxResults = maxResults)
    }

    /*fun updateInterpreter(interpreter: Interpreter) {
        _formInputState.value = _formInputState.value.copy(interpreter = interpreter)
    }*/

    /*fun updateLabels(labels: List<String>) {
        _formInputState.value = _formInputState.value.copy(labels = labels)
    }*/

    fun updateNumThreads(numThreads: Int) {
        _formInputState.value = _formInputState.value.copy(numThreads = numThreads)
    }

    fun updateCurrentDelegate(currentDelegate: Int) {
        _formInputState.value = _formInputState.value.copy(currentDelegate = currentDelegate)
    }

    fun updateCurrentModel(currentModel: Int) {
        _formInputState.value = _formInputState.value.copy(currentModel = currentModel)
    }

    fun updateSegmentationEnabled(segmentationEnabled: Boolean) {
        _formInputState.value =
            _formInputState.value.copy(segmentationEnabled = segmentationEnabled)
    }

    fun updateObjectDetectionEnabled(objectDetectionEnabled: Boolean) {
        _formInputState.value =
            _formInputState.value.copy(objectDetectionEnabled = objectDetectionEnabled)
    }

    fun updateVideoEnabled(videoEnable: Boolean) {
        _formInputState.value =
            _formInputState.value.copy(videoEnabled = videoEnable)
    }

}