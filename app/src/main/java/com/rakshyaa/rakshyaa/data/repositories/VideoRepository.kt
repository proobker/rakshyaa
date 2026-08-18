package com.rakshyaa.rakshyaa.data.repositories

import com.rakshyaa.rakshyaa.data.SupabaseProvider
import com.rakshyaa.rakshyaa.services.VideoEncryptionService
import io.github.jmnarloch.supabase.kaft.PostgrestException
import io.github.jmnarloch.supabase.kaft.SupabaseClient
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Repository for handling video operations with Supabase Storage and metadata storage
 */
@Singleton
class VideoRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val videoEncryptionService: VideoEncryptionService
) {

    companion object {
        private const val VIDEO_BUCKET = "encrypted-videos"
    }

    /**
     * Uploads an encrypted video to Supabase Storage
     *
     * @param userId The ID of the user uploading the video
     * @param videoFile The original video file to be encrypted and uploaded
     * @param videoType Type of video (e.g., "sos_incident", "ride_recording")
     * @return The public URL of the uploaded video
     * @throws Exception if upload fails
     */
    suspend fun uploadEncryptedVideo(
        userId: String,
        videoFile: File,
        videoType: String = "general"
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                // Step 1: Encrypt the video file
                val encryptedFile = videoEncryptionService.encryptVideo(videoFile)

                // Step 2: Generate a unique filename
                val timestamp = System.currentTimeMillis()
                val fileName = "${userId}_${videoType}_${timestamp}.enc"

                // Step 3: Upload the encrypted file to Supabase Storage
                encryptedFile.inputStream().use { inputStream ->
                    supabaseClient
                        .storage
                        .bucket(VIDEO_BUCKET)
                        .upload(fileName, inputStream)
                }

                // Step 4: Get the public URL for the uploaded video
                val videoUrl = supabaseClient
                    .storage
                    .bucket(VIDEO_BUCKET)
                    .getPublicUrl(fileName)

                // Step 5: Save video metadata to the database
                saveVideoMetadata(
                    userId = userId,
                    videoUrl = videoUrl,
                    videoType = videoType,
                    fileName = fileName,
                    uploadedAt = timestamp
                )

                // Step 6: Clean up the encrypted temporary file
                encryptedFile.delete()

                videoUrl
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to upload video: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error uploading video: ${e.message}", e)
            }
        }
    }

    /**
     * Saves video metadata to the videos table
     */
    private suspend fun saveVideoMetadata(
        userId: String,
        videoUrl: String,
        videoType: String,
        fileName: String,
        uploadedAt: Long
    ) {
        withContext(Dispatchers.IO) {
            try {
                val videoData = mapOf(
                    "user_id" to userId,
                    "video_url" to videoUrl,
                    "video_type" to videoType,
                    "file_name" to fileName,
                    "uploaded_at" to uploadedAt,
                    "created_at" to System.currentTimeMillis()
                )

                supabaseClient
                    .from("videos")
                    .insert(videoData)
                    .execute()
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to save video metadata: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error saving video metadata: ${e.message}", e)
            }
        }
    }

    /**
     * Retrieves video metadata for a user
     */
    suspend fun getUserVideos(
        userId: String,
        limit: Int = 50,
        offset: Int = 0
    ): List<VideoRecord> {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseClient
                    .from("videos")
                    .select("*")
                    .eq("user_id", userId)
                    .order("uploaded_at", ascending = false)
                    .limit(limit.toString())
                    .offset(offset.toString())
                    .execute()

                val data = response.data as List<<Map<String, Any>>
                return data.map { record ->
                    VideoRecord(
                        id = record["id"] as String,
                        userId = record["user_id"] as String,
                        videoUrl = record["video_url"] as String,
                        videoType = record["video_type"] as String,
                        fileName = record["file_name"] as String,
                        uploadedAt = record["uploaded_at"] as Long,
                        createdAt = record["created_at"] as Long
                    )
                }
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to get user videos: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error getting user videos: ${e.message}", e)
            }
        }
    }

    /**
     * Gets a video record by ID
     */
    suspend fun getVideoById(videoId: String): VideoRecord? {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseClient
                    .from("videos")
                    .select("*")
                    .eq("id", videoId)
                    .single()
                    .execute()

                val data = response.data as Map<String, Any>
                return VideoRecord(
                    id = data["id"] as String,
                    userId = data["user_id"] as String,
                    videoUrl = data["video_url"] as String,
                    videoType = data["video_type"] as String,
                    fileName = data["file_name"] as String,
                    uploadedAt = data["uploaded_at"] as Long,
                    createdAt = data["created_at"] as Long
                )
            } catch (e: PostgrestException) {
                if (e.code == "PGRST116") {
                    // No rows returned
                    return null
                }
                throw RuntimeException("Failed to get video: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error getting video: ${e.message}", e)
            }
        }
    }
}

/**
 * Data class representing a video record
 */
data class VideoRecord(
    val id: String,
    val userId: String,
    val videoUrl: String,
    val videoType: String,
    val fileName: String,
    val uploadedAt: Long,
    val createdAt: Long
)