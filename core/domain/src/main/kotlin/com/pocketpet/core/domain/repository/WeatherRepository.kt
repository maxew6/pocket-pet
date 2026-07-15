package com.pocketpet.core.domain.repository

import com.pocketpet.core.model.WeatherSnapshot

/**
 * Fetches weather from a public, no-secret provider (Open-Meteo) and caches the last successful
 * reading. There is no synthetic fallback: a failed or offline fetch surfaces as a thrown
 * exception / null cache, never a made-up [WeatherSnapshot].
 */
interface WeatherRepository {
    /** Resolves a typed place name (e.g. "Lisbon") to coordinates. Returns `null` if not found. */
    suspend fun geocodeLocation(query: String): GeocodedPlace?

    /** Fetches fresh weather for the given coordinates. Throws on network/parse failure. */
    suspend fun fetchWeather(latitude: Double, longitude: Double, locationLabel: String): WeatherSnapshot

    /** The last successfully fetched snapshot, if any, regardless of how stale it is. */
    suspend fun cachedWeather(): WeatherSnapshot?
}

data class GeocodedPlace(
    val label: String,
    val latitude: Double,
    val longitude: Double,
)
