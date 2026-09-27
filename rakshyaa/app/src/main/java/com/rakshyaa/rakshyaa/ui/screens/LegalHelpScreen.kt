package com.rakshyaa.rakshyaa.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rakshyaa.rakshyaa.R
import com.rakshyaa.rakshyaa.data.models.LegalResource
import com.rakshyaa.rakshyaa.viewmodels.LegalHelpViewModel

@Composable
fun LegalHelpScreen(
    viewModel: LegalHelpViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogBody by remember { mutableStateOf("") }
    var dialogPhone by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var resourceToDelete by remember { mutableStateOf<LegalResource?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(stringResource(R.string.legal_help_title)) },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    dialogTitle = ""
                    dialogBody = ""
                    dialogPhone = ""
                    showAddDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_note)) },
                modifier = Modifier.padding(16.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                androidx.compose.material3.TextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        viewModel.search(it)
                    },
                    label = { Text("Search") },
                    placeholder = { Text(stringResource(R.string.search_legal_resources)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    singleLine = true,
                    colors = androidx.compose.material3.TextFieldDefaults.textFieldColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
            }

            // Category Chips
            if (uiState.allResources.isNotEmpty()) {
                val categories = viewModel.getCategories()
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = uiState.selectedCategory == category
                        androidx.compose.material3.FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.filterByCategory(category) },
                            label = { Text(category.uppercase()) }
                        )
                    }
                }
            }

            // Resources List
            if (uiState.filteredResources.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) stringResource(R.string.no_search_results) else stringResource(R.string.no_legal_resources),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                ) {
                    items(uiState.filteredResources) { resource ->
                        LegalResourceCard(
                            resource = resource,
                            onCall = { callPhone(context, resource.phone) },
                            onDelete = if (resource.category == "user") {
                                {
                                    resourceToDelete = resource
                                    showDeleteConfirm = true
                                }
                            } else null
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // Add Note Dialog
    if (showAddDialog) {
        AddLegalNoteDialog(
            title = dialogTitle,
            onTitleChange = { dialogTitle = it },
            body = dialogBody,
            onBodyChange = { dialogBody = it },
            phone = dialogPhone,
            onPhoneChange = { dialogPhone = it },
            onConfirm = {
                if (dialogTitle.isNotBlank() && dialogBody.isNotBlank()) {
                    viewModel.addNote(dialogTitle, dialogBody, dialogPhone.ifBlank { null })
                    showAddDialog = false
                }
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false; resourceToDelete = null },
            title = { Text(stringResource(R.string.delete_confirmation_title)) },
            text = { Text(stringResource(R.string.delete_note_confirmation)) },
            confirmButton = {
                Button(
                    onClick = {
                        resourceToDelete?.let { viewModel.deleteNote(it.id) }
                        showDeleteConfirm = false
                        resourceToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirm = false; resourceToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun LegalResourceCard(
    resource: LegalResource,
    onCall: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val colors = MaterialTheme.colorScheme
    val categoryColor = when (resource.category) {
        "emergency" -> Color(0xFFF44336)
        "helpline" -> Color(0xFF2196F3)
        "general" -> Color(0xFF9C27B0)
        "user" -> Color(0xFF009688)
        else -> colors.primary
    }
    val categoryIcon = when (resource.category) {
        "emergency" -> Icons.Default.Help
        "helpline" -> Icons.Default.Phone
        "general" -> Icons.Default.Gavel
        "user" -> Icons.Default.Edit
        else -> Icons.Default.Gavel
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (resource.category == "user") colors.primaryContainer.copy(alpha = 0.3f) else colors.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(categoryColor.copy(alpha = 0.15f))
                        .clip(androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = resource.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .background(categoryColor.copy(alpha = 0.2f))
                                .clip(RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                text = resource.category.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = categoryColor,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = resource.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 3
                    )
                }

                if (resource.phone != null) {
                    IconButton(onClick = onCall) {
                        Icon(Icons.Default.Call, contentDescription = stringResource(R.string.call), tint = colors.primary)
                    }
                }
                onDelete?.let { deleteAction ->
                    IconButton(onClick = deleteAction) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = colors.error)
                    }
                }
            }

            if (resource.phone != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = resource.phone!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

fun callPhone(context: android.content.Context, phone: String?) {
    phone?.let { number ->
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$number")
        }
        context.startActivity(intent)
    }
}

@Composable
fun AddLegalNoteDialog(
    title: String,
    onTitleChange: (String) -> Unit,
    body: String,
    onBodyChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_note)) },
        text = {
            Column(
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                androidx.compose.material3.OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text(stringResource(R.string.note_title)) },
                    placeholder = { Text(stringResource(R.string.enter_note_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                androidx.compose.material3.OutlinedTextField(
                    value = body,
                    onValueChange = onBodyChange,
                    label = { Text(stringResource(R.string.note_body)) },
                    placeholder = { Text(stringResource(R.string.enter_note_body)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 3,
                    maxLines = 5
                )
                androidx.compose.material3.OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = { Text(stringResource(R.string.helpline_phone_optional)) },
                    placeholder = { Text("+1 XXX XXXX") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = title.isNotBlank() && body.isNotBlank()) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}