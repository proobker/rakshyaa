package com.rakshyaa.rakshyaa.data.sync

import com.rakshyaa.rakshyaa.data.local.EncryptedLocalStore
import com.rakshyaa.rakshyaa.data.network.ApiClient
import com.rakshyaa.rakshyaa.data.network.BackupListResponse
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Syncs encrypted on-device datastores with the backend backup API.
 * Only opaque encrypted blobs travel to/from the server (keys stay on-device).
 */
@Singleton
class SyncManager @Inject constructor(
    private val localStore: EncryptedLocalStore,
    private val apiClient: ApiClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Saves [plain] locally under [key] and pushes its encrypted blob to the backend. */
    suspend fun saveAndSync(key: String, plain: String) {
        localStore.savePlain(key, plain)
        push(key)
    }

    /** Pushes the already-encrypted local blob for [key] to the backend. */
    suspend fun push(key: String) {
        val raw = localStore.readRaw(key) ?: return
        if (!apiClientHasSession()) return
        runCatching {
            apiClient.putRaw("/backup/data/${enc(key)}", raw.toByteArray(Charsets.UTF_8))
        }
    }

    /** Pulls the encrypted blob for [key] from the backend and stores it locally. */
    suspend fun pull(key: String): String? {
        try {
            val bytes = apiClient.getRaw("/backup/data/${enc(key)}")
            if (bytes.isNotEmpty()) {
                localStore.writeRaw(key, String(bytes, Charsets.UTF_8))
            }
        } catch (_: Exception) {
            // Server unreachable or blob missing; fall back to local.
        }
        return localStore.loadPlain(key)
    }

    /** Returns the decrypted plaintext for [key], pulling from backend first if needed. */
    suspend fun getOrPull(key: String): String? {
        if (!localStore.exists(key)) {
            return pull(key)
        }
        return localStore.loadPlain(key)
    }

    /** Pushes an encrypted media blob to the backend under [id]. */
    suspend fun pushMedia(id: String, encryptedBytes: ByteArray) {
        if (!apiClientHasSession()) return
        runCatching {
            apiClient.putRaw("/backup/media/${enc(id)}", encryptedBytes)
        }
    }

    /** Pulls an encrypted media blob from the backend under [id]. */
    suspend fun pullMedia(id: String): ByteArray? =
        runCatching { apiClient.getRaw("/backup/media/${enc(id)}") }.getOrNull()

    /** Lists which encrypted blobs exist on the backend. */
    suspend fun remoteKeys(): Set<String> =
        runCatching {
            val body = apiClient.get("/backup")
            val res = json.decodeFromString(BackupListResponse.serializer(), body)
            res.blobs.filter { it.kind == "data" }.map { it.key }.toSet()
        }.getOrDefault(emptySet())

    private fun apiClientHasSession(): Boolean = true

    private fun enc(s: String): String =
        s.replace(Regex("[^A-Za-z0-9._-]"), "_")
}
