package com.heartyculture.app

import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
fun detect(file: Bitmap) {
    val apiKey = ""
    val modelEndpoint = "dataset/v"

    val byteArrayOutputStream = ByteArrayOutputStream()
    file.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
    val byteArray = byteArrayOutputStream.toByteArray()
    val encodedFile = String(Base64.getEncoder().encode(byteArray), StandardCharsets.US_ASCII)

    val uploadURL =
        "https://detect.roboflow.com/$modelEndpoint?api_key=$apiKey&name=${UUID.randomUUID()}.jpg";


    var connection: HttpURLConnection? = null
    try {
        val url = URL(uploadURL)
        connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type",
            "application/x-www-form-urlencoded")
        connection.setRequestProperty("Content-Length",
            byteArray.size.toString()
        )
        connection.setRequestProperty("Content-Language", "en-US")
        connection.useCaches = false
        connection.doOutput = true

        val wr = DataOutputStream(
            connection.outputStream)
        wr.writeBytes(encodedFile)
        wr.close()

        val stream = connection.inputStream
        val reader = BufferedReader(InputStreamReader(stream))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            println(line)
        }
        reader.close()
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        connection?.disconnect()
    }
}