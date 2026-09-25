package com.rakshyaa.rakshyaa.data.sync

import com.google.common.truth.Truth.assertThat
import com.rakshyaa.rakshyaa.data.local.EncryptedLocalStore
import com.rakshyaa.rakshyaa.data.network.ApiClient
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.*

class SyncManagerUnitTest {
    @Test fun `unreadable remote backup preserves local data`() = runTest {
        val store = mock(EncryptedLocalStore::class.java)
        val api = mock(ApiClient::class.java)
        `when`(store.accountId).thenReturn("owner")
        `when`(api.getRaw("/backup/data/contacts")).thenReturn("foreign-key-ciphertext".toByteArray())
        `when`(store.restoreRaw("contacts", "foreign-key-ciphertext")).thenReturn(false)
        `when`(store.loadPlain("contacts")).thenReturn("local-contacts")
        val sync = SyncManager(store, api)
        assertThat(sync.pull("contacts")).isEqualTo("local-contacts")
        assertThat(sync.error.value).contains("original installation")
        verify(store, never()).writeRaw(anyString(), anyString())
    }

    @Test fun `switching account during download discards response`() = runTest {
        val store = mock(EncryptedLocalStore::class.java)
        val api = mock(ApiClient::class.java)
        `when`(store.accountId).thenReturn("owner", "other")
        `when`(api.getRaw("/backup/data/contacts")).thenReturn("ciphertext".toByteArray())
        val sync = SyncManager(store, api)
        assertThat(sync.pull("contacts")).isNull()
        verify(store, never()).restoreRaw(anyString(), anyString())
    }

    @Test fun `disabled backup never uploads locally saved data`() = runTest {
        val store = mock(EncryptedLocalStore::class.java)
        val api = mock(ApiClient::class.java)
        `when`(store.readRaw("contacts")).thenReturn("encrypted")
        `when`(store.loadPlain("profile_settings")).thenReturn("""{"backupEnabled":false}""")
        SyncManager(store, api).saveAndSync("contacts", "local")
        verify(store).savePlain("contacts", "local")
        verifyNoInteractions(api)
    }
}
