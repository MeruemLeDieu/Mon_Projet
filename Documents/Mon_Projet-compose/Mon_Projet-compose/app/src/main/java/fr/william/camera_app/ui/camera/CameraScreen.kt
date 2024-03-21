package fr.william.camera_app.ui.camera

//import com.google.firebase.Timestamp
//import fr.william.camera_app.data.datasource.labels.Position
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
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
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.william.camera_app.data.TfImageSegmentationHelper
import fr.william.camera_app.data.TfObjectDetectionHelper
import fr.william.camera_app.data.VideoClassifier
import fr.william.camera_app.ui.analyser.ImageAnalyzer
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch


@Composable
fun CameraScreen(
    isExpandedScreen: Boolean,
    //viewModel: CameraViewModel = hiltViewModel(LocalContext.current as ViewModelStoreOwner),
) {
    val context = LocalContext.current
    val viewModel: CameraViewModel = hiltViewModel(LocalContext.current as ViewModelStoreOwner)

    val segmentationResult by viewModel.segmentation.collectAsStateWithLifecycle()
    val objectDetectionResult by viewModel.objectDetection.collectAsStateWithLifecycle()
    val videoClassifierResult by viewModel.video.collectAsStateWithLifecycle()

    val segmentation by remember { derivedStateOf { segmentationResult.segmentation } }
    val objectDetection by remember { derivedStateOf { objectDetectionResult.results } }
    val classification by remember { derivedStateOf { videoClassifierResult.classification } }

    val segmentationResultInferenceTime by remember { derivedStateOf { segmentationResult.inferenceTime } }
    val objectResultInferenceTime by remember { derivedStateOf { objectDetectionResult.inferenceTime } }
    val videoInferenceTime by remember { derivedStateOf { videoClassifierResult.inferenceTime } }

    val imageWidth by remember { derivedStateOf { objectDetectionResult.imageWidth } }
    val imageHeight by remember { derivedStateOf { objectDetectionResult.imageHeight } }

    var coloredLabels by remember { mutableStateOf<List<ColorLabel>>(emptyList()) }

    var isSettingsExpanded by remember { mutableStateOf(false) }

    // Create a FormInputState to manage the input configuration for image segmentation
    val formInputState by viewModel.formInputState.collectAsStateWithLifecycle()

    val numThreads by remember { derivedStateOf { formInputState.numThreads } }
    val maxResults by remember { derivedStateOf { formInputState.maxResults } }
    val interpreter by remember { derivedStateOf { formInputState.interpreter } }
    val labels by remember { derivedStateOf { formInputState.labels } }
    val modelFile by remember { derivedStateOf { formInputState.modelFile } }
    val labelFile by remember { derivedStateOf { formInputState.labelFile } }
    val currentDelegate by remember { derivedStateOf { formInputState.currentDelegate } }
    val currentModel by remember { derivedStateOf { formInputState.currentModel } }
    val segmentationEnabled by remember { derivedStateOf { formInputState.segmentationEnabled } }
    val objectDetectionEnabled by remember { derivedStateOf { formInputState.objectDetectionEnabled } }
    val videoEnabled by remember { derivedStateOf { formInputState.videoEnabled } }


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

    val videoClassifier = remember {
        VideoClassifier(
            numThreads = numThreads,
            maxResults = maxResults,
            interpreter = interpreter,
            labels = labels,
            context = context,
            modelFile = modelFile,
            labelFile = labelFile,
        )
    }

    val analyzer = remember {
        ImageAnalyzer(
            segmenter = imageSegmentationHelper,
            objectDetectionHelper = imageObjectDetectionHelper,
            videoClassifier = videoClassifier,
            isSegmenterEnabled = segmentationEnabled,
            isObjectDetectionEnabled = objectDetectionEnabled,
            isVideoEnabled = videoEnabled,
        ) { segmentationResult, objectDetectionResult, videoClassifierResult ->
            MainScope().launch {
                segmentationResult.apply {
                    viewModel.updateSegmentationResult(
                        results ?: emptyList(),
                        inferenceTime,
                        imageWidth,
                        imageHeight,
                    )
                }
                objectDetectionResult.apply {
                    viewModel.updateObjectDetectionResult(
                        results,
                        inferenceTime,
                        imageWidth,
                        imageHeight,
                    )
                }
                videoClassifierResult.apply {
                    viewModel.updateVideoResult(
                        categories,
                        inferenceTime,
                        imageWidth,
                        imageHeight,
                    )
                }
            }
        }
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
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
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
                videoResult = classification,
                imageHeight = imageHeight,
                imageWidth = imageWidth,
                segmentationEnabled = segmentationEnabled,
                objectDetectionEnabled = objectDetectionEnabled,
                videoEnabled = videoEnabled,
            ) { coloredLabelsResult ->
                coloredLabels = coloredLabelsResult
            }
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(top = 16.dp),
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

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            classification?.toList()?.let {
                items(it) { category ->
                    key(category.index) {
                        Column(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Text(
                                text = category.label,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "Score: ${category.score}",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = category.displayName,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .wrapContentHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Segmentation Inference time: $segmentationResultInferenceTime ms",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Object Detection Inference time: $objectResultInferenceTime ms",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Video Classification Inference time: $videoInferenceTime ms",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = { isSettingsExpanded = !isSettingsExpanded },
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

          Column(
              modifier = Modifier
                  .fillMaxWidth()
                  .verticalScroll(rememberScrollState()),
          ) {
              // NumThreads
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

              // CurrentDelegate
              Text(
                  text = "Image Segmentation Model: $currentDelegate",
                  modifier = Modifier.padding(horizontal = 16.dp),
                  style = MaterialTheme.typography.titleLarge
              )
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

              // CurrentModel
              Text(
                  text = "Object Detection Model: $currentModel",
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

              // ModelFile
              Text(
                  text = "Video Classification Model: $modelFile",
                  modifier = Modifier.padding(horizontal = 16.dp),
                  style = MaterialTheme.typography.titleLarge
              )
              GroupRadio(
                  modifier = Modifier.padding(16.dp),
                  selectedOption = modelFile,
                  options = listOf(
                      "movinet_a0_stream_int8.tflite" to VideoClassifier.MODEL_MOVINET_A0_FILE,
                      "movinet_a1_stream_int8.tflite" to VideoClassifier.MODEL_MOVINET_A1_FILE,
                      "movinet_a2_stream_int8.tflite" to VideoClassifier.MODEL_MOVINET_A2_FILE,
                  ),
                  onOptionSelected = { model2 ->
                      viewModel.updateModelFile(model2)
                  }
              )

              // MaxResults
              Text(
                  text = "Video Classification MaxResults: $maxResults",
                  modifier = Modifier.padding(horizontal = 16.dp),
                  style = MaterialTheme.typography.titleLarge
              )
              Slider(
                  value = maxResults.toFloat(),
                  onValueChange = { value ->
                      viewModel.updateMaxResults(value.toInt())
                  },
                  valueRange = 1f..3f,
                  steps = 2,
                  modifier = Modifier
                      .fillMaxWidth()
                      .padding(16.dp)
              )

              // SegmentationEnabled
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

              // ObjectDetectionEnabled
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

              // VideoEnable
              Row(
                  modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 16.dp)
                      .padding(bottom = 16.dp),
                  verticalAlignment = Alignment.CenterVertically,
              ) {
                  Text(
                      text = "Video Classification",
                      modifier = Modifier
                          .weight(1f),
                      fontSize = 16.sp,
                      color = MaterialTheme.colorScheme.onBackground
                  )
                  RadioButton(
                      selected = videoEnabled,
                      onClick = { viewModel.updateVideoEnabled(!videoEnabled) }
                  )
              }
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

@Composable
fun GroupRadio(
    modifier: Modifier = Modifier,
    selectedOption: String,
    options: List<Pair<String, String>>,
    onOptionSelected: (String) -> Unit
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