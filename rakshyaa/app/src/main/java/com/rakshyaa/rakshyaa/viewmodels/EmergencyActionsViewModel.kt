package com.rakshyaa.rakshyaa.viewmodels

import android.content.Intent
import androidx.lifecycle.ViewModel
import com.rakshyaa.rakshyaa.data.repositories.EmergencyContactsRepository
import com.rakshyaa.rakshyaa.data.repositories.LocationRepository
import com.rakshyaa.rakshyaa.ui.components.emergencyMessageIntent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class EmergencyActionsViewModel @Inject constructor(
    private val contacts: EmergencyContactsRepository,
    private val locations: LocationRepository
) : ViewModel() {
    suspend fun message(): Intent {
        val phones = withTimeoutOrNull(3000) {
            runCatching { contacts.getAll().sortedByDescending { it.isPrimary }.map { it.phoneNumber } }.getOrDefault(emptyList())
        }.orEmpty()
        val location = withTimeoutOrNull(3000) { runCatching { locations.currentLocation() }.getOrNull() }
        val body = buildString {
            append("I need help. Please contact me now.")
            if (location != null) {
                append(" Last available location: https://maps.google.com/?q=")
                append(location.latitude); append(","); append(location.longitude)
            } else append(" My location is unavailable.")
        }
        return emergencyMessageIntent(phones, body)
    }
}
