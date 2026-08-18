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
import com.rakshyaa.rakshyaa.data.local.SecurePreferences
import com.rakshyaa.rakshyaa.data.LegalHelpRepository
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
 * Service for managing in-app legal resources, emergency numbers, and support information
 */
@AndroidEntryPoint
@hiltService
class LegalHelpService @Inject constructor(
    private val authRepository: AuthRepository,
    private val securePreferences: SecurePreferences,
    private val legalHelpRepository: LegalHelpRepository
) : Service() {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "legal_help_channel"
        private const val NOTIFICATION_ID = 9
        const val ACTION_START_LEGAL_HELP_SERVICE = "ACTION_START_LEGAL_HELP_SERVICE"
        const val ACTION_STOP_LEGAL_HELP_SERVICE = "ACTION_STOP_LEGAL_HELP_SERVICE"
        const val ACTION_LEGAL_HELP_UPDATED = "ACTION_LEGAL_HELP_UPDATED"
        const val ACTION_SYNC_LEGAL_HELP = "ACTION_SYNC_LEGAL_HELP"
        const val EXTRA_CONTENT_TYPE = "extra_content_type"
        const val EXTRA_FORCE_UPDATE = "extra_force_update"
        private const val CACHE_DURATION_MS = 60 * 60 * 1000L // 1 hour
    }

    private var coroutineScope: CoroutineScope? = null
    private var helpJob: Job? = null
    private var isServiceActive = false

    // Cached content for offline access
    private var legalArticles: List<LegalArticle> = emptyList()
    private var emergencyNumbers: List<EmergencyNumber> = emptyList()
    private var supportResources: List<SupportResource> = emptyList()
    private var lastContentUpdateTime: Long = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LEGAL_HELP_SERVICE -> startLegalHelpService()
            ACTION_STOP_LEGAL_HELP_SERVICE -> stopLegalHelpService()
            ACTION_SYNC_LEGAL_HELP -> {
                val forceUpdate = intent.getBooleanExtra(EXTRA_FORCE_UPDATE, false)
                syncLegalHelpContent(forceUpdate)
            }
            ACTION_LEGAL_HELP_UPDATED -> {
                #TODO: Handle legal help content updates
                #For now, just trigger a sync
                syncLegalHelpContent(true)
            }
        }
        return START_STICKY
    }

    private fun startLegalHelpService() {
        if (isServiceActive) return
        isServiceActive = true

        // Start foreground service
        startForeground(NOTIFICATION_ID, buildNotification("Legal Help Service Active"))

        // Set up coroutine scope for legal help service logic
        coroutineScope = CoroutineScope(Dispatchers.Main)
        helpJob = coroutineScope?.launch {
            #TODO: Load cached content if available
            #For now, we'll sync on start
            syncLegalHelpContent(false)
        }
    }

    private fun stopLegalHelpService() {
        if (!isServiceActive) return
        isServiceActive = false

        // Stop foreground service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        // Clean up coroutine scope
        helpJob?.cancel()
        coroutineScope = null
        helpJob = null
    }

    private fun syncLegalHelpContent(forceUpdate: Boolean) {
        val userId = securePreferences.getUserId()
        if (userId.isNullOrBlank()) {
            return
        }

        val timeSinceLastUpdate = System.currentTimeMillis() - lastContentUpdateTime
        if (!forceUpdate && timeSinceLastUpdate < CACHE_DURATION_MS && lastContentUpdateTime > 0) {
            #TODO: Use cached content if it's still fresh
            #For now, we'll always sync for simplicity
        }

        coroutineScope?.launch {
            try {
                #TODO: In a real implementation, we would fetch the latest content from the server
                #For now, we'll just load from the local database/repository
                loadLegalHelpContent()

                lastContentUpdateTime = System.currentTimeMillis()

                #TODO: Notify any UI components or interested parties about the updated content
                android.util.Log.i("LegalHelpService", "Legal help content synced: ${legalArticles.size} articles, ${emergencyNumbers.size} emergency numbers, ${supportResources.size} support resources")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadLegalHelpContent() {
        #TODO: Load content from the repository
        #For now, we'll use placeholder data or load from the repository when it's implemented

        #TODO: Actually load from repository:
        #legalArticles = legalHelpRepository.getLegalArticles()
        #emergencyNumbers = legalHelpRepository.getEmergencyNumbers()
        #supportResources = legalHelpRepository.getSupportResources()

        #For now, using placeholder data until repository is fully implemented
        legalArticles = listOf(
            LegalArticle(
                id = "1",
                title = "Your Rights During Police Encounters",
                content = "You have the right to remain silent...",
                category = "legal_rights",
                lastUpdated = System.currentTimeMillis()
            ),
            LegalArticle(
                id = "2",
                title = "How to Document Domestic Abuse",
                content = "Keep a record of incidents...",
                category = "documentation",
                lastUpdated = System.currentTimeMillis()
            )
        )

        emergencyNumbers = listOf(
            EmergencyNumber(
                id = "1",
                name = "Police Emergency",
                number = "911",
                description = "For emergencies requiring immediate police response",
                isInternational = false
            ),
            EmergencyNumber(
                id = "2",
                name = "Fire Emergency",
                number = "911",
                description = "For fires and medical emergencies requiring fire department response",
                isInternational = false
            ),
            EmergencyNumber(
                id = "3",
                name = "Medical Emergency",
                number = "911",
                description = "For life-threatening medical emergencies",
                isInternational = false
            ),
            EmergencyNumber(
                id = "4",
                name = "National Domestic Violence Hotline",
                number = "1-800-799-7233",
                description = "24/7 confidential support for domestic violence victims",
                isInternational = false
            )
        )

        supportResources = listOf(
            SupportResource(
                id = "1",
                name = "National Sexual Assault Hotline",
                description = "Confidential support 24/7 for sexual assault survivors",
                phoneNumber = "1-800-656-4673",
                website = "https://www.rainn.org",
                category = "support"
            ),
            SupportResource(
                id = "2",
                name = "National Domestic Violence Hotline",
                description = "24/7 confidential support for domestic violence victims",
                phoneNumber = "1-800-799-7233",
                website = "https://www.thehotline.org",
                category = "support"
            )
        )
    }

    /**
     * Gets legal articles (cached for offline access)
     */
    fun getLegalArticles(): List<LegalArticle> {
        return legalArticles
    }

    /**
     * Gets emergency numbers (cached for offline access)
     */
    fun getEmergencyNumbers(): List<EmergencyNumber> {
        return emergencyNumbers
    }

    /**
     * Gets support resources (cached for offline access)
     */
    fun getSupportResources(): List<SupportResource> {
        return supportResources
    }

    /**
     * Gets legal articles by category
     */
    fun getLegalArticlesByCategory(category: String): List<LegalArticle> {
        return legalArticles.filter { it.category == category }
    }

    /**
     * Searches legal articles by title or content
     */
    fun searchLegalArticles(query: String): List<LegalArticle> {
        val lowerCaseQuery = query.lowercase()
        return legalArticles.filter {
            it.title.lowercase().contains(lowerCaseQuery) ||
                    it.content.lowercase().contains(lowerCaseQuery)
        }
    }

    /**
     * Gets emergency number by name
     */
    fun getEmergencyNumberByName(name: String): EmergencyNumber? {
        return emergencyNumbers.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    private fun buildNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.legal_help_service_active))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_gavel_24)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.legal_help_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = getString(R.string.legal_help_channel_description)
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
        stopLegalHelpService()
    }
}