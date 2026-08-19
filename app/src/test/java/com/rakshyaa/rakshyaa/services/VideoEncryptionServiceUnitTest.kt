package com.rakshyaa.rakshyaa.services

import android.content.Context
import androidx.security.crypto.MasterKeys
import com.rakshyaa.rakshyaa.data.local.SecurePreferences
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

import java.io.File
import java.io.IOException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VideoEncryptionServiceUnitTest {

    @get:Rule
    val mockitoRule = MockitoRule()

    @Mock
    lateinit var mockContext: Context

    @Mock
    lateinit var mockSecurePreferences: SecurePreferences

    @Mock
    lateinit var mockKeyStore: java.security.KeyStore

    @Mock
    lateinit var mockKeyGenerator: javax.crypto.KeyGenerator

    @Mock
    lateinit var mockSecretKey: SecretKey

    @Mock
    lateinit var mockCipher: Cipher

    private lateinit var videoEncryptionService: VideoEncryptionService

    @Before
    fun setUp() {
        // Initialize VideoEncryptionService with mocked dependencies
        videoEncryptionService = VideoEncryptionService(mockContext, mockSecurePreferences)

        // Mock the KeyStore
        `when`(java.security.KeyStore.getInstance(Mockito.anyString()))
            .thenReturn(mockKeyStore)

        // Mock the KeyGenerator
        `when`(javax.crypto.KeyGenerator.getInstance(
            Mockito.anyString(),
            Mockito.eq("AndroidKeyStore")
        )).thenReturn(mockKeyGenerator)

        // Mock the SecretKey
        `when`(mockKeyStore.getKey(Mockito.anyString(), Mockito.anyArray()))
            .thenReturn(mockSecretKey)

        // Mock the Cipher
        `when`(javax.crypto.Cipher.getInstance(Mockito.anyString()))
            .thenReturn(mockCipher)
    }

    @After
    fun tearDown() {
        Mockito.reset(
            mockContext,
            mockSecurePreferences,
            mockKeyStore,
            mockKeyGenerator,
            mockSecretKey,
            mockCipher
        )
    }

    @Test
    fun `initializeEncryptionKeyShouldCreateKeyWhenNotExists`() {
        // Arrange
        `when`(mockKeyStore.load(Mockito.any())).thenReturn(Unit)
        `when`(mockKeyStore.containsAlias("rakshyaa_video_encryption_key")).thenReturn(false)

        // Act
        videoEncryptionService.initializeEncryptionKey()

        // Assert
        Mockito.verify(mockKeyGenerator).init(Mockito.any())
        Mockito.verify(mockKeyGenerator).generateKey()
    }

    @Test
    fun `initializeEncryptionKeyShouldDoNothingWhenKeyExists`() {
        // Arrange
        `when`(mockKeyStore.load(Mockito.any())).thenReturn(Unit)
        `when`(mockKeyStore.containsAlias("rakshyaa_video_encryption_key")).thenReturn(true)

        // Act
        videoEncryptionService.initializeEncryptionKey()

        // Assert
        Mockito.verify(mockKeyStore, Mockito.times(0)).getKey(Mockito.anyString(), Mockito.anyArray())
        Mockito.verify(mockKeyGenerator, Mockito.times(0)).init(Mockito.any())
        Mockito.verify(mockKeyGenerator, Mockito.times(0)).generateKey()
    }

    @Test
    fun `getSecretKeyShouldReturnKeyFromKeyStore`() {
        // Arrange
        `when`(mockKeyStore.load(Mockito.any())).thenReturn(Unit)
        `when`(mockKeyStore.getKey(Mockito.eq("rakshyaa_video_encryption_key"), Mockito.anyArray()))
            .thenReturn(mockSecretKey)

        // Act
        val key = videoEncryptionService.getSecretKey()

        // Assert
        assertThat(key).isEqualTo(mockSecretKey)
    }

    @Test
    fun `getCipherShouldReturnInitializedCipher`() {
        // Arrange
        `when`(mockKeyStore.load(Mockito.any())).thenReturn(Unit)
        `when`(mockKeyStore.getKey(Mockito.eq("rakshyaa_video_encryption_key"), Mockito.anyArray()))
            .thenReturn(mockSecretKey)
        `when`(javax.crypto.Cipher.getInstance("AES/GCM/NoPadding"))
            .thenReturn(mockCipher)

        // Act
        val cipher = videoEncryptionService.getCipher(javax.crypto.Cipher.ENCRYPT_MODE)

        // Assert
        assertThat(cipher).isEqualTo(mockCipher)
        Mockito.verify(mockCipher).init(
            Mockito.eq(javaxecrypto.Cipher.ENCRYPT_MODE),
            Mockito.eq(mockSecretKey)
        )
    }

    // Note: Testing actual file encryption/decryption would require more complex mocking
    // of file I/O operations. For now, we're focusing on testing the service initialization
    // and key management logic.

    @Test
    fun `encryptVideoShouldThrowIOExceptionWhenEncryptionFails`() {
        // Arrange
        val inputFile = File(RuntimeEnvironment.application.cacheDir, "test.mp4")
        // Create a dummy file
        inputFile.parentFile?.mkdirs()
        inputFile.createNewFile()

        // Make the cipher throw an exception during encryption
        `when`(mockCipher.update(Mockito.anyByteArray(), Mockito.anyInt(), Mockito.anyInt()))
            .thenThrow(javax.crypto.ShortBufferException())

        // Act & Assert
        val exception = assertThrows(IOException::class.java) {
            videoEncryptionService.encryptVideo(inputFile)
        }
        assertThat(exception.message).contains("Failed to encrypt video")

        // Clean up
        inputFile.delete()
    }
}