package com.rakshyaa.rakshyaa.data.network

import com.rakshyaa.rakshyaa.BuildConfig
import com.rakshyaa.rakshyaa.data.local.SecurePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin OkHttp client for talking to the Rakshyaa backend. Attaches the stored
 * session JWT as a Bearer token to authenticated requests.
 */
@Singleton
class ApiClient @Inject constructor(
    private val securePreferences: SecurePreferences
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()
    private val baseUrl: String = BuildConfig.BACKEND_BASE_URL.trimEnd('/')

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /** Executes a request on the IO dispatcher. Returns body text if 2xx, else throws. */
    private suspend fun execute(request: Request): String = withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = kotlin.runCatching { json.decodeFromString<ErrorResponse>(body).error }
                    .getOrNull() ?: "HTTP ${response.code}"
                throw Exception(message)
            }
            body
        }
    }

    private fun authedRequest(path: String, method: String, body: String? = null): Request {
        val builder = Request.Builder().url("$baseUrl$path")
        securePreferences.getAccessToken()?.let { builder.header("Authorization", "Bearer $it") }
        builder.method(method, body?.toRequestBody(jsonMedia))
        return builder.build()
    }

    suspend fun postJsonPublic(path: String, body: String): String {
        val request = Request.Builder()
            .url("$baseUrl$path")
            .post(body.toRequestBody(jsonMedia))
            .build()
        return execute(request)
    }

    suspend fun postJson(path: String, body: String): String =
        execute(authedRequest(path, "POST", body))

    suspend fun get(path: String): String =
        execute(authedRequest(path, "GET"))

    suspend fun putRaw(path: String, bytes: ByteArray): String {
        val request = authedRequest(path, "PUT").newBuilder()
            .put(bytes.toRequestBody("application/octet-stream".toMediaType()))
            .build()
        return execute(request)
    }

    suspend fun getRaw(path: String): ByteArray = withContext(Dispatchers.IO) {
        client.newCall(authedRequest(path, "GET")).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}")
            }
            response.body?.bytes() ?: ByteArray(0)
        }
    }

    suspend fun delete(path: String): String =
        execute(authedRequest(path, "DELETE"))
}
