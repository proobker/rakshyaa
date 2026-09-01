package com.rakshyaa.rakshyaa.data.repositories

import com.rakshyaa.rakshyaa.data.local.EncryptedLocalStore
import com.rakshyaa.rakshyaa.data.models.VideoRecord
import com.rakshyaa.rakshyaa.data.sync.SyncManager
import com.rakshyaa.rakshyaa.services.VideoEncryptionService
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages encrypted video recordings: videos are encrypted on-device
 * ([VideoEncryptionService]) and their encrypted bytes are backed up to the backend
 * as opaque blobs. Only video metadata is kept locally (encrypted).
 */
@Singleton
class VideoRepository @Inject constructor(
    private val store: EncryptedLocalStore,
    private val sync: SyncManager,
    private val videoEncryptionService: VideoEncryptionService
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val listSerializer = ListSerializer(VideoRecord.serializer())
    private val key = "videos"

    /** Encrypts [videoFile], records its metadata and backs up the encrypted blob. */
    suspend fun uploadEncryptedVideo(
        userId: String,
        videoFile: File,
        videoType: String = "general"
    ): VideoRecord {
        val encryptedFile = videoEncryptionService.encryptVideo(videoFile)
        val id = UUID.randomUUID().toString()
        val fileName = "${userId}_${videoType}_${System.currentTimeMillis()}.enc"

        val encryptedBytes = encryptedFile.readBytes()
        sync.pushMedia(id, encryptedBytes)
        encryptedFile.delete()

        val record = VideoRecord(
            id = id,
            videoType = videoType,
            fileName = fileName,
            createdAt = System.currentTimeMillis()
        )
        modify { it + record }
        return record
    }

    /** Returns stored video metadata, most recent first. */
    suspend fun getUserVideos(limit: Int = 100): List<VideoRecord> =
        loadAll().sortedByDescending { it.createdAt }.take(limit)

    /** Returns a single video record by id, if present. */
    suspend fun getVideoById(videoId: String): VideoRecord? =
        loadAll().firstOrNull { it.id == videoId }

    /** Downloads and decrypts a previously backed-up video into a cache file. */
    suspend fun downloadAndDecrypt(videoId: String): File? {
        val record = getVideoById(videoId) ?: return null
        val encryptedBytes = sync.pullMedia(videoId) ?: return null
        val encFile = File(storeFileDirFor(videoId), "$videoId.enc")
        encFile.writeBytes(encryptedBytes)
        return videoEncryptionService.decryptVideo(encFile)
    }

    /** Removes a video record and its encrypted local/media reference. */
    suspend fun remove(videoId: String) {
        modify { list -> list.filterNot { it.id == videoId } }
    }

    private suspend fun loadAll(): List<VideoRecord> {
        val raw = store.loadPlain(key) ?: return emptyList()
        return runCatching { json.decodeFromString(listSerializer, raw) }
            .getOrElse { emptyList() }
    }

    private suspend fun modify(transform: (List<VideoRecord>) -> List<VideoRecord>) {
        val updated = transform(loadAll())
        store.savePlain(key, json.encodeToString(listSerializer, updated))
    }

    private fun storeFileDirFor(videoId: String): File {
        val dir = File(store.fileFor(key).parentFile, "video_$videoId")
        dir.mkdirs()
        return dir
    }
}
