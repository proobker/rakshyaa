package com.rakshyaa.rakshyaa.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rakshyaa.rakshyaa.data.models.EmergencyContact
import com.rakshyaa.rakshyaa.services.EmergencyContactsService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmergencyContactsViewModel @Inject constructor(
    private val contactsService: EmergencyContactsService
) : ViewModel() {

    data class UiState(
        val contacts: List<EmergencyContact> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val isSyncing: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    init {
        loadContacts()
    }

    fun loadContacts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val contacts = contactsService.listContacts()
            _uiState.value = _uiState.value.copy(
                contacts = contacts,
                isLoading = false
            )
        }
    }

    fun addContact(name: String, phone: String, relationship: String, isPrimary: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            contactsService.addContact(name, phone, relationship, isPrimary)
            loadContacts()
        }
    }

    fun updateContact(id: String, name: String, phone: String, relationship: String, isPrimary: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            contactsService.updateContact(id, name, phone, relationship, isPrimary)
            loadContacts()
        }
    }

    fun removeContact(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            contactsService.removeContact(id)
            loadContacts()
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            loadContacts()
            _uiState.value = _uiState.value.copy(isSyncing = false)
        }
    }

    val primaryContact: StateFlow<EmergencyContact?> = uiState
    .map { it.contacts.firstOrNull { it.isPrimary } }
    .distinctUntilChanged()
    .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(), null)
}