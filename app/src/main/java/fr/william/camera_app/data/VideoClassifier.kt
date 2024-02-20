package fr.william.camera_app.data

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import fr.william.camera_app.domain.VideoClassifierHelper
import fr.william.camera_app.ui.utils.CalculateUtils
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.label.Category
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min
//import fr.william.camera_app.domain.VideoClassifierResult


class VideoClassifier constructor(
    val interpreter: Interpreter,
    val labels: List<String>,
    val maxResults: Int? = 3,
    val numThreads: Int = 1,
    val context: Context,
    val modelFile: String,
    val labelFile: String,
    //val options: VideoClassifierOptions
): VideoClassifierHelper {
    private val inputShape = interpreter
        .getInputTensorFromSignature(IMAGE_INPUT_NAME, SIGNATURE_KEY)
        .shape()
    private val outputCategoryCount = interpreter
        .getOutputTensorFromSignature(LOGITS_OUTPUT_NAME, SIGNATURE_KEY)
        .shape()[1]
    private val inputHeight = inputShape[2]
    private val inputWidth = inputShape[3]
    private var inputState = HashMap<String, Any>()
    private val lock = Any()

    companion object {
        const val IMAGE_INPUT_NAME = "image"
        const val LOGITS_OUTPUT_NAME = "logits"
        const val SIGNATURE_KEY = "serving_default"
        const val INPUT_MEAN = 0f
        const val INPUT_STD = 255f
        const val REQUEST_CODE_PERMISSIONS = 10
        const val TAG = "TFLite-VidClassify"
        val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        const val MAX_RESULT = 3
        const val MODEL_MOVINET_A0_FILE = "movinet_a0_stream_int8.tflite"
        const val MODEL_MOVINET_A1_FILE = "movinet_a1_stream_int8.tflite"
        const val MODEL_MOVINET_A2_FILE = "movinet_a2_stream_int8.tflite"
        const val MODEL_LABEL_FILE = "kinetics600_label_map.txt"
        const val MODEL_FPS = 5 // Ensure the input images are fed to the model at this fps.
        const val MODEL_FPS_ERROR_RANGE = 0.1 // Acceptable error range in fps.
        const val MAX_CAPTURE_FPS = 20

        fun createFromFileAndLabelsAndOptions(
            context: Context,
            modelFile: String,
            labelFile: String,
            options: VideoClassifierOptions
        ): VideoClassifier {
            // Create a TFLite interpreter from the TFLite model file.
            val interpreterOptions = Interpreter.Options().apply {
                numThreads = options.numThreads
            }
            val interpreter =
                Interpreter(FileUtil.loadMappedFile(context, modelFile))

            // Load the label file.
            val labels = FileUtil.loadLabels(context, labelFile)

            // Save the max result option.
            val maxResults = if (options.maxResults > 0 && options.maxResults <= labels.size)
                options.maxResults else null

            return VideoClassifier(interpreter, labels, maxResults, options.numThreads, context, modelFile, labelFile)
        }
    }

    init {
        if (outputCategoryCount != labels.size)
            throw java.lang.IllegalArgumentException(
                "Label list size doesn't match with model output shape " +
                        "(${labels.size} != $outputCategoryCount"
            )
        inputState = initializeInput()
    }

    /**
     * Initialize the input objects and fill them with zeros.
     */
    override fun initializeInput(): HashMap<String, Any> {
        val inputs = HashMap<String, Any>()
        for (inputName in interpreter.getSignatureInputs(SIGNATURE_KEY)) {
            // Skip the input image tensor as it'll be fed in later.
            if (inputName.equals(IMAGE_INPUT_NAME))
                continue

            // Initialize a ByteBuffer filled with zeros as an initial input of the TFLite model.
            val tensor = interpreter.getInputTensorFromSignature(inputName, SIGNATURE_KEY)
            val byteBuffer = ByteBuffer.allocateDirect(tensor.numBytes())
            byteBuffer.order(ByteOrder.nativeOrder())
            inputs[inputName] = byteBuffer
        }

        return inputs
    }

    /**
     * Initialize the output objects to store the TFLite model outputs.
     */
    override fun initializeOutput(): HashMap<String, Any> {
        val outputs = HashMap<String, Any>()
        for (outputName in interpreter.getSignatureOutputs(SIGNATURE_KEY)) {
            // Initialize a ByteBuffer to store the output of the TFLite model.
            val tensor = interpreter.getOutputTensorFromSignature(outputName, SIGNATURE_KEY)
            val byteBuffer = ByteBuffer.allocateDirect(tensor.numBytes())
            byteBuffer.order(ByteOrder.nativeOrder())
            outputs[outputName] = byteBuffer
        }

        return outputs
    }

    /**
     * Run classify and return a list include action and score.
     */
    override fun classify(inputBitmap: Bitmap): List<Category> {
        // As this model is stateful, ensure there's only one inference going on at once.
        synchronized(lock) {
            // Prepare inputs.
            val tensorImage = preprocessInputImage(inputBitmap)
            inputState[IMAGE_INPUT_NAME] = tensorImage.buffer

            // Initialize a placeholder to store the output objects.
            val outputs = initializeOutput()

            // Run inference using the TFLite model.
            interpreter.runSignature(inputState, outputs)

            // Post-process the outputs.
            var categories = postprocessOutputLogits(outputs[LOGITS_OUTPUT_NAME] as ByteBuffer)

            // Store the output states to feed as input for the next frame.
            outputs.remove(LOGITS_OUTPUT_NAME)
            inputState = outputs

            // Sort the output and return only the top K results.
            categories.sortByDescending { it.score }

            // Take only maxResults number of result.
            maxResults?.let {
                categories = categories.subList(0, max(maxResults, categories.size))
            }
            return categories
        }
    }

    /**
     * Return the input size required by the model.
     */
    override fun getInputSize(): Size {
        return Size(inputWidth, inputHeight)
    }

    /**
     * Convert input bitmap to TensorImage and normalize.
     */
    override fun preprocessInputImage(bitmap: Bitmap): TensorImage {
        val size = min(bitmap.width, bitmap.height)
        val imageProcessor = ImageProcessor.Builder().apply {
            add(ResizeWithCropOrPadOp(size, size))
            add(ResizeOp(inputHeight, inputWidth, ResizeOp.ResizeMethod.BILINEAR))
            add(NormalizeOp(INPUT_MEAN, INPUT_STD))
        }.build()
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        return imageProcessor.process(tensorImage)
    }

    /**
     * Convert output logits of the model to a list of Category objects.
     */
    override fun postprocessOutputLogits(logitsByteBuffer: ByteBuffer): MutableList<Category> {
        // Convert ByteBuffer to FloatArray.
        val logits = FloatArray(outputCategoryCount)
        logitsByteBuffer.rewind()
        logitsByteBuffer.asFloatBuffer().get(logits)

        // Convert logits into probability list.
        val probabilities = CalculateUtils.softmax(logits)

        // Append label name to form a list of Category objects.
        val categories = mutableListOf<Category>()
        probabilities.forEachIndexed { index, probability ->
            categories.add(Category(labels[index], probability))
        }
        return categories
    }

    /**
     * Close the interpreter when it's no longer needed.
     */
    override fun close() {
        interpreter.close()
    }

    /**
     * Clear the internal state of the model.
     *
     * Call this function if the future inputs is unrelated to the past inputs. (e.g. when changing
     * to a new video sequence)
     */
    override fun reset() {
        // Ensure that no inference is running when the state is being cleared.
        synchronized(lock) {
            inputState = initializeInput()
        }
    }

    class VideoClassifierOptions constructor(
        val numThreads: Int,
        val maxResults: Int
    ) {
        companion object {
            fun builder() = Builder()
        }

        class Builder {
            private var numThreads: Int = -1
            private var maxResult: Int = -1

            fun setNumThreads(numThreads: Int): Builder {
                this.numThreads = numThreads
                return this
            }

            fun setMaxResult(maxResults: Int): Builder {
                if ((maxResults <= 0) && (maxResults != -1)) {
                    throw IllegalArgumentException("maxResults must be positive or -1.")
                }
                this.maxResult = maxResults
                return this
            }

            fun build(): VideoClassifierOptions {
                return VideoClassifierOptions(this.numThreads, this.maxResult)
            }
        }
    }
}
