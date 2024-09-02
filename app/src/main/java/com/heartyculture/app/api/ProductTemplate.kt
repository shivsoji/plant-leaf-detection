package com.heartyculture.app.api

import android.content.Context
import com.heartyculture.app.data.ProductTemplate
import com.heartyculture.app.data.ProductTemplateResponse
import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

suspend fun fetchProductTemplates(context: Context, plantName: String): List<ProductTemplate>? {
    val client = HttpClient()

    val baseUrl = "https://erp.heartyculturenursery.com/api/product.template"
    val url = URLBuilder(baseUrl).apply {
        parameters.append("page_size", "10")
        parameters.append("page", "1")
        parameters.append("filter", """[["name", "=", "$plantName"]]""")
    }.buildString()


    if (getSessionId(context) == null) {
        loginAndGetSessionId(
            "https://erp.heartyculturenursery.com/web/session/authenticate",
            "shivchoudhary4109@gmail.com",
            "heartyculture",
            "hc-nursery",
            context
        )
    }

    return try {
        val response = makeAuthenticatedRequest(url, context)
        val jsonResponse = Json{
            ignoreUnknownKeys = true
        }.decodeFromString<ProductTemplateResponse>(response.bodyAsText())
        jsonResponse.result
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } finally {
        client.close()
    }
}