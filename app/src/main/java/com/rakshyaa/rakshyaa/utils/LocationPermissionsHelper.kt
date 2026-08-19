package com.rakshyaa.rakshyaa.utils

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.rakshyaa.rakshyaa.R
import dagger.hilt.android.androidContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for managing location permissions at runtime using Activity Result API
 */
@Singleton
class LocationPermissionsHelper @Inject constructor(
    @androidContext private val context: Context
) {

    // Activity Result API for requesting permissions
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // Check if all requested permissions were granted
            val allGranted = permissions.all { (_, granted) -> granted }

            if (allGranted) {
                onPermissionsGranted?.invoke()
            } else {
                // Check if any permission was permanently denied
                val shouldShowRationale = permissions.any { (permission, granted) ->
                    !granted &&
                    context is Activity &&
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        context as Activity,
                        permission
                    )
                }

                if (!shouldShowRationale) {
                    // User has denied permissions and checked "Don't ask again"
                    showPermissionRationaleExplanation(
                        onGoToSettings = { /* Open app settings */ },
                        onCancel = onPermissionsDenied
                    )
                } else {
                    // User denied permissions but might grant if asked again with explanation
                    onPermissionsDenied?.invoke()
                }
            }
        }

    // Callbacks for permission results
    private var onPermissionsGranted: (() -> Unit)? = null
    private var onPermissionsDenied: (() -> Unit)? = null

    /**
     * Check if all required location permissions are granted
     */
    fun areLocationPermissionsGranted(): Boolean {
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val backgroundLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )

        return fineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                coarseLocationPermission == PackageManager.PERMISSION_GRANTED &&
                backgroundLocationPermission == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request location permissions from the user using Activity Result API
     */
    fun requestLocationPermissions(
        onPermissionsGranted: () -> Unit,
        onPermissionsDenied: () -> Unit
    ) {
        // Store callbacks for use in the activity result handler
        this.onPermissionsGranted = onPermissionsGranted
        this.onPermissionsDenied = onPermissionsDenied

        val permissionsToRequest = mutableListOf<String>()

        // Check each permission and add to request list if not granted
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        // Note: ACCESS_BACKGROUND_LOCATION is only needed for Android 10+ (API 29+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }

        if (permissionsToRequest.isEmpty()) {
            // All permissions already granted
            onPermissionsGranted()
        } else {
            // Request the permissions using Activity Result API
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    /**
     * Show explanation for why location permissions are needed
     */
    private fun showPermissionRationaleExplanation(
        onGoToSettings: () -> Unit,
        onCancel: () -> Unit
    ) {
        // If we have an Activity context, show a Toast with explanation
        if (context is Activity) {
            val activity = context as Activity
            Toast.makeText(
                activity,
                "Location permissions are needed for core safety features. Please grant them in Settings.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            // Log a warning if we don't have an Activity context
            android.util.Log.w("LocationPermissionsHelper", "Cannot show permission rationale: no Activity context available")
        }
        // Proceed with the cancel action (user will need to go to settings manually)
        onCancel()
    }

    /**
     * Check if the app has permission to access location in the background
     */
    fun hasBackgroundLocationPermission(): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            // Background location permission is only required for Android 10+
            return true
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get a rationale string for why location permissions are needed
     */
    fun getLocationPermissionRationale(): String {
        return context.getString(R.string.location_permission_rationale)
    }

    /**
     * Get a rationale string for why background location permissions are needed
     */
    fun getBackgroundLocationPermissionRationale(): String {
        return context.getString(R.string.background_location_permission_rationale)
    }
}