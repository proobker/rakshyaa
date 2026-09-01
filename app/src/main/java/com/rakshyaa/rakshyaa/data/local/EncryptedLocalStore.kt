package com.rakshyaa.rakshyaa.data.local

import android.content.Context
import com.rakshyaa.rakshyaa.utils.CryptoManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores serialized datastores as encrypted JSON files on the device.
 * Each value is AES-GCM encrypted (key from Android Keystore) before being
 * written to disk. Backs up each encrypted blob to the backend via [SyncManager].
 */
@Singleton
class EncryptedLocalStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val storeDir: File by lazy {
        val dir = File(context.filesDir, "encrypted_data")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    /** Encrypts [plain] under [key] and writes the encrypted blob to disk. */
    fun savePlain(key: String, plain: String) {
        val encrypted = CryptoManager.encryptString(plain)
        File(storeDir, sanitize(key)).writeText(encrypted)
    }

    /** Reads + decrypts the blob stored under [key], or null if absent/corrupt. */
    fun loadPlain(key: String): String? {
        val file = File(storeDir, sanitize(key))
        if (!file.exists()) return null
        return runCatching { CryptoManager.decryptString(file.readText()) }.getOrNull()
    }

    /** Reads the raw encrypted text blob under [key] (for cloud sync), or null. */
    fun readRaw(key: String): String? {
        val file = File(storeDir, sanitize(key))
        if (!file.exists()) return null
        return runCatching { file.readText() }.getOrNull()
    }

    /** Writes an already-encrypted text blob under [key] (from cloud sync). */
    fun writeRaw(key: String, encryptedText: String) {
        File(storeDir, sanitize(key)).writeText(encryptedText)
    }

    /** Whether a stored blob exists for [key]. */
    fun exists(key: String): Boolean = File(storeDir, sanitize(key)).exists()

    /** Deletes a stored blob. */
    fun delete(key: String): Boolean = File(storeDir, sanitize(key)).delete()

    /** The on-disk location for a given datastore key (for diagnostics). */
    fun fileFor(key: String): File = File(storeDir, sanitize(key))

    private fun sanitize(key: String): String =
        key.replace(Regex("[^A-Za-z0-9._-]"), "_")
}
