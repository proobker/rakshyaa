package com.rakshyaa.rakshyaa.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts/decrypts data using an AES-GCM key stored in the Android Keystore.
 * The secret key never leaves the device's secure hardware / keystore.
 */
object CryptoManager {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "rakshyaa_master"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE_BYTES = 12
    private const val TAG_BITS = 128

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return keyGenerator.generateKey()
    }

    /** Encrypts bytes to a Base64 string: [iv][ciphertext]. */
    fun encrypt(data: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(data)
        val combined = ByteArray(iv.size + ciphertext.size)
        iv.copyInto(combined, 0)
        ciphertext.copyInto(combined, iv.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /** Decrypts a Base64 string produced by [encrypt]. */
    fun decrypt(encoded: String): ByteArray {
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        if (combined.size <= IV_SIZE_BYTES) {
            throw IllegalArgumentException("Invalid encrypted payload")
        }
        val iv = combined.copyOfRange(0, IV_SIZE_BYTES)
        val ciphertext = combined.copyOfRange(IV_SIZE_BYTES, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    fun encryptString(value: String): String =
        encrypt(value.toByteArray(Charsets.UTF_8))

    fun decryptString(encoded: String): String =
        String(decrypt(encoded), Charsets.UTF_8)
}
