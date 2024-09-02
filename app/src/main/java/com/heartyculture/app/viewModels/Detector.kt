package com.heartyculture.app.viewModels

import android.graphics.Bitmap
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heartyculture.app.database.AppDatabase
import com.heartyculture.app.database.ImageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class DetectorViewModel : ViewModel() {
    var cameraRunning = mutableStateOf(true)

    var showImage = mutableStateOf(false)

    var image: MutableState<Bitmap?> = mutableStateOf(null)

    var disease: MutableState<String?> = mutableStateOf(null)

    var plant: MutableState<String?> = mutableStateOf(null)

    fun setImage(bitmap: Bitmap, diseaseName: String) {
        image.value = bitmap
        disease.value = diseaseName
    }

    fun setPlant(plantName: String) {
        plant.value = plantName
    }

    fun saveImageToDatabase(db: AppDatabase) {
        viewModelScope.launch(Dispatchers.IO) {
            image.value?.let {
                val imageEntity = ImageEntity(imageData = bitmapToByteArray(it), disease = let { disease.value ?: "Unknown" }, plant = let { plant.value ?: "Unknown" })
                db.imageDao().insert(imageEntity)
                image.value = null
                disease.value = null
                plant.value = null
            }
        }
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}