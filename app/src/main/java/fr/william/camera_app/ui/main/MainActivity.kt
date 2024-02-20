package fr.william.camera_app.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.util.Range
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import fr.william.camera_app.data.VideoClassifier
import fr.william.camera_app.databinding.ActivityMainBinding
import fr.william.camera_app.ui.camera.CameraViewModel
import fr.william.camera_app.ui.theme.CameraAppTheme
import fr.william.camera_app.ui.utils.CalculateUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.label.Category
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<MainViewModel>()

    // video classifier

    private val lock = Any()
    private lateinit var binding: ActivityMainBinding
    private lateinit var executor: ExecutorService
    private lateinit var sheetBehavior: BottomSheetBehavior<LinearLayout>
    private var videoClassifier: VideoClassifier? = null
    private var numThread = 1
    private var modelPosition = 0
    private var lastInferenceStartTime: Long = 0
    private var changeModelListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {
            // do nothing
        }
        override fun onItemSelected(
            parent: AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long
        ) {
            modelPosition = position
            createClassifier()
        }
    }
    private lateinit var cameraViewModel: CameraViewModel

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // video classifier
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sheetBehavior = BottomSheetBehavior.from(binding.bottomSheet.bottomSheetLayout)
        binding.bottomSheet.gestureLayout.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.bottomSheet.gestureLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val height = binding.bottomSheet.gestureLayout.measuredHeight
                sheetBehavior.peekHeight = height
            }
        })
        sheetBehavior.isHideable = false
        sheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        binding.bottomSheet.bottomSheetArrow.setImageResource(fr.william.camera_app.R.drawable.icn_chevron_down)
                    }
                    BottomSheetBehavior.STATE_COLLAPSED, BottomSheetBehavior.STATE_SETTLING -> {
                        binding.bottomSheet.bottomSheetArrow.setImageResource(fr.william.camera_app.R.drawable.icn_chevron_up)
                    }
                    else -> {
                        // do nothing.
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // no func
            }

        })
        binding.bottomSheet.threads.text = numThread.toString()
        binding.bottomSheet.minus.setOnClickListener {
            if (numThread <= 1) return@setOnClickListener
            numThread--
            binding.bottomSheet.threads.text = numThread.toString()
            createClassifier()
        }
        binding.bottomSheet.plus.setOnClickListener {
            if (numThread >= 4) return@setOnClickListener
            numThread++
            binding.bottomSheet.threads.text = numThread.toString()
            createClassifier()
        }
        binding.bottomSheet.btnClearModelState.setOnClickListener {
            videoClassifier?.reset()
        }
        initSpinner()

        // Initialisez le ViewModel avec le contexte de l'activité
        /*cameraViewModel = ViewModelProvider(this)[CameraViewModel::class.java]
        cameraViewModel.initialize(this)*/



        var state by mutableStateOf<MainUiState>(MainUiState.Loading)

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collectLatest {
                    state = it
                }
            }
        }

        if(!hasCameraPermission()) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), 1
            )
        }
        setContent {
            val widthSizeClass = calculateWindowSizeClass(this).widthSizeClass
            CameraAppTheme {
                MainScreen(
                    widthSizeClass = widthSizeClass,
                )
            }
        }
    }
    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED


    /**
     * Initialize the spinner to let users change the TFLite model.
     */
    private fun initSpinner() {
        ArrayAdapter.createFromResource(
            this,
            fr.william.camera_app.R.array.tfe_pe_models_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            binding.bottomSheet.spnSelectModel.adapter = adapter
            binding.bottomSheet.spnSelectModel.setSelection(modelPosition)
        }
        binding.bottomSheet.spnSelectModel.onItemSelectedListener = changeModelListener
    }

    /**
     * Start the image capturing pipeline.
     */
    @androidx.annotation.OptIn(ExperimentalCamera2Interop::class) private fun startCamera() {
        executor = Executors.newSingleThreadExecutor()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Create a Preview to show the image captured by the camera on screen.
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.preview.surfaceProvider)
                }

            try {
                // Unbind use cases before rebinding.
                cameraProvider.unbindAll()

                // Create an ImageAnalysis to continuously capture still images using the camera,
                // and feed them to the TFLite model. We set the capturing frame rate to a multiply
                // of the TFLite model's desired FPS to keep the preview smooth, then drop
                // unnecessary frames during image analysis.
                val targetFpsMultiplier = VideoClassifier.MAX_CAPTURE_FPS.div(VideoClassifier.MODEL_FPS)
                val targetCaptureFps = VideoClassifier.MODEL_FPS * targetFpsMultiplier
                val builder = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                val extender: Camera2Interop.Extender<*> = Camera2Interop.Extender(builder)
                extender.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    Range(targetCaptureFps, targetCaptureFps)
                )
                val imageAnalysis = builder.build()

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    processImage(imageProxy)
                }

                // Combine the ImageAnalysis and Preview into a use case group.
                val useCaseGroup = UseCaseGroup.Builder()
                    .addUseCase(preview)
                    .addUseCase(imageAnalysis)
                    .setViewPort(binding.preview.viewPort!!)
                    .build()

                // Bind use cases to camera.
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, useCaseGroup
                )

            } catch (e: Exception) {
                Log.e(VideoClassifier.TAG, "Use case binding failed.", e)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Run a frames received from the camera through the TFLite video classification pipeline.
     */
    @androidx.annotation.OptIn(ExperimentalGetImage::class) private fun processImage(imageProxy: ImageProxy) {
        // Ensure that only one frame is processed at any given moment.
        synchronized(lock) {
            val currentTime = SystemClock.uptimeMillis()
            val diff = currentTime - lastInferenceStartTime

            // Check to ensure that we only run inference at a frequency required by the
            // model, within an acceptable error range (e.g. 10%). Discard the frames
            // that comes too early.
            if (diff * VideoClassifier.MODEL_FPS >= 1000 /* milliseconds */ * (1 - VideoClassifier.MODEL_FPS_ERROR_RANGE)) {
                lastInferenceStartTime = currentTime

                val image = imageProxy.image
                image?.let {
                    videoClassifier?.let { classifier ->
                        // Convert the captured frame to Bitmap.
                        val imageBitmap = Bitmap.createBitmap(
                            it.width,
                            it.height,
                            Bitmap.Config.ARGB_8888
                        )
                        CalculateUtils.yuvToRgb(image, imageBitmap)

                        // Rotate the image to the correct orientation.
                        val rotateMatrix = Matrix()
                        rotateMatrix.postRotate(
                            imageProxy.imageInfo.rotationDegrees.toFloat()
                        )
                        val rotatedBitmap = Bitmap.createBitmap(
                            imageBitmap, 0, 0, it.width, it.height,
                            rotateMatrix, false
                        )

                        // Run inference using the TFLite model.
                        val startTimeForReference = SystemClock.uptimeMillis()
                        val results = classifier.classify(rotatedBitmap)
                        val endTimeForReference =
                            SystemClock.uptimeMillis() - startTimeForReference
                        val inputFps = 1000f / diff
                        showResults(results, endTimeForReference, inputFps)

                        if (inputFps < VideoClassifier.MODEL_FPS * (1 - VideoClassifier.MODEL_FPS_ERROR_RANGE)) {
                            Log.w(
                                VideoClassifier.TAG, "Current input FPS ($inputFps) is " +
                                        "significantly lower than the TFLite model's " +
                                        "expected FPS ($VideoClassifier.MODEL_FPS). It's likely because " +
                                        "model inference takes too long on this device."
                            )
                        }
                    }
                }
            }
            imageProxy.close()
        }
    }

    /**
     * Check whether camera permission is already granted.
     */
    private fun allPermissionsGranted() = VideoClassifier.REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == VideoClassifier.REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    /**
     * Initialize the TFLite video classifier.
     */
    private fun createClassifier() {
        synchronized(lock) {
            if (videoClassifier != null) {
                videoClassifier?.close()
                videoClassifier = null
            }
            val options =
                VideoClassifier.VideoClassifierOptions.builder()
                    .setMaxResult(VideoClassifier.MAX_RESULT)
                    .setNumThreads(numThread)
                    .build()
            val modelFile = when (modelPosition) {
                0 -> VideoClassifier.MODEL_MOVINET_A0_FILE
                1 -> VideoClassifier.MODEL_MOVINET_A1_FILE
                else -> VideoClassifier.MODEL_MOVINET_A2_FILE
            }

            videoClassifier = VideoClassifier.createFromFileAndLabelsAndOptions(
                this,
                modelFile,
                VideoClassifier.MODEL_LABEL_FILE,
                options
            )

            // show input size of video classification
            videoClassifier?.getInputSize()?.let {
                binding.bottomSheet.inputSizeInfo.text =
                    getString(fr.william.camera_app.R.string.frame_size, it.width, it.height)
            }
            Log.d(VideoClassifier.TAG, "Classifier created.")
        }
    }

    /**
     * Show the video classification results on the screen.
     */
    private fun showResults(labels: List<Category>, inferenceTime: Long, inputFps: Float) {
        runOnUiThread {
            binding.bottomSheet.tvDetectedItem0.text = labels[0].label
            binding.bottomSheet.tvDetectedItem1.text = labels[1].label
            binding.bottomSheet.tvDetectedItem2.text = labels[2].label
            binding.bottomSheet.tvDetectedItem0Value.text = labels[0].score.toString()
            binding.bottomSheet.tvDetectedItem1Value.text = labels[1].score.toString()
            binding.bottomSheet.tvDetectedItem2Value.text = labels[2].score.toString()
            binding.bottomSheet.inferenceInfo.text =
                getString(fr.william.camera_app.R.string.inference_time, inferenceTime)
            binding.bottomSheet.inputFpsInfo.text = String.format("%.1f", inputFps)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        videoClassifier?.close()
        executor.shutdown()
    }
}