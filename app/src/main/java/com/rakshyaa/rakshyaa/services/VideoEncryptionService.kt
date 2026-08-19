package com.rakshyaa.rakshyaa.services

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.MasterKeys
import androidx.security.crypto.EncryptedSharedPreferences
import com.rakshyaa.rakshyaa.data.local.SecurePreferences
import dagger.hilt.android.AndroidEntryPoint
import hiltService
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for encrypting and decrypting video files using AES-256
 * Keys are stored securely in Android Keystore
 */
@Singleton
class VideoEncryptionService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securePreferences: SecurePreferences
) {
    init {
        // Initialize encryption key on service creation
        initializeEncryptionKey()
    }

    companion object {
        private const val VIDEO_KEY_ALIAS = "rakshyaa_video_encryption_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    }

    /**
     * Initializes the encryption key in Android Keystore
     * Called once during app setup or when key needs to be regenerated
     */
    fun initializeEncryptionKey() {
        try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)

            // Check if key already exists
            if (!keyStore.containsAlias(VIDEO_KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER
                )

                // Configure key properties
                val keySpec = KeyGenParameterSpec.Builder(
                    VIDEO_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setUserAuthenticationRequired(false)
                    .setUnlockedDeviceRequired(false)
                    .build()

                keyGenerator.init(keySpec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to initialize encryption key", e)
        }
    }

    /**
     * Encrypts a video file and returns the path to the encrypted file
     *
     * @param inputFile Path to the original video file
     * @return Path to the encrypted video file
     * @throws IOException if encryption fails
     */
    fun encryptVideo(inputFile: File): File {
        return try {
            val outputFile = File(
                context.cacheDir,
                "${inputFile.name}.enc"
            )

            encryptFile(inputFile, outputFile)
            outputFile
        } catch (e: Exception) {
            throw IOException("Failed to encrypt video: ${e.message}", e)
        }
    }

    /**
     * Decrypts an encrypted video file and returns the path to the decrypted file
     *
     * @param encryptedFile Path to the encrypted video file
     * @return Path to the decrypted video file
     * @throws IOException if decryption fails
     */
    fun decryptVideo(encryptedFile: File): File {
        return try {
            val outputFile = File(
                context.cacheDir,
                encryptedFile.name.removeSuffix(".enc")
            )

            decryptFile(encryptedFile, outputFile)
            outputFile
        } catch (e: Exception) {
            throw IOException("Failed to decrypt video: ${e.message}", e)
        }
    }

    /**
     * Encrypts a file using AES-256/GCM
     */
    private fun encryptFile(inputFile: File, outputFile: File) {
        val cipher = getCipher(Cipher.ENCRYPT_MODE)
        val iv = cipher.iv

        FileInputStream(inputFile).use { inputStream ->
            FileOutputStream(outputFile).use { outputStream ->
                // Write IV first (needed for decryption)
                outputStream.write(iv)

                // Encrypt and write the file data
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    val output = cipher.update(buffer, 0, bytesRead)
                    if (output != null) {
                        outputStream.write(output)
                    }
                }

                // Write the final encrypted bytes
                val outputBytes = cipher.doFinal()
                if (outputBytes != null) {
                    outputStream.write(outputBytes)
                }
            }
        }
    }

    /**
     * Decrypts a file using AES-256/GCM
     */
    private fun decryptFile(encryptedFile: File, outputFile: File) {
        val cipher = getCipher(Cipher.DECRYPT_MODE)

        FileInputStream(encryptedFile).use { inputStream ->
            FileOutputStream(outputFile).use { outputStream ->
                // Read IV from the beginning of the file
                val iv = ByteArray(12) // GCM standard IV length
                if (inputStream.read(iv) != iv.size) {
                    throw IOException("Failed to read IV from encrypted file")
                }
                cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), iv)

                // Decrypt and write the file data
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    val output = cipher.update(buffer, 0, bytesRead)
                    if (output != null) {
                        outputStream.write(output)
                    }
                }

                // Write the final decrypted bytes
                val outputBytes = cipher.doFinal()
                if (outputBytes != null) {
                    outputStream.write(outputBytes)
                }
            }
        }
    }

    /**
     * Gets the Cipher instance initialized with the secret key
     */
    private fun getCipher(mode: Int): Cipher {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(mode, getSecretKey())
            cipher
        } catch (e: Exception) {
            throw RuntimeException("Failed to initialize cipher", e)
        }
    }

    /**
     * Gets the secret key from Android Keystore
     */
    private fun getSecretKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)
            keyStore.getKey(VIDEO_KEY_ALIAS, null) as SecretKey
        } catch (e: Exception) {
            throw RuntimeException("Failed to get secret key from keystore", e)
        }
    }

    /**
     * Generates a secure random salt for key derivation (if needed)
     */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        val random = java.security.SecureRandom()
        random.nextBytes(salt)
        return salt
    }
}