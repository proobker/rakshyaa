package com.rakshyaa.rakshyaa.services

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypts and decrypts video files using AES-256/GCM, with the key held securely in
 * the Android Keystore so it never leaves the device. The encrypted bytes can then be
 * backed up to the backend as opaque blobs.
 */
@Singleton
class VideoEncryptionService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val VIDEO_KEY_ALIAS = "rakshyaa_video_encryption_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val IV_SIZE = 12
        private const val BUFFER_SIZE = 4096
    }

    private val key: SecretKey by lazy {
        keyStoreKey() ?: createKey()
    }

    private fun keyStoreKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        return keyStore.getKey(VIDEO_KEY_ALIAS, null) as? SecretKey
    }

    private fun createKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                VIDEO_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .setUnlockedDeviceRequired(false)
                .build()
        )
        return generator.generateKey()
    }

    /** Encrypts [inputFile] into a new encrypted file in cache, returning it. */
    fun encryptVideo(inputFile: File): File {
        val outputFile = File(context.cacheDir, "${inputFile.name}.enc")
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key) }
        FileOutputStream(outputFile).use { out ->
            out.write(cipher.iv)
            FileInputStream(inputFile).use { input -> streamCipher(input, out, cipher, outputFile) }
        }
        return outputFile
    }

    /** Decrypts [encryptedFile] into a new file in cache, returning it. */
    fun decryptVideo(encryptedFile: File): File {
        val outputFile = File(context.cacheDir, encryptedFile.name.removeSuffix(".enc"))
        FileInputStream(encryptedFile).use { input ->
            val iv = ByteArray(IV_SIZE)
            if (input.read(iv) != iv.size) throw IOException("Failed to read IV from encrypted file")
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            FileOutputStream(outputFile).use { out -> streamCipher(input, out, cipher, outputFile) }
        }
        return outputFile
    }

    private fun streamCipher(
        input: FileInputStream,
        out: FileOutputStream,
        cipher: Cipher,
        outputFile: File
    ) {
        try {
            val buffer = ByteArray(BUFFER_SIZE)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                cipher.update(buffer, 0, read)?.let { out.write(it) }
            }
            cipher.doFinal()?.let { out.write(it) }
        } catch (e: Exception) {
            outputFile.delete()
            throw IOException("Failed to cipher video", e)
        }
    }

    /** Encrypts a plaintext string to a Base64 blob (for small sensitive fields). */
    fun encryptSensitiveData(plain: String): String {
        if (plain.isEmpty()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key) }
        val ciphertext = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val out = ByteArray(cipher.iv.size + ciphertext.size)
        cipher.iv.copyInto(out, 0)
        ciphertext.copyInto(out, cipher.iv.size)
        return android.util.Base64.encodeToString(out, android.util.Base64.NO_WRAP)
    }

    /** Decrypts a Base64 blob produced by [encryptSensitiveData]. */
    fun decryptSensitiveData(encoded: String): String {
        if (encoded.isEmpty()) return ""
        val raw = android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP)
        val iv = raw.copyOfRange(0, IV_SIZE)
        val ciphertext = raw.copyOfRange(IV_SIZE, raw.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }
}
