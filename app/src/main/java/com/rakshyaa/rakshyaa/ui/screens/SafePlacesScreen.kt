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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rakshyaa.rakshyaa.R
import com.rakshyaa.rakshyaa.data.models.SafePlace
import com.rakshyaa.rakshyaa.services.SafePlacesService
import com.rakshyaa.rakshyaa.viewmodels.SafePlacesViewModel
import kotlinx.coroutines.launch

@Composable
fun SafePlacesScreen(
    viewModel: SafePlacesViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var dialogName by remember { mutableStateOf("") }
    var dialogAddress by remember { mutableStateOf("") }
    var dialogLatitude by remember { mutableStateOf(0.0) }
    var dialogLongitude by remember { mutableStateOf(0.0) }
    var dialogType by remember { mutableStateOf("user") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var placeToDelete by remember { mutableStateOf<SafePlace?>(null) }
    var useCurrentLocation by remember { mutableStateOf(false) }

    val types = listOf(
        "user" to Icons.Default.Favorite,
        "hospital" to Icons.Default.LocalHospital,
        "police" to Icons.Default.LocalPolice,
        "fire" to Icons.Default.LocalFireDepartment
    )

    // Load nearby places on first load
    androidx.compose.runtime.LaunchedEffect(key1 = true) {
        // Request location permission and get current location
        // For now, we'll use a default location
        viewModel.loadNearbyPlaces(27.7172, 85.3240)
        viewModel.loadUserPlaces()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(stringResource(R.string.safe_places_title)) },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                actions = {
                    IconButton(onClick = { viewModel.loadNearbyPlaces(27.7172, 85.3240) }) {
                        Icon(Icons.Default.MyLocation, contentDescription = stringResource(R.string.use_current_location))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    dialogName = ""
                    dialogAddress = ""
                    dialogType = "user"
                    showAddDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_safe_place)) },
                modifier = Modifier.padding(16.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Radius Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
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
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Search radius: ${uiState.searchRadius.toInt() / 1000} km",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Slider(
                                value = (uiState.searchRadius / 1000).toFloat(),
                                onValueChange = { viewModel.setSearchRadius(it.toDouble() * 1000) },
                                valueRange = 1f..20f,
                                steps = 19,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Nearby Places Section
            if (uiState.nearbyPlaces.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.nearby_safe_places),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                    ) {
                        items(uiState.nearbyPlaces) { place ->
                            SafePlaceCard(
                                place = place,
                                isUserPlace = false,
                                onNavigate = { navigateToPlace(context, place) },
                                onDelete = null
                            )
                        }
                    }
                }
            }

            // User Places Section
            if (uiState.userPlaces.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.my_safe_places),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                    ) {
                        items(uiState.userPlaces) { place ->
                            SafePlaceCard(
                                place = place,
                                isUserPlace = true,
                                onNavigate = { navigateToPlace(context, place) },
                                onDelete = {
                                    placeToDelete = place
                                    showDeleteConfirm = true
                                }
                            )
                        }
                    }
                }
            } else if (uiState.nearbyPlaces.isEmpty()) {
                // Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.no_safe_places_found),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.add_safe_places_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
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

    // Add Place Dialog
    if (showAddDialog) {
        AddSafePlaceDialog(
            name = dialogName,
            onNameChange = { dialogName = it },
            address = dialogAddress,
            onAddressChange = { dialogAddress = it },
            latitude = dialogLatitude,
            onLatitudeChange = { dialogLatitude = it },
            longitude = dialogLongitude,
            onLongitudeChange = { dialogLongitude = it },
            type = dialogType,
            onTypeChange = { dialogType = it },
            types = types,
            useCurrentLocation = useCurrentLocation,
            onUseCurrentLocationChange = { useCurrentLocation = it },
            onPickLocation = { },
            onConfirm = {
                if (dialogName.isNotBlank() && dialogLatitude != 0.0 && dialogLongitude != 0.0) {
                    viewModel.addPlace(dialogName, dialogAddress, dialogLatitude, dialogLongitude, dialogType)
                    showAddDialog = false
                }
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false; placeToDelete = null },
            title = { Text(stringResource(R.string.delete_confirmation_title)) },
            text = { Text(stringResource(R.string.delete_safe_place_confirmation)) },
            confirmButton = {
                Button(
                    onClick = {
                        placeToDelete?.let { viewModel.removePlace(it.id) }
                        showDeleteConfirm = false
                        placeToDelete = null
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
                Button(onClick = { showDeleteConfirm = false; placeToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun SafePlaceCard(
    place: SafePlace,
    isUserPlace: Boolean,
    onNavigate: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val colors = MaterialTheme.colorScheme
    val typeIcon = when (place.type) {
        "hospital" -> Icons.Default.LocalHospital
        "police" -> Icons.Default.LocalPolice
        "fire" -> Icons.Default.LocalFireDepartment
        else -> Icons.Default.Favorite
    }
    val typeColor = when (place.type) {
        "hospital" -> Color(0xFFE91E63)
        "police" -> Color(0xFF2196F3)
        "fire" -> Color(0xFFFF5722)
        else -> Color(0xFF009688)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isUserPlace) colors.primaryContainer.copy(alpha = 0.3f) else colors.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(typeColor.copy(alpha = 0.15f))
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = place.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface
                    )
                    if (place.address.isNotBlank()) {
                        Text(
                            text = place.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .background(typeColor.copy(alpha = 0.2f))
                                .clip(RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                text = place.type.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = typeColor,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                IconButton(onClick = onNavigate) {
                    Icon(Icons.Default.Directions, contentDescription = stringResource(R.string.navigate), tint = colors.primary)
                }
                onDelete?.let { deleteAction ->
                    IconButton(onClick = deleteAction) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = colors.error)
                    }
                }
            }
        }
    }
}

fun navigateToPlace(context: android.content.Context, place: SafePlace) {
    val uri = Uri.parse("geo:${place.latitude},${place.longitude}?q=${place.latitude},${place.longitude}(${Uri.encode(place.name)})")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    intent.setPackage("com.google.android.apps.maps")
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=${place.latitude},${place.longitude}"))
        context.startActivity(fallbackIntent)
    }
}

@Composable
fun AddSafePlaceDialog(
    name: String,
    onNameChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    latitude: Double,
    onLatitudeChange: (Double) -> Unit,
    longitude: Double,
    onLongitudeChange: (Double) -> Unit,
    type: String,
    onTypeChange: (String) -> Unit,
    types: List<Pair<String, androidx.compose.ui.graphics.vector.ImageVector>>,
    useCurrentLocation: Boolean,
    onUseCurrentLocationChange: (Boolean) -> Unit,
    onPickLocation: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_safe_place)) },
        text = {
            Column(
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.place_name)) },
                    placeholder = { Text(stringResource(R.string.enter_place_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                androidx.compose.material3.OutlinedTextField(
                    value = address,
                    onValueChange = onAddressChange,
                    label = { Text(stringResource(R.string.address_optional)) },
                    placeholder = { Text(stringResource(R.string.enter_address)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                // Type Dropdown
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.place_type),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.material3.ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        androidx.compose.material3.TextField(
                            value = types.first { it.first == type }.second.toString(),
                            onValueChange = { },
                            label = { Text(type.uppercase()) },
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
                        androidx.compose.material3.DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            types.forEach { (t, icon) ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(t.uppercase()) },
                                    onClick = {
                                        onTypeChange(t)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                // Coordinates
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = latitude.toString(),
                        onValueChange = { onLatitudeChange(it.toDoubleOrNull() ?: 0.0) },
                        label = { Text(stringResource(R.string.latitude)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = longitude.toString(),
                        onValueChange = { onLongitudeChange(it.toDoubleOrNull() ?: 0.0) },
                        label = { Text(stringResource(R.string.longitude)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                // Use Current Location
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.material3.Checkbox(
                        checked = useCurrentLocation,
                        onCheckedChange = onUseCurrentLocationChange
                    )
                    Text(stringResource(R.string.use_current_location))
                }
                Button(
                    onClick = onPickLocation,
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
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(20.dp).padding(end = 8.dp))
                        Text(stringResource(R.string.pick_on_map))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = name.isNotBlank()) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}