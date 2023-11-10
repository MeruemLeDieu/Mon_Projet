package fr.william.camera_app.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color.argb
import android.graphics.Color.blue
import android.graphics.Color.green
import android.graphics.Color.red
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import fr.william.camera_app.R
import org.tensorflow.lite.task.vision.detector.Detection
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
    objectDetection: List<Detection>?,
    imageHeight: Int,
    imageWidth: Int,
    segmentationEnabled: Boolean,
    objectDetectionEnabled: Boolean,
    callback: (List<ColorLabel>) -> Unit,
) {
    var scaleBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val boxPaint = remember { Paint() }
    val textBackgroundPaint = remember { Paint() }
    val textPaint = remember { Paint() }
    val bounds = remember { Rect() }
    val textMeasure = rememberTextMeasurer()

    LaunchedEffect(segmentResult) {
        textBackgroundPaint.apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.FILL
            textSize = 25f
        }

        textPaint.apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
            textSize = 25f
        }

        boxPaint.apply {
            color = android.graphics.Color.RED
            strokeWidth = 4F
            style = Paint.Style.STROKE
        }

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
        modifier = Modifier.fillMaxSize()
    ) {
        try {
            scaleBitmap?.let { imageBitmap ->
                val scaleFactor = max(size.width * 1f / imageWidth, size.height * 1f / imageHeight)
                val scaleWidth = (imageWidth * scaleFactor).toInt()
                val scaleHeight = (imageHeight * scaleFactor).toInt()

                if(segmentationEnabled) {
                    scaleBitmap = Bitmap.createScaledBitmap(imageBitmap, scaleWidth, scaleHeight, false)

                    drawImage(
                        image = scaleBitmap!!.asImageBitmap(),
                        topLeft = Offset(0f, 0f),
                        alpha = 0.5f,
                        blendMode = BlendMode.SrcOver
                    )
                }
               if (objectDetectionEnabled){
                   objectDetection?.forEach { result ->
                       val boundingBox = result.boundingBox

                       val top = boundingBox.top * scaleFactor
                       val bottom = boundingBox.bottom * scaleFactor
                       val left = boundingBox.left * scaleFactor
                       val right = boundingBox.right * scaleFactor

                       // Draw bounding box around detected objects
                       val drawableRect = RectF(left, top, right, bottom)
                       drawRect(
                           color = boxPaint.color.asComposeColor(),
                           topLeft = Offset(left, top),
                           size = Size(drawableRect.width(), drawableRect.height()),
                           alpha = 0.5f
                       )

                       val drawableText =
                           "${result.categories[0].label} ${String.format("%.2f", result.categories[0].score)}"

                       // Draw rect behind display text
                       textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
                       val textWidth = bounds.width()
                       val textHeight = bounds.height()

                       // Adjust the position of the rect and text
                       val rectAndTextTop = top - textHeight - 8
                       drawRect(
                           color = textBackgroundPaint.color.asComposeColor(),
                           topLeft = Offset(left, rectAndTextTop),
                           size = Size(left + textWidth + 8, rectAndTextTop + textHeight + 8),
                           alpha = 0.5f
                       )

                       // Draw text for detected object
                       drawText(
                           textMeasurer = textMeasure,
                           text = drawableText,
                           style = TextStyle(color = textPaint.color.asComposeColor()),
                           topLeft = Offset(left, rectAndTextTop + bounds.height())
                       )
                   }
               }

                // Combine object detection bounding boxes with segmentation masks

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}
