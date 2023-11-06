package fr.william.camera_app.ui

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.segmenter.ImageSegmenter
import org.tensorflow.lite.task.vision.segmenter.OutputType
import org.tensorflow.lite.task.vision.segmenter.Segmentation

class ImageSegmentationHelper(
    var numThreads: Int = 2,
    var currentDelegate: Int = 0,
    val context: Context,
    val imageSegmentationListener: SegmentationListener?
) {
    private var imageSegmenter: ImageSegmenter? = null

    init {
        setupImageSegmenter()
    }

    fun clearImageSegmenter() {
        imageSegmenter = null
    }

    private fun setupImageSegmenter() {
        // Create the base options for the segment
        val optionsBuilder =
            ImageSegmenter.ImageSegmenterOptions.builder()

        // Set general segmentation options, including number of used threads
        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads)

        // Use the specified hardware for running the model. Default to CPU
        when (currentDelegate) {
            DELEGATE_CPU -> {
                // Default
            }
            DELEGATE_GPU -> {
                if (CompatibilityList().isDelegateSupportedOnThisDevice) {
                    baseOptionsBuilder.useGpu()
                } else {
                    imageSegmentationListener?.onError("GPU is not supported on this device")
                }
            }
            DELEGATE_NNAPI -> {
                baseOptionsBuilder.useNnapi()
            }
        }

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        /*
        CATEGORY_MASK is being specifically used to predict the available objects
        based on individual pixels in this sample. The other option available for
        OutputType, CONFIDENCE_MAP, provides a gray scale mapping of the image
        where each pixel has a confidence score applied to it from 0.0f to 1.0f
         */
        optionsBuilder.setOutputType(OutputType.CATEGORY_MASK)
        try {
            imageSegmenter =
                ImageSegmenter.createFromFileAndOptions(
                    context,
                    MODEL_DEEPLABV3,
                    optionsBuilder.build()
                )
        } catch (e: IllegalStateException) {
            imageSegmentationListener?.onError(
                "Image segmentation failed to initialize. See error logs for details"
            )
            Log.e(TAG, "TFLite failed to load model with error: " + e.message)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun segment(image: Bitmap, imageRotation: Int) {

        if (imageSegmenter == null) {
            setupImageSegmenter()
        }

        // Inference time is the difference between the system time at the start and finish of the
        // process
        var inferenceTime = SystemClock.uptimeMillis()

        // Create preprocessor for the image.
        // See https://www.tensorflow.org/lite/inference_with_metadata/
        //            lite_support#imageprocessor_architecture
        val imageProcessor =
            ImageProcessor.Builder()
                .add(Rot90Op(-imageRotation / 90))
                .build()

        // Preprocess the image and convert it into a TensorImage for segmentation.
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))

        val segmentResult = imageSegmenter?.segment(tensorImage)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        imageSegmentationListener?.onResults(
            segmentResult,
            inferenceTime,
            tensorImage.height,
            tensorImage.width
        )
    }

    interface SegmentationListener {
        fun onError(error: String)
        fun onResults(
            results: List<Segmentation>?,
            inferenceTime: Long,
            imageHeight: Int,
            imageWidth: Int
        )
    }

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DELEGATE_NNAPI = 2
        const val MODEL_DEEPLABV3 = "deeplabv3.tflite"

        private const val TAG = "Image Segmentation Helper"
    }
}