package com.heartyculture.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.heartyculture.app.database.AppDatabase
import com.heartyculture.app.ml.DrawImages
import com.heartyculture.app.ml.InstanceSegmentation
import com.heartyculture.app.ml.Success
import com.heartyculture.app.ui.theme.BaseTheme
import com.heartyculture.app.viewModels.DetectorViewModel
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class LeafSegmenter : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private val processingScope = CoroutineScope(Dispatchers.IO)
    private lateinit var db: AppDatabase
    private val imageAnalysisExecutor: ExecutorService by lazy {
        Executors.newSingleThreadExecutor()
    }
    private lateinit var viewFinder: PreviewView
    private lateinit var viewModel: DetectorViewModel
    private var instanceSegmentation: InstanceSegmentation? = null
    private lateinit var backgroundExecutor: ExecutorService
    private lateinit var drawImages: DrawImages
    private var imageCapture: ImageCapture? = null // Change this line

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_feed)
        db = AppDatabase.getDatabase(this)

        viewFinder = PreviewView(this)

        setContent {
            viewModel = viewModel()
            BaseTheme {
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                ) { MainContent() }
            }
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        drawImages = DrawImages(applicationContext)

        backgroundExecutor = Executors.newSingleThreadExecutor()
        backgroundExecutor.execute {
            instanceSegmentation =
                    InstanceSegmentation(
                            context = applicationContext,
                            modelPath = Constants.MODEL_PATH,
                            labelPath = Constants.LABELS_PATH,
                            smoothEdges = Constants.SMOOTH_EDGES
                    ) { toast(it) }
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(
                {
                    val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                    val preview =
                            Preview.Builder().build().also {
                                it.setSurfaceProvider(viewFinder.surfaceProvider)
                            }

                    imageCapture = ImageCapture.Builder().build()

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                    } catch (exc: Exception) {
                        Toast.makeText(this, "Use case binding failed", Toast.LENGTH_SHORT).show()
                    }
                },
                ContextCompat.getMainExecutor(this)
        )
    }

    fun captureImage() { // Change from private to public
        val imageCapture = imageCapture ?: return

        val outputOptions = ImageCapture.OutputFileOptions.Builder(getOutputFile()).build()

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = output.savedUri ?: Uri.fromFile(getOutputFile())
                        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, savedUri)
                        runInstanceSegmentation(bitmap)
                    }

                    override fun onError(exc: ImageCaptureException) {
                        Log.e("LeafSegmenter", "Photo capture failed: ${exc.message}", exc)
                    }
                }
        )
    }

    private fun getOutputFile(): File {
        val mediaDir =
                externalMediaDirs.firstOrNull()?.let {
                    File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
                }
        return if (mediaDir != null && mediaDir.exists()) File(mediaDir, "captured_image.jpg")
        else File(filesDir, "captured_image.jpg")
    }

    private val imageAnalysisUseCase by lazy {
        ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(imageAnalysisExecutor) { imageProxy ->
                        imageProxy.use {
                            if (viewModel.cameraRunning.value) {
                                imageProxy.toBitmap().also { bitmap ->
                                    viewModel.image.value = bitmap
                                    runInstanceSegmentation(bitmap)
                                }
                            }
                        }
                    }
                }
    }

    private fun allPermissionsGranted() =
            REQUIRED_PERMISSIONS.all {
                ContextCompat.checkSelfPermission(baseContext, it) ==
                        PackageManager.PERMISSION_GRANTED
            }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        instanceSegmentation?.close()
    }

    private fun onDetect(original: Bitmap, success: Success) {
        Log.d("LeafSegmenter", "onDetect: original=${original}, success=${success}")

        processingScope.launch(Dispatchers.Main) {
            val images =
                    drawImages.invoke(
                            original = original,
                            success = success,
                            isSeparateOut = false,
                            isMaskOut = false
                    )

            Log.d("LeafSegmenter", "images: $images")

            val updatedBitmap = images[0]
            viewModel.plant.value = success.results.first().box.clsName

            if (viewModel.plant.value != "") {
                viewModel.image.value = updatedBitmap.first
                viewModel.cameraRunning.value = false
                viewModel.showImage.value = true
            }
        }
    }

    private fun onEmptyDetect(error: String) {
        Log.d("LeafSegmenter", error)
    }

    private fun runInstanceSegmentation(bitmap: Bitmap) {
        backgroundExecutor.submit {
            instanceSegmentation?.invoke(
                    frame = bitmap,
                    onSuccess = { onDetect(bitmap, it) },
                    onFailure = { onEmptyDetect(it) }
            )
        }
    }

    private fun toast(message: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainContent() {
        val navController = rememberNavController()
        val context = LocalContext.current
        val leafSegmenter = remember { context as LeafSegmenter }

        Scaffold(
                bottomBar = { BottomNavigationBar(navController = navController) },
                topBar = {
                    TopAppBar(
                            title = { Text("Plant Detection") },
                    )
                },
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                NavigationGraph(
                        navController = navController,
                        viewModel = viewModel,
                        viewFinder = viewFinder
                )
            }
        }
    }
}
