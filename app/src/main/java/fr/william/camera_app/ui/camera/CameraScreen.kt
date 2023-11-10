package fr.william.camera_app.ui.camera

import android.util.Log
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.william.camera_app.data.TfImageSegmentationHelper
import fr.william.camera_app.data.TfObjectDetectionHelper
import fr.william.camera_app.ui.analyser.ImageAnalyzer
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(
    isExpandedScreen: Boolean,
    viewModel: CameraViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    //segmentation
    val segmentationResult by viewModel.segmentation.collectAsStateWithLifecycle()

    val segmentation by remember { derivedStateOf { segmentationResult.segmentation } }
    val inferenceTime by remember { derivedStateOf { segmentationResult.inferenceTime } }
    val imageWidth by remember { derivedStateOf { segmentationResult.imageWidth } }
    val imageHeight by remember { derivedStateOf { segmentationResult.imageHeight } }
    var coloredLabels by remember { mutableStateOf<List<ColorLabel>>(emptyList()) }

    //object detection
    val objectDetectionResult by viewModel.objectDetection.collectAsStateWithLifecycle()

    val objectDetection by remember { derivedStateOf { objectDetectionResult.results } }

    var isSettingsExpanded by remember { mutableStateOf(false) }

    // Create a FormInputState to manage the input configuration for image segmentation
    val formInputState by viewModel.formInputState.collectAsStateWithLifecycle()

    val numThreads by remember { derivedStateOf { formInputState.numThreads } }
    val currentDelegate by remember { derivedStateOf { formInputState.currentDelegate } }
    val currentModel by remember { derivedStateOf { formInputState.currentModel } }
    val segmentationEnabled by remember { derivedStateOf { formInputState.segmentationEnabled } }
    val objectDetectionEnabled by remember { derivedStateOf { formInputState.objectDetectionEnabled } }


    // Use the values from formInputState to initialize the TfImageSegmentationHelper
    val imageSegmentationHelper = remember {
        TfImageSegmentationHelper(
            numThreads = numThreads,
            currentDelegate = currentDelegate,
            context = context,
        )
    }

    val imageObjectDetectionHelper = remember {
        TfObjectDetectionHelper(
            numThreads = numThreads,
            currentDelegate = currentDelegate,
            context = context,
            currentModel = currentModel,
        )
    }

    val analyzer = remember {
        ImageAnalyzer(
            segmenter = imageSegmentationHelper,
            objectDetectionHelper = imageObjectDetectionHelper,
            onResult = { segmentationResult, objectDetectionResult ->
                MainScope().launch {
                    segmentationResult.apply {
                        viewModel.updateSegmentationResult(
                            results ?: emptyList(),
                            inferenceTime,
                            imageWidth,
                            imageHeight,
                        )
                        Log.d("segmentationResult", "imageWidth: $imageWidth, imageHeight: $imageHeight")
                    }
                    objectDetectionResult.apply {
                        viewModel.updateObjectDetectionResult(
                            results,
                            inferenceTime,
                            imageWidth,
                            imageHeight,
                        )
                    }
                }
            }
        )
    }

    val controller = remember {
        LifecycleCameraController(context.applicationContext).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            setImageAnalysisAnalyzer(
                ContextCompat.getMainExecutor(context.applicationContext),
                analyzer
            )
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
        ) {
            CameraPreview(controller, Modifier.fillMaxSize())
            OverlayView(
                segmentResult = segmentation,
                objectDetection = objectDetection,
                imageHeight = imageHeight,
                imageWidth = imageWidth,
                segmentationEnabled = segmentationEnabled,
                objectDetectionEnabled = objectDetectionEnabled,
            ) { coloredLabelsResult ->
                coloredLabels = coloredLabelsResult
            }
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(coloredLabels) { colorLabel ->
                key(colorLabel.id) { // Use a unique identifier for each item
                    Text(
                        modifier = Modifier
                            .background(
                                color = colorLabel.rgbColor.asComposeColor(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
                        text = colorLabel.label,
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        color = Color.White,
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Inference time: $inferenceTime ms",
                modifier = Modifier
                    .padding(16.dp),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = { isSettingsExpanded = !isSettingsExpanded },
                modifier = Modifier
                    .padding(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

    }

    AnimatedVisibility(
        visible = isSettingsExpanded,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
    ) {
        ModalBottomSheet(
            modifier = Modifier
                .fillMaxWidth(),
            scrimColor = Color.Black.copy(alpha = 0.4f),
            sheetState = rememberModalBottomSheetState(
               skipPartiallyExpanded = true
            ),
            onDismissRequest = { isSettingsExpanded = false },
        ) {
            Text(
                text = "Number of Threads: $numThreads",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleLarge
            )
            Slider(
                value = numThreads.toFloat(),
                onValueChange = { value ->
                    viewModel.updateNumThreads(value.toInt())
                },
                valueRange = 1f..8f,
                steps = 7,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )


            // Delegate selection
            RadioGroup(
                modifier = Modifier.padding(horizontal = 16.dp),
                selectedOption = currentDelegate,
                options = listOf(
                    "CPU" to TfImageSegmentationHelper.DELEGATE_CPU,
                    "GPU" to TfImageSegmentationHelper.DELEGATE_GPU,
                    "NNAPI" to TfImageSegmentationHelper.DELEGATE_NNAPI
                ),
                onOptionSelected = { delegate ->
                    viewModel.updateCurrentDelegate(delegate)
                }
            )

            // Model selection
            Text(
                text = "Model: $currentModel",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleLarge
            )
            RadioGroup(
                modifier = Modifier.padding(16.dp),
                selectedOption = currentModel,
                options = listOf(
                    "EfficientDet Lite 0" to TfObjectDetectionHelper.MODEL_EFFICIENTDETV0,
                    "EfficientDet Lite 1" to TfObjectDetectionHelper.MODEL_EFFICIENTDETV1,
                    "EfficientDet Lite 2" to TfObjectDetectionHelper.MODEL_EFFICIENTDETV2,
                ),
                onOptionSelected = { model ->
                    viewModel.updateCurrentModel(model)
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Segmentation",
                    modifier = Modifier
                        .weight(1f),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                RadioButton(
                    selected = segmentationEnabled,
                    onClick = { viewModel.updateSegmentationEnabled(!segmentationEnabled) }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Object Detection",
                    modifier = Modifier
                        .weight(1f),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                RadioButton(
                    selected = objectDetectionEnabled,
                    onClick = { viewModel.updateObjectDetectionEnabled(!objectDetectionEnabled) }
                )
            }

        }
    }

}

fun Int.asComposeColor(): Color {
    return Color(
        red = android.graphics.Color.red(this) / 255f,
        green = android.graphics.Color.green(this) / 255f,
        blue = android.graphics.Color.blue(this) / 255f,
        alpha = android.graphics.Color.alpha(this) / 255f
    )
}

@Composable
fun RadioGroup(
    modifier: Modifier = Modifier,
    selectedOption: Int,
    options: List<Pair<String, Int>>,
    onOptionSelected: (Int) -> Unit
) {
    Column(
        modifier = modifier,
    ) {
        options.forEach { (text, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOptionSelected(value) }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = value == selectedOption,
                    onClick = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = text)
            }
        }
    }
}