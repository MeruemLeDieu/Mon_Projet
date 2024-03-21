package fr.william.camera_app.domain

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import fr.william.camera_app.data.VideoClassifier
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.label.Category
import java.nio.ByteBuffer

data class VideoClassifierResult(
    val categories: MutableList<Category>?,
    val inferenceTime: Long,
    val imageHeight: Int,
    val imageWidth: Int
)
interface VideoClassifierHelper {

    fun createFromFileAndLabelsAndOptions(
        context: Context,
        options: VideoClassifier.VideoClassifierOptions
    ): VideoClassifier

    fun initializeInput(): HashMap<String, Any>

    fun initializeOutput(): HashMap<String, Any>

    fun classify(inputBitmap: Bitmap): VideoClassifierResult

    fun getInputSize(): Size

    fun preprocessInputImage(bitmap: Bitmap): TensorImage

    fun postprocessOutputLogits(logitsByteBuffer: ByteBuffer): MutableList<Category>

    fun close()

    fun reset()
}