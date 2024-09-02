package com.heartyculture.app.api

import android.content.Context
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val client =
        HttpClient(CIO) {
            install(HttpCookies) { storage = AcceptAllCookiesStorage() }
            install(Logging) {
                level = LogLevel.ALL
                logger =
                        object : Logger {
                            override fun log(message: String) {
                                println(message)
                            }
                        }
            }
            install(ContentNegotiation) {
                json(
                        Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                        }
                )
            }
        }

@Serializable data class Params(val login: String, val password: String, val db: String)

@Serializable data class LoginRequest(val params: Params)

suspend fun loginAndGetSessionId(
        url: String,
        login: String,
        password: String,
        db: String,
        context: Context
): String? {
    client.post(url) {
        contentType(io.ktor.http.ContentType.Application.Json)
        setBody(LoginRequest(Params(login, password, db)))
    }
    val cookies = client.cookies(url)
    val sessionId = cookies.find { it.name == "session_id" }?.value
    saveSessionId(context, sessionId)
    return sessionId
}

fun saveSessionId(context: Context, sessionId: String?) {
    val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
    sharedPreferences.edit().putString("SESSION_ID", sessionId).apply()
}

fun getSessionId(context: Context): String? {
    val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
    return sharedPreferences.getString("SESSION_ID", null)
}

suspend fun makeAuthenticatedRequest(url: String, context: Context): HttpResponse {
    var response: HttpResponse?
    try {
        response =
                client.get(url) {
                    val sessionId = getSessionId(context)
                    if (sessionId != null) {
                        cookie("session_id", sessionId)
                    }
                }

        // If response status is 500, clear session_id and retry
        if (response.status == HttpStatusCode.InternalServerError) {
            // Clear the session_id
            saveSessionId(context, null)
            println("Session ID cleared due to 500 error. Retrying...")

            val endpoint = System.getenv("API_ENDPOINT") ?: error("API_ENDPOINT not set")
            val username = System.getenv("API_USERNAME") ?: error("API_USERNAME not set")
            val password = System.getenv("API_PASSWORD") ?: error("API_PASSWORD not set")
            val database = System.getenv("API_DATABASE") ?: error("API_DATABASE not set")

            loginAndGetSessionId(endpoint, username, password, database, context)

            // Retry the request
            response =
                    client.get(url) {
                        val sessionId = getSessionId(context)
                        if (sessionId != null) {
                            cookie("session_id", sessionId)
                        }
                    }
        }
    } catch (e: Exception) {
        println("Request failed: ${e.message}")
        throw e // rethrow the exception or handle it based on your needs
    }

    return response
}
