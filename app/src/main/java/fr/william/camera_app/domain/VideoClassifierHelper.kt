package fr.william.camera_app.domain

import android.graphics.Bitmap
import android.util.Size
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.nio.ByteBuffer

data class VideoClassifierResult(
    val videoResults: MutableList<ImageClassifier>?,
    val inferenceTime: Long,
    val imageHeight: Int,
    val imageWidth: Int
)
interface VideoClassifierHelper {
    fun initializeInput(): HashMap<String, Any>

    fun initializeOutput(): HashMap<String, Any>

    fun classify(inputBitmap: Bitmap): List<Category>

    fun getInputSize(): Size

    fun preprocessInputImage(bitmap: Bitmap): TensorImage

    fun postprocessOutputLogits(logitsByteBuffer: ByteBuffer): MutableList<Category>

    fun close()

    fun reset()
}