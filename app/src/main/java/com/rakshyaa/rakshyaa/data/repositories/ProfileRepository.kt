package com.rakshyaa.rakshyaa.data.repositories

import android.content.Context
import android.net.Uri
import com.rakshyaa.rakshyaa.data.auth.AuthRepository
import com.rakshyaa.rakshyaa.data.local.EncryptedLocalStore
import com.rakshyaa.rakshyaa.data.network.ApiClient
import com.rakshyaa.rakshyaa.data.network.MeResponse
import com.rakshyaa.rakshyaa.data.network.UpdateProfileRequest
import com.rakshyaa.rakshyaa.data.network.UserDto
import com.rakshyaa.rakshyaa.data.sync.SyncManager
import com.rakshyaa.rakshyaa.services.VideoEncryptionService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persists the user-editable profile (name, phone, bio, picture). Text fields are
 * cached in [EncryptedLocalStore]; the server copy is kept in sync via
 * [ApiClient] so the profile survives logout/reinstall.
 *
 * The picture is stored as an opaque encrypted blob on the backend and referenced
 * by `picture = "media:<id>"` (Google avatar URLs are left untouched).
 */
@Singleton
class ProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiClient: ApiClient,
    private val store: EncryptedLocalStore,
    private val sync: SyncManager,
    private val videoEncryptionService: VideoEncryptionService,
    private val authRepository: AuthRepository
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun localName(): String? = store.loadPlain(PROFILE_NAME_KEY)
    fun localPhone(): String? = store.loadPlain(PROFILE_PHONE_KEY)
    fun localBio(): String? = store.loadPlain(PROFILE_BIO_KEY)
    fun localPictureRef(): String? = store.loadPlain(PROFILE_PICTURE_KEY)

    /** Google avatar of the signed-in user, or null. */
    fun googlePicture(): String? = authRepository.state.value.user?.picture

    /** Merged picture to display: uploaded media ref if set, else Google avatar. */
    fun displayPictureRef(): String? = localPictureRef()?.takeIf { it.isNotEmpty() } ?: googlePicture()

    suspend fun fetchRemoteProfile(): UserDto? = withContext(Dispatchers.IO) {
        runCatching {
            val body = apiClient.get("/user/profile")
            json.decodeFromString<MeResponse>(body).user
        }.getOrNull()
    }

    /** Pushes editable profile fields; keeps the existing server picture unless one is passed. */
    suspend fun saveProfile(name: String, phone: String, bio: String): UserDto? {
        val existing = fetchRemoteProfile()
        val picture = existing?.picture
            ?: authRepository.state.value.user?.picture
            ?: localPictureRef()?.takeIf { it.isNotEmpty() }
        val updated = updateProfile(name, phone, bio, picture)
        if (updated != null) {
            store.savePlain(PROFILE_NAME_KEY, updated.name ?: name)
            store.savePlain(PROFILE_PHONE_KEY, updated.phone ?: phone)
            store.savePlain(PROFILE_BIO_KEY, updated.bio ?: bio)
            updated.picture?.let { store.savePlain(PROFILE_PICTURE_KEY, it) }
        }
        return updated
    }

    /** Encrypts the picked image, pushes it as a media blob, and points profile.picture at it. */
    suspend fun uploadAndApplyPicture(uri: Uri): UserDto? = withContext(Dispatchers.IO) {
        runCatching {
            val cacheFile = File(context.cacheDir, "avatar_${System.currentTimeMillis()}")
            context.contentResolver.openInputStream(uri)?.use { input ->
                cacheFile.outputStream().use { out -> input.copyTo(out) }
            } ?: return@runCatching null

            val encrypted = videoEncryptionService.encryptVideo(cacheFile)
            val mediaId = "profile-picture"
            sync.pushMedia(mediaId, encrypted.readBytes())
            encrypted.delete()
            cacheFile.delete()

            val localName = localName()
            val localPhone = localPhone()
            val localBio = localBio()
            val updated = updateProfile(localName.orEmpty(), localPhone.orEmpty(), localBio.orEmpty(), "media:$mediaId")
            updated?.let { store.savePlain(PROFILE_PICTURE_KEY, "media:$mediaId") }
            updated
        }.getOrNull()
    }

    /** Downloads + decrypts the uploaded picture into a cache file, or null. */
    suspend fun cachedPictureFile(): File? = withContext(Dispatchers.IO) {
        val ref = displayPictureRef() ?: return@withContext null
        if (!ref.startsWith("media:")) return@withContext null
        val mediaId = ref.removePrefix("media:")
        runCatching {
            val encBytes = sync.pullMedia(mediaId) ?: return@runCatching null
            val encFile = File(context.cacheDir, "avatar_$mediaId.enc")
            encFile.writeBytes(encBytes)
            videoEncryptionService.decryptVideo(encFile)
        }.getOrNull()
    }

    /** Pulls the latest profile from the backend into local storage (used on login/restore). */
    suspend fun syncProfileFromRemote() {
        val user = fetchRemoteProfile() ?: return
        user.name?.let { store.savePlain(PROFILE_NAME_KEY, it) }
        user.phone?.let { store.savePlain(PROFILE_PHONE_KEY, it) }
        user.bio?.let { store.savePlain(PROFILE_BIO_KEY, it) }
        user.picture?.let { store.savePlain(PROFILE_PICTURE_KEY, it) }
    }

    private suspend fun updateProfile(name: String, phone: String, bio: String, picture: String?): UserDto? =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = json.encodeToString(
                    UpdateProfileRequest(
                        name = name.ifEmpty { null },
                        phone = phone.ifEmpty { null },
                        bio = bio.ifEmpty { null },
                        picture = picture
                    )
                )
                val resp = apiClient.putJson("/user/profile", body)
                json.decodeFromString<MeResponse>(resp).user
            }.getOrNull()
        }

    private companion object {
        const val PROFILE_NAME_KEY = "profile_name"
        const val PROFILE_PHONE_KEY = "profile_phone"
        const val PROFILE_BIO_KEY = "profile_bio"
        const val PROFILE_PICTURE_KEY = "profile_picture_ref"
    }
}