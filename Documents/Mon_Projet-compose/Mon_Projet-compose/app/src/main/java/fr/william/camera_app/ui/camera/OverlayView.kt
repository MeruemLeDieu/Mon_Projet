package fr.william.camera_app.ui.camera

import android.graphics.Bitmap
import android.graphics.Color.argb
import android.graphics.Color.blue
import android.graphics.Color.green
import android.graphics.Color.red
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.segmenter.Segmentation
import kotlin.math.min

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
    videoResult: List<Category>?,
    imageHeight: Int,
    imageWidth: Int,
    segmentationEnabled: Boolean,
    objectDetectionEnabled: Boolean,
    videoEnabled: Boolean,
    callback: (List<ColorLabel>) -> Unit,
) {
    var scaleBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val boxPaint = remember { Paint() }
    val textBackgroundPaint = remember { Paint() }
    val textPaint = remember { Paint() }
    val bounds = remember { Rect() }
    val textMeasure = rememberTextMeasurer()

    LaunchedEffect(objectDetection) {
        textBackgroundPaint.apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.FILL
            textSize = 50f
        }

        textPaint.apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
            textSize = 50f
        }

        boxPaint.apply {
            color = android.graphics.Color.RED
            strokeWidth = 8F
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
        modifier = Modifier.fillMaxSize() //.height(500.dp)
    ) {
        try {

            scaleBitmap?.let { imageBitmap ->
                val scaleFactor = min(size.width * 1f / imageWidth, size.height * 1f / imageHeight)
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
                        val bottom = min(boundingBox.bottom * scaleFactor, size.height.toFloat())  // Ensure bottom doesn't exceed the available height
                        val left = boundingBox.left * scaleFactor
                        val right = min(boundingBox.right * scaleFactor, size.width)  // Ensure right doesn't exceed the available width

                        // Draw bounding box around detected objects
                        val drawableRect = RectF(left, top, right, bottom)
                        drawRect(
                            color = boxPaint.color.asComposeColor(),
                            topLeft = Offset(left, top),
                            size = Size(drawableRect.width(), drawableRect.height()),
                            alpha = 0.5f,
                            style = Stroke(width = boxPaint.strokeWidth)
                        )

                        val drawableText = "${result.categories[0].label} ${String.format("%.2f", result.categories[0].score)}"

                        // Draw rect behind display text
                        textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
                        val textWidth = bounds.width()
                        val textHeight = bounds.height()

                        // Adjust the position of the rect and text
                        //val rectAndTextTop = top - textHeight - 8
                        drawRect(
                            color = textBackgroundPaint.color.asComposeColor(),
                            topLeft = Offset(left, top),//rectAndTextTop),
                            size = Size(textWidth + 24f,  textHeight + 8f), //size = Size(left + textWidth + 8, (textHeight + 8).toFloat()), //rectAndTextTop + textHeight + 8),
                            alpha = 0.5f,
                            style = if (textBackgroundPaint.style == Paint.Style.FILL_AND_STROKE) Stroke(width = textBackgroundPaint.strokeWidth) else
                                Fill
                        )

                        // Draw text for detected object
                        drawText(
                            textMeasurer = textMeasure,
                            text = drawableText,
                            style = TextStyle(
                                color = textPaint.color.asComposeColor()
                            ),
                            topLeft = Offset(left, top - textHeight - 8 + bounds.height())//rectAndTextTop + bounds.height())
                        )
                    }
                }






                Log.d("OverlayView", """
    |Image Scaling and Analysis:
    |  Scale Factor: $scaleFactor
    |  Scaled Dimensions: ($scaleWidth x $scaleHeight)
    |  View Size: (${size.width} x ${size.height})
    |  Original Image Dimensions: $imageWidth x $imageHeight
    |Object Detection:
    |  Detection Results: $objectDetection
    |  Segmentation Result: $segmentResult
    |  Segmentation Enabled: $segmentationEnabled
    |  Object Detection Enabled: $objectDetectionEnabled
    |  Callback: $callback
""".trimMargin())



            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}