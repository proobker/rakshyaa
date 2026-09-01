package com.rakshyaa.rakshyaa.data.models

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val name: String = "",
    val email: String? = null,
    val phone: String = "",
    val emergencyContactId: String? = null
)

@Serializable
data class EmergencyContact(
    val id: String = "",
    val name: String,
    val phoneNumber: String,
    val relationship: String,
    val isPrimary: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class Incident(
    val id: String = "",
    val status: String = "active",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val activatedAt: Long = System.currentTimeMillis(),
    val note: String = ""
)

@Serializable
data class CheckIn(
    val id: String = "",
    val scheduledAt: Long,
    val checkedInAt: Long? = null,
    val status: String = "pending", // pending | completed | missed | escalated
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class RideSession(
    val id: String = "",
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val waypoints: List<RoutePoint> = emptyList(),
    val deviated: Boolean = false,
    val deviationAlertAt: Long? = null
)

@Serializable
data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)

@Serializable
data class SafePlace(
    val id: String = "",
    val name: String,
    val address: String = "",
    val latitude: Double,
    val longitude: Double,
    val type: String = "other" // hospital | police | fire | user
)

@Serializable
data class LegalResource(
    val id: String = "",
    val title: String,
    val body: String,
    val category: String = "general",
    val phone: String? = null
)
