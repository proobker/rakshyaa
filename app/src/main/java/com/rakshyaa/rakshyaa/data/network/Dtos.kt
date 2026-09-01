package com.rakshyaa.rakshyaa.data.network

import kotlinx.serialization.Serializable

@Serializable
data class GoogleAuthRequest(val idToken: String)

@Serializable
data class GoogleAuthResponse(val token: String, val user: UserDto)

@Serializable
data class UserDto(
    val sub: String,
    val email: String? = null,
    val name: String? = null,
    val picture: String? = null
)

@Serializable
data class MeResponse(val user: UserDto)

@Serializable
data class BlobDto(
    val key: String,
    val kind: String,
    val size: Long,
    val checksum: String? = null,
    val updatedAt: Long
)

@Serializable
data class BackupListResponse(val blobs: List<BlobDto>)

@Serializable
data class ErrorResponse(val error: String? = null)

@Serializable
data class IncidentRequest(
    val status: String = "active",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val activatedAt: Long? = null
)

@Serializable
data class IncidentResponse(val ok: Boolean, val id: String? = null)

@Serializable
data class OkResponse(val ok: Boolean)
