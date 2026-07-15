package com.pocketpet.core.system.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import com.pocketpet.core.system.permission.PermissionChecker

data class DeviceLocation(val latitude: Double, val longitude: Double)

/**
 * A minimal last-known-location lookup for the optional "use my location" weather toggle. Uses
 * the platform [LocationManager] directly rather than Play Services' fused location client, to
 * avoid pulling in a Play Services dependency for a single coarse reading. Returns `null` (never
 * a guess) whenever permission is missing or no provider has a recent fix.
 */
class DeviceLocationProvider(
    private val appContext: Context,
    private val permissionChecker: PermissionChecker,
) {
    @SuppressLint("MissingPermission")
    fun lastKnownLocation(): DeviceLocation? {
        if (!permissionChecker.hasCoarseLocationPermission()) return null
        val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
        return providers
            .filter { runCatching { locationManager.isProviderEnabled(it) }.getOrDefault(false) }
            .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
            ?.let { DeviceLocation(it.latitude, it.longitude) }
    }
}
