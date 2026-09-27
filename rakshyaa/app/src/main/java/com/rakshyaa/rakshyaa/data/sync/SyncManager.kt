package com.rakshyaa.rakshyaa.data.sync

import com.rakshyaa.rakshyaa.data.local.EncryptedLocalStore
import com.rakshyaa.rakshyaa.data.network.ApiClient
import com.rakshyaa.rakshyaa.data.network.BackupListResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val json = Json { ignoreUnknownKeys = true }

    /** Saves [plain] locally under [key] and pushes its encrypted blob to the backend. */
    suspend fun saveAndSync(key: String, plain: String) {
        localStore.savePlain(key, plain)
        push(key)
    }

    /** Pushes the already-encrypted local blob for [key] to the backend. */
    suspend fun push(key: String) {
        val raw = localStore.readRaw(key) ?: return
        if (!backupEnabled() || localStore.accountId == null) return
        try {
            apiClient.putRaw("/backup/data/${enc(key)}", raw.toByteArray(Charsets.UTF_8))
            _error.value = null
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { _error.value = "Backup upload failed. Your data is still saved on this device." }
    }

    /** Pulls the encrypted blob for [key] from the backend and stores it locally. */
    suspend fun pull(key: String): String? {
        val account = localStore.accountId ?: return null
        if (account == "local-device") return localStore.loadPlain(key)
        try {
            val bytes = apiClient.getRaw("/backup/data/${enc(key)}")
            if (account != localStore.accountId) return null
            if (bytes.isNotEmpty() && !localStore.restoreRaw(key, String(bytes, Charsets.UTF_8))) {
                _error.value = "This backup needs the encryption keys from the original installation. Local data was kept."
            }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) {
            // Server unreachable or blob missing; fall back to local.
        }
        return if (account == localStore.accountId) localStore.loadPlain(key) else null
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
        check(localStore.accountId != null) { "Sign in before uploading media." }
        check(backupEnabled()) { "Enable cloud backup before uploading media." }
        apiClient.putRaw("/backup/media/${enc(id)}", encryptedBytes)
    }

    /** Pulls an encrypted media blob from the backend under [id]. */
    suspend fun pullMedia(id: String): ByteArray? =
        runCatching { apiClient.getRaw("/backup/media/${enc(id)}") }.getOrNull()

    /** Lists which encrypted blobs exist on the backend. */
    suspend fun remoteKeys(): Set<String> =
        if (localStore.accountId == "local-device") emptySet() else runCatching {
            val body = apiClient.get("/backup")
            val res = json.decodeFromString(BackupListResponse.serializer(), body)
            res.blobs.filter { it.kind == "data" }.map { it.key }.toSet()
        }.getOrDefault(emptySet())

    suspend fun syncAll() {
        _error.value = null
        check(backupEnabled()) { "Enable cloud backup first." }
        // Verify connectivity rather than claiming success from a swallowed list error.
        apiClient.get("/backup")
        var failed = false
        for (key in localStore.keys()) {
            push(key)
            if (_error.value != null) failed = true
        }
        if (failed) {
            val message = "Some backups failed. Your data remains on this device; retry when connected."
            _error.value = message
            error(message)
        }
    }

    private fun backupEnabled(): Boolean = localStore.accountId != "local-device" && runCatching {
        localStore.loadPlain("profile_settings")?.let {
            json.parseToJsonElement(it).jsonObject["backupEnabled"]?.jsonPrimitive?.booleanOrNull
        } ?: true
    }.getOrDefault(false)

    private fun enc(s: String): String =
        s.replace(Regex("[^A-Za-z0-9._-]"), "_")
}
