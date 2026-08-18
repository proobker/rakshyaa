package com.rakshyaa.rakshyaa.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.rakshyaa.rakshyaa.R
import com.rakshyaa.rakshyaa.data.auth.AuthRepository
import com.rakshyaa.rakshyaa.data.EmergencyContactsRepository
import com.rakshyaa.rakshyaa.data.local.SecurePreferences
import com.rakshyaa.rakshyaa.services.VideoEncryptionService
import dagger.hilt.android.AndroidEntryPoint
import hiltService
import java.util.ArrayList
import java.util.List
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinlinenumberassigned

/**
 * Service for managing emergency contacts with encryption for sensitive data
 */
@AndroidEntryPoint
@hiltService
class EmergencyContactsService @Inject constructor(
    private val authRepository: AuthRepository,
    private val securePreferences: SecurePreferences,
    private val emergencyContactsRepository: EmergencyContactsRepository,
    private val videoEncryptionService: VideoEncryptionService
) : Service() {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "emergency_contacts_channel"
        private const val NOTIFICATION_ID = 7
        const val ACTION_START_EMERGENCY_CONTACTS_SERVICE = "ACTION_START_EMERGENCY_CONTACTS_SERVICE"
        const val ACTION_STOP_EMERGENCY_CONTACTS_SERVICE = "ACTION_STOP_EMERGENCY_CONTACTS_SERVICE"
        const val ACTION_ADD_CONTACT = "ACTION_ADD_CONTACT"
        const val ACTION_UPDATE_CONTACT = "ACTION_UPDATE_CONTACT"
        const val ACTION_REMOVE_CONTACT = "ACTION_REMOVE_CONTACT"
        const val ACTION_ESCALATE_MISSED_CHECK_IN = "ACTION_ESCALATE_MISSED_CHECK_IN"
        const val ACTION_SEND_SOS_ALERT = "ACTION_SEND_SOS_ALERT"
    }

    private var coroutineScope: CoroutineScope? = null
    private var contactsJob: Job? = null
    private var isServiceActive = false

    // Encryption key alias for emergency contacts
    private companion object {
        private const val CONTACT_KEY_ALIAS = "rakshyaa_emergency_contact_key"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }

    override fun onCreate() {
        super.onCreate()
        initializeEncryptionKey()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_EMERGENCY_CONTACTS_SERVICE -> startEmergencyContactsService()
            ACTION_STOP_EMERGENCY_CONTACTS_SERVICE -> stopEmergencyContactsService()
            ACTION_ADD_CONTACT -> {
                val name = intent.getStringExtra("name") ?: ""
                val phoneNumber = intent.getStringExtra("phone_number") ?: ""
                val relationship = intent.getStringExtra("relationship") ?: ""
                val publicKey = intent.getStringExtra("public_key") ?: ""
                val isPrimary = intent.getBooleanExtra("is_primary", false)
                addEmergencyContact(name, phoneNumber, relationship, publicKey, isPrimary)
            }
            ACTION_UPDATE_CONTACT -> {
                val contactId = intent.getStringExtra("contact_id") ?: ""
                val name = intent.getStringExtra("name") ?: ""
                val phoneNumber = intent.getStringExtra("phone_number") ?: ""
                val relationship = intent.getStringExtra("relationship") ?: ""
                val publicKey = intent.getStringExtra("public_key") ?: ""
                val isPrimary = intent.getBooleanExtra("is_primary", false)
                updateEmergencyContact(contactId, name, phoneNumber, relationship, publicKey, isPrimary)
            }
            ACTION_REMOVE_CONTACT -> {
                val contactId = intent.getStringExtra("contact_id") ?: ""
                removeEmergencyContact(contactId)
            }
            ACTION_ESCALATE_MISSED_CHECK_IN -> {
                val userId = intent.getStringExtra("user_id") ?: ""
                val checkInId = intent.getStringExtra("check_in_id") ?: ""
                val timestamp = intent.getLongExtra("timestamp", 0)
                escalateMissedCheckIn(userId, checkInId, timestamp)
            }
            ACTION_SEND_SOS_ALERT -> {
                val userId = intent.getStringExtra("user_id") ?: ""
                val SOSType = intent.getStringExtra("sos_type") ?: "general"
                val latitude = intent.getDoubleExtra("latitude", 0.0)
                val longitude = intent.getDoubleExtra("longitude", 0.0)
                sendSOSAlertToContacts(userId, SOSType, latitude, longitude)
            }
        }
        return START_STICKY
    }

    private fun startEmergencyContactsService() {
        if (isServiceActive) return
        isServiceActive = true

        // Start foreground service
        startForeground(NOTIFICATION_ID, buildNotification("Emergency Contacts Service Active"))

        // Set up coroutine scope for emergency contacts service logic
        coroutineScope = CoroutineScope(Dispatchers.Main)
        contactsJob = coroutineScope?.launch {
            #TODO: Load emergency contacts and cache them if needed
            #For now, we'll rely on the repository for direct access
        }
    }

    private fun stopEmergencyContactsService() {
        if (!isServiceActive) return
        isServiceActive = false

        // Stop foreground service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        // Clean up coroutine scope
        contactsJob?.cancel()
        coroutineScope = null
        contactsJob = null
    }

    private fun addEmergencyContact(
        name: String,
        phoneNumber: String,
        relationship: String,
        publicKey: String,
        isPrimary: Boolean
    ) {
        val userId = securePreferences.getUserId()
        if (userId.isNullOrBlank()) {
            return
        }

        coroutineScope?.launch {
            try {
                // Encrypt sensitive fields
                val encryptedPhoneNumber = encryptSensitiveData(phoneNumber)
                val encryptedPublicKey = encryptSensitiveData(publicKey)

                emergencyContactsRepository.addEmergencyContact(
                    userId = userId,
                    name = name,
                    encryptedPhoneNumber = encryptedPhoneNumber,
                    relationship = relationship,
                    encryptedPublicKey = encryptedPublicKey,
                    isPrimary = isPrimary
                )

                android.util.Log.i("EmergencyContactsService", "Added emergency contact: $name")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateEmergencyContact(
        contactId: String,
        name: String,
        phoneNumber: String,
        relationship: String,
        publicKey: String,
        isPrimary: Boolean
    ) {
        val userId = securePreferences.getUserId()
        if (userId.isNullOrBlank()) {
            return
        }

        coroutineScope?.launch {
            try {
                // Encrypt sensitive fields
                val encryptedPhoneNumber = encryptSensitiveData(phoneNumber)
                val encryptedPublicKey = encryptSensitiveData(publicKey)

                emergencyContactsRepository.updateEmergencyContact(
                    contactId = contactId,
                    name = name,
                    encryptedPhoneNumber = encryptedPhoneNumber,
                    relationship = relationship,
                    encryptedPublicKey = encryptedPublicKey,
                    isPrimary = isPrimary
                )

                android.util.Log.i("EmergencyContactsService", "Updated emergency contact: $contactId")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun removeEmergencyContact(contactId: String) {
        val userId = securePreferences.getUserId()
        if (userId.isNullOrBlank()) {
            return
        }

        coroutineScope?.launch {
            try {
                emergencyContactsRepository.removeEmergencyContact(
                    contactId = contactId,
                    userId = userId
                )

                android.util.Log.i("EmergencyContactsService", "Removed emergency contact: $contactId")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun escalateMissedCheckIn(
        userId: String,
        checkInId: String,
        timestamp: Long
    ) {
        if (userId.isNullOrBlank()) {
            return
        }

        coroutineScope?.launch {
            try {
                emergencyContactsRepository.escalateMissedCheckIn(
                    userId = userId,
                    checkInId = checkInId,
                    timestamp = timestamp
                )

                android.util.Log.i("EmergencyContactsService", "Escalated missed check-in $checkInId for user $userId")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sendSOSAlertToContacts(
        userId: String,
        SOSType: String,
        latitude: Double,
        longitude: Double
    ) {
        if (userId.isNullOrBlank()) {
            return
        }

        coroutineScope?.launch {
            try {
                // Get the user's emergency contacts
                val contacts = emergencyContactsRepository.getEmergencyContacts(userId)

                #TODO: In a real implementation, we would send actual alerts (SMS, calls, etc.)
                #For now, we'll just log the action
                android.util.Log.i("EmergencyContactsService", "Sending SOS alert ($SOSType) to ${contacts.size} contacts for user $userId at ($latitude, $longitude)")

                #TODO: Actually send notifications to contacts via preferred method (SMS, push notification, etc.)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Encrypts sensitive data using AES-256/GCM
     */
    private fun encryptSensitiveData(plainText: String): String {
        if (plainText.isEmpty()) {
            return plainText
        }

        return try {
            val cipher = javax.crypto.Cipher.getInstance(TRANSFORMATION)
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, getSecretKey())

            // Generate IV
            val iv = ByteArray(12) // GCM standard IV length
            val random = java.security.SecureRandom()
            random.nextBytes(iv)
            cipher.javax.crypto.Cipher.IV = iv

            // Encrypt the data
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            // Combine IV + encrypted data for storage
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

            // Return as Base64 for easy storage
            android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            throw RuntimeException("Failed to encrypt sensitive data: ${e.message}", e)
        }
    }

    /**
     * Decrypts sensitive data using AES-256/GCM
     */
    private fun decryptSensitiveData(encryptedText: String): String {
        if (encryptedText.isEmpty()) {
            return encryptedText
        }

        return try {
            val combined = android.util.Base64.decode(encryptedText, android.util.Base64.NO_WRAP)

            // Extract IV and encrypted data
            val iv = ByteArray(12)
            val encryptedBytes = ByteArray(combined.size - 12)
            System.arraycopy(combined, 0, iv, 0, iv.size)
            System.arraycopy(combined, 12, encryptedBytes, 0, encryptedBytes.size)

            // Decrypt the data
            val cipher = javax.crypto.Cipher.getInstance(TRANSFORMATION)
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, getSecretKey(), iv)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            throw RuntimeException("Failed to decrypt sensitive data: ${e.message}", e)
        }
    }

    /**
     * Gets the secret key from Android Keystore
     */
    private fun getSecretKey(): javax.crypto.SecretKey {
        return try {
            val keyStore = java.security.KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)
            keyStore.getKey(CONTACT_KEY_ALIAS, null) as javax.crypto.SecretKey
        } catch (e: Exception) {
            throw RuntimeException("Failed to get secret key from keystore: ${e.message}", e)
        }
    }

    /**
     * Initializes the encryption key in Android Keystore
     */
    private fun initializeEncryptionKey() {
        try {
            val keyStore = java.security.KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)

            // Check if key already exists
            if (!keyStore.containsAlias(CONTACT_KEY_ALIAS)) {
                val keyGenerator = javax.crypto.KeyGenerator.getInstance(
                    javax.crypto.KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER
                )

                // Configure key properties
                val keySpec = android.security.keystore.KeyGenParameterSpec.Builder(
                    CONTACT_KEY_ALIAS,
                    javax.crypto.KeyProperties.PURPOSE_ENCRYPT or javax.crypto.KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(javax.crypto.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(javax.crypto.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setUserAuthenticationRequired(false)
                    .setUnlockedDeviceRequired(false)
                    .build()

                keyGenerator.init(keySpec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            e.printStackThrow RuntimeException("Failed to initialize encryption key: ${e.message}", e)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.emergency_contacts_service_active))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_contact_phone_24)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.emergency_contacts_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = getString(R.string.emergency_contacts_channel_description)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Not a bound service
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopEmergencyContactsService()
    }
}