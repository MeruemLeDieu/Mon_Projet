package fr.william.camera_app.ui.camera

import android.graphics.Bitmap
import android.graphics.Color.argb
import android.graphics.Color.blue
import android.graphics.Color.green
import android.graphics.Color.red
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import org.tensorflow.lite.task.vision.segmenter.Segmentation
import kotlin.math.max

data class ColorLabel(
    val id: Int,
    val label: String,
    val rgbColor: Int,
    var isExist: Boolean = false
) {
    fun getColor(): Int {
        return if (id == 0) android.graphics.Color.TRANSPARENT else argb(128, red(rgbColor), green(rgbColor), blue(rgbColor))
    }
}

@Composable
fun OverlayView(
    segmentResult: List<Segmentation>?,
    imageHeight: Int,
    imageWidth: Int,
    callback: (List<ColorLabel>) -> Unit,
) {
    var scaleBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(segmentResult) {
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
                colorLabels.getOrNull(maskArray[i].toInt())?.isExist = true
                pixels[i] = colorLabels.getOrNull(maskArray[i].toInt())?.getColor() ?: 0
            }

            scaleBitmap = Bitmap.createBitmap(
                pixels,
                maskTensor.width,
                maskTensor.height,
                Bitmap.Config.ARGB_8888
            )

            callback(colorLabels.filter { it.isExist })
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
    ) {
        scaleBitmap?.let { imageBitmap ->
            val scaleFactor = max(size.width * 1f / imageWidth, size.height * 1f / imageHeight)
            val scaleWidth = (imageWidth * scaleFactor).toInt()
            val scaleHeight = (imageHeight * scaleFactor).toInt()

            scaleBitmap = Bitmap.createScaledBitmap(imageBitmap, scaleWidth, scaleHeight, false)

            drawImage(
                image = scaleBitmap!!.asImageBitmap(),
                topLeft = Offset(0f, 0f),
                alpha = 0.5f,
                blendMode = BlendMode.SrcOver
            )
        }
    }
}
