package fr.william.camera_app.ui.camera

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.william.camera_app.data.datasource.labels.Label
import fr.william.camera_app.data.datasource.labels.Position
import fr.william.camera_app.data.repository.labels.LabelsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.segmenter.Segmentation
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val labelsRepository: LabelsRepository

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

    private val _formInputState = MutableStateFlow(CameraUiState.FormInputState())
    val formInputState: StateFlow<CameraUiState.FormInputState>
        get() = _formInputState.asStateFlow()

    // Additional methods to update formInputState based on user input
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

}