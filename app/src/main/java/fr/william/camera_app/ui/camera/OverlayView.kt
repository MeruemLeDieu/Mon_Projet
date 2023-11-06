package fr.william.camera_app.ui.camera

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import org.tensorflow.lite.task.vision.segmenter.Segmentation


interface OverlayViewListener {
    fun onLabels(colorLabels: List<ColorLabel>)
}

data class ColorLabel(
    val id: Int,
    val label: String,
    val rgbColor: Int,
    var isExist: Boolean = false
) {

    fun getColor(): Int {
        // Use completely transparent for the background color.
        return rgbColor and 0x00FFFFFF
    }
}
@Composable
fun OverlayView(
    listener: (List<ColorLabel>) -> Unit
) {
    var scaleBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val context = LocalContext.current

    LaunchedEffect(context) {
        // Simulate setting results
        val imageWidth = 800 // Replace with your actual image width
        val imageHeight = 600 // Replace with your actual image height
        val segmentResult: List<Segmentation>? = null // Replace with your data

        if (!segmentResult.isNullOrEmpty()) {
            val colorLabels = segmentResult[0].coloredLabels.mapIndexed { index, coloredLabel ->
                ColorLabel(
                    index,
                    coloredLabel.getlabel(),
                    coloredLabel.argb
                )
            }

            val maskTensor = segmentResult[0].masks[0]
            val maskArray = maskTensor.buffer.array()
            val pixels = IntArray(maskArray.size)

            for (i in maskArray.indices) {
                val colorLabel = colorLabels[maskArray[i].toInt()].apply {
                    isExist = true
                }
                val color = colorLabel.getColor()
                pixels[i] = color
            }

            val image = Bitmap.createBitmap(
                pixels,
                maskTensor.width,
                maskTensor.height,
                Bitmap.Config.ARGB_8888
            )

            val scaleFactor = maxOf(
                context.resources.displayMetrics.widthPixels.toFloat() / imageWidth,
                context.resources.displayMetrics.heightPixels.toFloat() / imageHeight
            )
            val scaleWidth = (imageWidth * scaleFactor).toInt()
            val scaleHeight = (imageHeight * scaleFactor).toInt()

            scaleBitmap = Bitmap.createScaledBitmap(image, scaleWidth, scaleHeight, false)
            listener(colorLabels.filter { it.isExist })
        }
    }

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        scaleBitmap?.let { imageBitmap ->
            scale(imageBitmap.width.toFloat() / size.width, imageBitmap.height.toFloat() / size.height) {
                drawImage(imageBitmap.asImageBitmap())
            }
        }
    }

}
