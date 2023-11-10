package fr.william.camera_app.ui.camera

import android.util.Log
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import fr.william.camera_app.data.TfImageSegmentationHelper
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.tensorflow.lite.task.vision.segmenter.Segmentation

@Composable
fun CameraScreen(isExpandedScreen: Boolean) {
    val context = LocalContext.current

    var segmentation by remember { mutableStateOf(emptyList<Segmentation>()) }
    var coloredLabels by remember { mutableStateOf(emptyList<ColorLabel>()) }
    var inferenceTime by remember { mutableLongStateOf(0L) }
    var imageWidth by remember { mutableIntStateOf(500) }
    var imageHeight by remember { mutableIntStateOf(500) }

    

    val imageSegmentationHelper = remember {
        TfImageSegmentationHelper(
            numThreads = 2,
            currentDelegate = TfImageSegmentationHelper.DELEGATE_CPU,
            context = context,
        )
    }

    val analyzer = remember {
        ImageSegmenterAnalyzer(
            segmenter = imageSegmentationHelper,
            onResult = { segmentationResult ->
                MainScope().launch {
                    segmentation = segmentationResult.results ?: emptyList()
                    inferenceTime = segmentationResult.inferenceTime
                    imageWidth = segmentationResult.imageWidth
                    imageHeight = segmentationResult.imageHeight
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                imageHeight = imageHeight,
                imageWidth = imageWidth,
            ) { coloredLabelsResult ->
                coloredLabels = coloredLabelsResult
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(coloredLabels) { colorLabel ->
                key(colorLabel.id) { // Use a unique identifier for each item
                    Text(
                        modifier = Modifier.
                        background(
                            color = colorLabel.rgbColor.asComposeColor(),
                            shape = RoundedCornerShape(8.dp)
                        ).padding(8.dp),
                        text = colorLabel.label,
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        color = Color.White,
                    )
                }
            }
        }

        Text(
            text = "Inference time: $inferenceTime ms",
            modifier = Modifier.padding(16.dp),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
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