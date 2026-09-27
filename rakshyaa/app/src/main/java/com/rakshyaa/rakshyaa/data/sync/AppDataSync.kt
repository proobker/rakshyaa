package com.rakshyaa.rakshyaa.data.sync

import com.rakshyaa.rakshyaa.data.repositories.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Restores the signed-in user's data on login / app start: pulls every encrypted
 * blob the backend has for the current session, then syncs the editable profile.
 */
@Singleton
class AppDataSync @Inject constructor(
    private val sync: SyncManager,
    private val profileRepository: ProfileRepository
) {
    suspend fun restoreAll() {
        for (key in sync.remoteKeys()) {
            sync.pull(key)
        }
        profileRepository.syncProfileFromRemote()
    }
}