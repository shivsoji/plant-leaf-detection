package com.heartyculture.app.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.File

object Utils {

    fun List<DoubleArray>.toBitmap(): Bitmap {
        val height = this.size
        val width = if (height > 0) this[0].size else 0
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in this.indices) {
            for (x in this[y].indices) {
                val color = if (this[y][x] > 0) Color.WHITE else Color.BLACK
                bitmap.setPixel(x, y, color)
            }
        }
        return bitmap
    }

    fun Context.getBitmapFromAsset(fileName: String): Bitmap? {
        return try {
            val assetManager = this.assets
            val inputStream = assetManager.open(fileName)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun List<Array<FloatArray>>.clone(): List<Array<FloatArray>> {
        return this.map { array -> array.map { it.clone() }.toTypedArray() }
    }

    fun fileToBitmap(filePath: String): Bitmap? {
        return BitmapFactory.decodeFile(filePath)
    }

    fun createImageFile(context: Context): File {
        val uupDir = File(context.filesDir, "heartyculture")
        if (!uupDir.exists()) {
            uupDir.mkdir()
        }
        return File.createTempFile("${System.currentTimeMillis()}", ".jpg", uupDir)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            val bitmap = ImageDecoder.decodeBitmap(source)
            if (bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            Log.e("BitmapError", "Failed to load bitmap from URI", e)
            null
        }
    }

    fun getCameraId(cameraManager: CameraManager): String {
        val cameraIds = cameraManager.cameraIdList
        for (id in cameraIds) {
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                return id
            }
        }
        return ""
    }
}