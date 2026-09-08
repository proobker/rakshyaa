package com.rakshyaa.rakshyaa.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rakshyaa.rakshyaa.data.models.LegalResource
import com.rakshyaa.rakshyaa.services.LegalHelpService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LegalHelpViewModel @Inject constructor(
    private val legalHelpService: LegalHelpService
) : ViewModel() {

    data class UiState(
        val allResources: List<LegalResource> = emptyList(),
        val filteredResources: List<LegalResource> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val selectedCategory: String = "all",
        val searchQuery: String = ""
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadResources()
    }

    fun loadResources() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val resources = legalHelpService.allResources()
            _uiState.value = _uiState.value.copy(
                allResources = resources,
                filteredResources = resources,
                isLoading = false
            )
        }
    }

    fun filterByCategory(category: String) {
        val current = _uiState.value
        val filtered = if (category == "all") {
            current.allResources
        } else {
            current.allResources.filter { it.category == category }
        }
        _uiState.value = current.copy(
            filteredResources = filtered,
            selectedCategory = category
        )
    }

    fun search(query: String) {
        val current = _uiState.value
        if (query.isBlank()) {
            val base = if (current.selectedCategory == "all") current.allResources
            else current.allResources.filter { it.category == current.selectedCategory }
            _uiState.value = current.copy(filteredResources = base, searchQuery = query)
            return
        }
        _uiState.value = current.copy(searchQuery = query)
        viewModelScope.launch {
            val results = legalHelpService.search(query)
            _uiState.value = _uiState.value.copy(filteredResources = results)
        }
    }

    fun addNote(title: String, body: String, phone: String? = null) {
        viewModelScope.launch {
            legalHelpService.addNote(title, body, phone)
            loadResources()
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            legalHelpService.deleteNote(id)
            loadResources()
        }
    }

    fun getCategories(): List<String> {
        val categories = _uiState.value.allResources.map { it.category }.distinct()
        return listOf("all") + categories.filter { it != "user" } + "user"
    }
}