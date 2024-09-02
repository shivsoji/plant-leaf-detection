package com.heartyculture.app.screens

import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.heartyculture.app.database.AppDatabase
import com.heartyculture.app.viewModels.DetectorViewModel

@Composable
fun DiseaseScreen(viewFinder: PreviewView, viewModel: DetectorViewModel) {

    val cameraRunning = viewModel.cameraRunning
    val showImage = viewModel.showImage
    val image = viewModel.image
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val disease = viewModel.disease

    fun save() {
        image.value?.let { bitmap ->
            viewModel.setImage(bitmap, disease.value ?: "Unknown")
            viewModel.saveImageToDatabase(db)
            showImage.value = false
            cameraRunning.value = true
            }
        }

    fun skip() {
        showImage.value = false
        cameraRunning.value = true
    }

    Box {
        if (cameraRunning.value) {
            AndroidView(
                factory = { viewFinder },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (showImage.value) {
            viewModel.image.value?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Detected Image",
                    modifier = Modifier.fillMaxSize()
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { save() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text("Save", color = Color.White)
                    }
                    Button(
                        onClick = { skip() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text("Skip", color = Color.White)
                    }
                }
            }
        }
    }
}