package com.heartyculture.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.heartyculture.app.ui.theme.BaseTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.heartyculture.app.database.AppDatabase
import com.heartyculture.app.viewModels.DetectorViewModel
import kotlinx.coroutines.CoroutineScope


class MainActivity : ComponentActivity(), Detector.DetectorListener {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var detector: Detector
    private val processingScope = CoroutineScope(Dispatchers.IO)
    private lateinit var db: AppDatabase
    private val imageAnalysisExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private lateinit var viewFinder: PreviewView
    private lateinit var viewModel: DetectorViewModel

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
                ) {
                    MainContent()
                }
            }
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
            startCamera()
        }

        val modelPath = "best_float16.tflite"
        val labelPath = "labels.txt"
        detector = Detector(baseContext, modelPath, labelPath, this)
        detector.setup()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysisUseCase
                )

            } catch(exc: Exception) {
                Toast.makeText(this, "Use case binding failed", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private val imageAnalysisUseCase by lazy {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(imageAnalysisExecutor) { imageProxy ->
                    imageProxy.use {
                        imageProxy.toBitmap().also { bitmap ->
                                    viewModel.image.value = bitmap
                                    detector.detect(bitmap)
                            }
                    }
                }
            }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun drawBoundingBoxes(bitmap: Bitmap, boxes: List<BoundingBox>): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint().apply {
            color = Color.MAGENTA
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        val textPaint = Paint().apply {
            color = Color.rgb(0,255,0)
            textSize = 80f
            typeface = Typeface.DEFAULT_BOLD
        }

        for (box in boxes) {
            val rect = RectF(
                box.x1 * mutableBitmap.width,
                box.y1 * mutableBitmap.height,
                box.x2 * mutableBitmap.width,
                box.y2 * mutableBitmap.height
            )
            canvas.drawRect(rect, paint)
            canvas.drawText(box.clsName, rect.left, rect.bottom, textPaint)
        }

        return mutableBitmap
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>) {
        processingScope.launch(Dispatchers.Main) {
            val updatedBitmap = viewModel.image.value?.let { drawBoundingBoxes(it, boundingBoxes) }
            val labels = detector.labels

            for (box in boundingBoxes) {
                if (box.clsName != "" && box.clsName in labels) {
                    viewModel.disease.value = box.clsName
                    break
                }
            }


            if (viewModel.disease.value != "") {
                viewModel.image.value = updatedBitmap
                viewModel.cameraRunning.value = false
                viewModel.showImage.value = true
            }
        }

        Log.d("MainActivity", "Detected bounding boxes: $boundingBoxes")
    }

    override fun onEmptyDetect() {
        Log.d("MainActivity", "No detection")
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainContent() {
        val navController = rememberNavController()

        Scaffold(
            bottomBar = { BottomNavigationBar(navController = navController) },
            topBar = {
                TopAppBar(
                    title = { Text("Plant Disease Detection") },
                )
            },
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                NavigationGraph(navController = navController, viewModel = viewModel, viewFinder= viewFinder)
            }
        }
    }
}