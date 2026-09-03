package com.rakshyaa.rakshyaa.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rakshyaa.rakshyaa.R
import com.rakshyaa.rakshyaa.data.models.EmergencyContact
import com.rakshyaa.rakshyaa.viewmodels.EmergencyContactsViewModel

@Composable
fun EmergencyContactsScreen(
    viewModel: EmergencyContactsViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = remember { context as ComponentActivity }

    var showDialog by remember { mutableStateOf(false) }
    var editingContact by remember { mutableStateOf<EmergencyContact?>(null) }
    var dialogName by remember { mutableStateOf("") }
    var dialogPhone by remember { mutableStateOf("") }
    var dialogRelationship by remember { mutableStateOf("Friend") }
    var dialogIsPrimary by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var contactToDelete by remember { mutableStateOf<EmergencyContact?>(null) }

    val relationships = listOf(
        stringResource(R.string.relationship_spouse),
        stringResource(R.string.relationship_parent),
        stringResource(R.string.relationship_sibling),
        stringResource(R.string.relationship_friend),
        stringResource(R.string.relationship_other)
    )

    val pickContact = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri ->
        uri?.let {
            val cursor = context.contentResolver.query(
                it,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null, null
            )
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val phone = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    dialogPhone = phone.replace("\\D".toRegex(), "")
                }
            }
        }
    }

    val resetDialog = {
        dialogName = editingContact?.name ?: ""
        dialogPhone = editingContact?.phoneNumber ?: ""
        dialogRelationship = editingContact?.relationship ?: "Friend"
        dialogIsPrimary = editingContact?.isPrimary ?: false
    }

    val openAddDialog = {
        editingContact = null
        resetDialog()
        showDialog = true
    }

    val openEditDialog = { contact: EmergencyContact ->
        editingContact = contact
        resetDialog()
        showDialog = true
    }

    val confirmDelete = { contact: EmergencyContact ->
        contactToDelete = contact
        showDeleteConfirm = true
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(stringResource(R.string.contacts_title)) },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = openAddDialog,
                icon = { Icon(Icons.Default.Person, contentDescription = null) },
                text = { Text(stringResource(R.string.add_contact)) },
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
            if (uiState.contacts.isEmpty()) {
                // Empty State
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.no_contacts),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = openAddDialog) {
                        Text(stringResource(R.string.add_contact))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                ) {
                    items(uiState.contacts) { contact ->
                        ContactCard(
                            contact = contact,
                            onCall = { launchCall(contact.phoneNumber) },
                            onMessage = { launchSms(contact.phoneNumber) },
                            onEdit = { openEditDialog(contact) },
                            onDelete = { confirmDelete(contact) }
                        )
                    }
                }
            }

            // Sync Now Button at bottom
            if (uiState.contacts.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { viewModel.syncNow() },
                        enabled = !uiState.isSyncing,
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uiState.isSyncing) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp).padding(end = 8.dp)
                                )
                            }
                            Text(stringResource(R.string.sync_now))
                        }
                    }
                }
            }
        }
    }

    // Add/Edit Contact Dialog
    if (showDialog) {
        ContactDialog(
            isEditing = editingContact != null,
            name = dialogName,
            onNameChange = { dialogName = it },
            phone = dialogPhone,
            onPhoneChange = { dialogPhone = it },
            relationship = dialogRelationship,
            onRelationshipChange = { dialogRelationship = it },
            relationships = relationships,
            isPrimary = dialogIsPrimary,
            onPrimaryChange = { dialogIsPrimary = it },
            onPickContact = { pickContact.launch(null) },
            onSave = {
                if (dialogName.isNotBlank() && dialogPhone.isNotBlank()) {
                    if (editingContact != null) {
                        viewModel.updateContact(
                            editingContact!!.id,
                            dialogName,
                            dialogPhone,
                            dialogRelationship,
                            dialogIsPrimary
                        )
                    } else {
                        viewModel.addContact(
                            dialogName,
                            dialogPhone,
                            dialogRelationship,
                            dialogIsPrimary
                        )
                    }
                    showDialog = false
                    editingContact = null
                }
            },
            onDismiss = { showDialog = false; editingContact = null }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false; contactToDelete = null },
            title = { Text(stringResource(R.string.delete_confirmation_title)) },
            text = { Text(stringResource(R.string.delete_confirmation_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        contactToDelete?.let { viewModel.removeContact(it.id) }
                        showDeleteConfirm = false
                        contactToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.delete_contact))
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirm = false; contactToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun ContactCard(
    contact: EmergencyContact,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val initial = contact.name.firstOrNull()?.uppercase() ?: "?"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (contact.isPrimary) colors.primaryContainer else colors.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                if (contact.isPrimary) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(colors.primary)
                            .clip(CircleShape)
                    ) {
                        Text(
                            text = initial.toString(),
                            color = colors.onPrimary,
                            fontSize = 20.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(colors.surfaceContainerHighest)
                            .clip(CircleShape)
                    ) {
                        Text(
                            text = initial.toString(),
                            color = colors.onSurfaceVariant,
                            fontSize = 20.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )
                    }
                }
            }

            // Contact Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                        fontWeight = if (contact.isPrimary) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                    )
                    if (contact.isPrimary) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .background(colors.primary)
                                .clip(RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                text = stringResource(R.string.primary_contact),
                                color = colors.onPrimary,
                                fontSize = 10.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = contact.relationship,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onCall) {
                    Icon(Icons.Default.Call, contentDescription = stringResource(R.string.call), tint = colors.primary)
                }
                IconButton(onClick = onMessage) {
                    Icon(Icons.Default.Message, contentDescription = stringResource(R.string.message), tint = colors.primary)
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_contact), tint = colors.onSurfaceVariant)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_contact), tint = colors.error)
                }
            }
        }
    }
}

@Composable
fun ContactDialog(
    isEditing: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    relationship: String,
    onRelationshipChange: (String) -> Unit,
    relationships: List<String>,
    isPrimary: Boolean,
    onPrimaryChange: (Boolean) -> Unit,
    onPickContact: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) stringResource(R.string.edit_contact) else stringResource(R.string.add_contact),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.contact_name)) },
                    placeholder = { Text(stringResource(R.string.enter_first_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = { Text(stringResource(R.string.contact_phone)) },
                    placeholder = { Text("+1 XXX XXXX") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                // Relationship Dropdown
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.contact_relationship),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.material3.ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        androidx.compose.material3.TextField(
                            value = relationship,
                            onValueChange = { },
                            label = { Text(relationship) },
                            readOnly = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = if (expanded) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.TextFieldDefaults.textFieldColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            relationships.forEach { rel ->
                                DropdownMenuItem(
                                    text = { Text(rel) },
                                    onClick = {
                                        onRelationshipChange(rel)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                // Primary Checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Checkbox(
                        checked = isPrimary,
                        onCheckedChange = onPrimaryChange,
                        colors = androidx.compose.material3.CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = stringResource(R.string.set_as_primary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                // Pick from Contacts Button
                Button(
                    onClick = onPickContact,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContactPhone,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp).padding(end = 8.dp)
                        )
                        Text(stringResource(R.string.pick_from_contacts))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
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

@Composable
private fun launchCall(phoneNumber: String) {
    val context = LocalContext.current
    val intent = Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse("tel:$phoneNumber")
    }
    context.startActivity(intent)
}

@Composable
private fun launchSms(phoneNumber: String) {
    val context = LocalContext.current
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("smsto:$phoneNumber")
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    }
}