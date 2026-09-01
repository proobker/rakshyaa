package com.rakshyaa.rakshyaa.data.local

import com.rakshyaa.rakshyaa.data.sync.SyncManager
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Base repository that persists an in-memory list of [T] as an encrypted,
 * cloud-synced JSON datastore. Subclasses provide the element serializer and a
 * unique datastore key.
 */
abstract class EncryptedListRepository<T : Any>(
    protected val store: EncryptedLocalStore,
    protected val sync: SyncManager,
    protected val key: String,
    private val elementSerializer: KSerializer<T>
) {
    protected val json = Json { ignoreUnknownKeys = true }
    private val listSerializer = ListSerializer(elementSerializer)

    protected suspend fun saveAll(list: List<T>) {
        sync.saveAndSync(key, json.encodeToString(listSerializer, list))
    }

    protected suspend fun loadAll(): List<T> {
        val plain = sync.getOrPull(key) ?: "[]"
        return runCatching { json.decodeFromString(listSerializer, plain) }.getOrElse { emptyList() }
    }

    protected suspend fun modify(transform: (List<T>) -> List<T>) {
        saveAll(transform(loadAll()))
    }
}
