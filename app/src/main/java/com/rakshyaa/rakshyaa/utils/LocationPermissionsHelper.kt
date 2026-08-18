package com.rakshyaa.rakshyaa.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.rakshyaa.rakshyaa.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for managing location permissions at runtime
 */
@Singleton
class LocationPermissionsHelper @Inject constructor(
    private val context: Context
) {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

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
     * Request location permissions from the user
     */
    fun requestLocationPermissions(activity: Activity,
                                   onPermissionsGranted: () -> Unit,
                                   onPermissionsDenied: () -> Unit) {
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
            // Request the permissions
            ActivityCompat.requestPermissions(
                activity,
                permissionsToRequest.toTypedArray(),
                LOCATION_PERMISSION_REQUEST_CODE
            )

            // Store the callbacks for use in onRequestPermissionsResult
            // In a real implementation, you would use the Activity Result API
            #TODO: Implement proper callback handling with Activity Result API
        }
    }

    /**
     * Handle the result of a location permission request
     * This should be called from the Activity's onRequestPermissionsResult method
     */
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onPermissionsGranted: () -> Unit,
        onPermissionsDenied: () -> Unit
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            // Check if all requested permissions were granted
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (allGranted) {
                onPermissionsGranted()
            } else {
                // Check if any permission was permanently denied
                val shouldShowRationale = permissions.mapIndexed { index, permission ->
                    index to grantResults[index]
                }.filter { (_, result) -> result == PackageManager.PERMISSION_DENIED }
                    .any { (permission, _) ->
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            (context as Activity), permission
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
                    onPermissionsDenied()
                }
            }
        }
    }

    /**
     * Show explanation for why location permissions are needed
     */
    private fun showPermissionRationaleExplanation(
        onGoToSettings: () -> Unit,
        onCancel: () -> Unit
    ) {
        #TODO: Implement a dialog explaining why location permissions are needed
        #For now, just call the callbacks directly
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