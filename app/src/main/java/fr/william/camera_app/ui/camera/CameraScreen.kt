package fr.william.camera_app.ui.camera

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import fr.william.camera_app.data.TfImageSegmentationHelper
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.segmenter.ColoredLabel

@Composable
fun CameraScreen(isExpandedScreen: Boolean) {
    val context = LocalContext.current

    var segmentation by remember { mutableStateOf(emptyList<ColorLabel>()) }
    var masks by remember { mutableStateOf(emptyList<Bitmap>()) }

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
                val coloredLabels = segmentationResult.results?.get(0)?.coloredLabels ?: emptyList()
                segmentation = coloredLabels.mapIndexed { index, coloredLabel ->
                    ColorLabel(
                        index,
                        coloredLabel.getlabel(),
                        coloredLabel.argb
                    )
                }
                segmentationResult.results?.let { results ->
                    val maskTensor = results[0].masks[0]
                    masks = createMasks(coloredLabels, maskTensor)
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

    ){
        Box(
            modifier = Modifier.fillMaxWidth()
                .height(500.dp)
        ) {
            CameraPreview(controller, Modifier.fillMaxSize())
            Canvas(
                modifier = Modifier.fillMaxSize(),
                onDraw = {
                    // Set a transparent background color
                    drawRect(color = Color.Transparent, size = this.size)
                    masks.forEachIndexed { index, maskBitmap ->
                        val colorLabel = segmentation.getOrNull(index)
                        val scaleBitmap = Bitmap.createScaledBitmap(
                            maskBitmap,
                            size.width.toInt(),
                            size.height.toInt(),
                            false
                        )
                        if (colorLabel != null) {
                            drawImage(
                                image = scaleBitmap.asImageBitmap(),
                                topLeft = Offset(0f, 0f),
                                alpha = 0.5f,
                                blendMode = BlendMode.Src
                            )
                        }
                        // Recycle the scaled bitmap
                        scaleBitmap.recycle()
                    }
                }

            )




        }
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            items(segmentation) { colorLabel ->
                val maskIndex = colorLabel.id
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = colorLabel.label,
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }

}

fun createMasks(coloredLabels: List<ColoredLabel>, maskTensor: TensorImage): List<Bitmap> {
    val bitmaps = mutableListOf<Bitmap>()
    coloredLabels.forEach { coloredLabel ->
        val maskArray = maskTensor.buffer.array()
        val pixels = IntArray(maskArray.size)

        for (i in maskArray.indices) {
            val colorLabel = coloredLabels[maskArray[i].toInt()].color
            pixels[i] = colorLabel.toArgb()
        }

        // Log pixel values
        Log.d("MaskCreation", "Pixels: ${pixels.joinToString(", ")}")

        val bitmap = Bitmap.createBitmap(
            pixels,
            maskTensor.width,
            maskTensor.height,
            Bitmap.Config.ARGB_8888
        )
        bitmaps.add(bitmap)
    }
    return bitmaps
}
