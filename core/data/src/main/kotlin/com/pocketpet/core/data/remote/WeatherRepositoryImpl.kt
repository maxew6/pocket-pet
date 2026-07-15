package com.pocketpet.core.data.remote

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pocketpet.core.data.repository.toEnumOrDefault
import com.pocketpet.core.domain.provider.DispatcherProvider
import com.pocketpet.core.domain.repository.GeocodedPlace
import com.pocketpet.core.domain.repository.WeatherRepository
import com.pocketpet.core.model.WeatherCondition
import com.pocketpet.core.model.WeatherSnapshot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

private object WeatherCacheKeys {
    val LOCATION = stringPreferencesKey("weather_cache_location")
    val TEMPERATURE = floatPreferencesKey("weather_cache_temperature")
    val CONDITION = stringPreferencesKey("weather_cache_condition")
    val FETCHED_AT = longPreferencesKey("weather_cache_fetched_at")
}

@Serializable
private data class GeocodingResponse(val results: List<GeocodingResult>? = null)

@Serializable
private data class GeocodingResult(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
)

@Serializable
private data class ForecastResponse(val current: CurrentWeatherPayload? = null)

@Serializable
private data class CurrentWeatherPayload(
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("weather_code") val weatherCode: Int,
)

/**
 * Talks to Open-Meteo's free geocoding + forecast APIs — no API key, no secret, subject to their
 * documented terms. Every reading returned is a real fetch; failures throw rather than making up
 * a plausible-looking temperature.
 */
class WeatherRepositoryImpl(
    private val httpClient: OkHttpClient,
    private val json: Json,
    private val dataStore: DataStore<Preferences>,
    private val dispatchers: DispatcherProvider,
) : WeatherRepository {

    override suspend fun geocodeLocation(query: String): GeocodedPlace? = withContext(dispatchers.io) {
        val url = "https://geocoding-api.open-meteo.com/v1/search".toHttpUrl().newBuilder()
            .addQueryParameter("name", query)
            .addQueryParameter("count", "1")
            .addQueryParameter("language", "en")
            .addQueryParameter("format", "json")
            .build()
        httpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string() ?: return@withContext null
            val parsed = json.decodeFromString<GeocodingResponse>(body)
            val first = parsed.results?.firstOrNull() ?: return@withContext null
            val label = if (first.country != null) "${first.name}, ${first.country}" else first.name
            GeocodedPlace(label = label, latitude = first.latitude, longitude = first.longitude)
        }
    }

    override suspend fun fetchWeather(latitude: Double, longitude: Double, locationLabel: String): WeatherSnapshot =
        withContext(dispatchers.io) {
            val url = "https://api.open-meteo.com/v1/forecast".toHttpUrl().newBuilder()
                .addQueryParameter("latitude", latitude.toString())
                .addQueryParameter("longitude", longitude.toString())
                .addQueryParameter("current", "temperature_2m,weather_code")
                .addQueryParameter("timezone", "auto")
                .build()
            httpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Weather request failed (HTTP ${response.code}).")
                val body = response.body?.string() ?: throw IOException("Empty weather response.")
                val current = json.decodeFromString<ForecastResponse>(body).current
                    ?: throw IOException("Weather response had no current reading.")
                val snapshot = WeatherSnapshot(
                    locationLabel = locationLabel,
                    temperatureCelsius = current.temperature,
                    condition = current.weatherCode.toWeatherCondition(),
                    fetchedAtEpochMillis = System.currentTimeMillis(),
                )
                cache(snapshot)
                snapshot
            }
        }

    override suspend fun cachedWeather(): WeatherSnapshot? = withContext(dispatchers.io) {
        val prefs = dataStore.data.first()
        val label = prefs[WeatherCacheKeys.LOCATION]
        val temperature = prefs[WeatherCacheKeys.TEMPERATURE]
        val fetchedAt = prefs[WeatherCacheKeys.FETCHED_AT]
        if (label == null || temperature == null || fetchedAt == null) return@withContext null
        WeatherSnapshot(
            locationLabel = label,
            temperatureCelsius = temperature.toDouble(),
            condition = prefs[WeatherCacheKeys.CONDITION]?.toEnumOrDefault(WeatherCondition.Unknown)
                ?: WeatherCondition.Unknown,
            fetchedAtEpochMillis = fetchedAt,
        )
    }

    private suspend fun cache(snapshot: WeatherSnapshot) {
        dataStore.edit { prefs ->
            prefs[WeatherCacheKeys.LOCATION] = snapshot.locationLabel
            prefs[WeatherCacheKeys.TEMPERATURE] = snapshot.temperatureCelsius.toFloat()
            prefs[WeatherCacheKeys.CONDITION] = snapshot.condition.name
            prefs[WeatherCacheKeys.FETCHED_AT] = snapshot.fetchedAtEpochMillis
        }
    }

    /** Maps WMO weather codes (used by Open-Meteo) to our coarse [WeatherCondition] buckets. */
    private fun Int.toWeatherCondition(): WeatherCondition = when (this) {
        0 -> WeatherCondition.Clear
        1, 2, 3 -> WeatherCondition.Cloudy
        45, 48 -> WeatherCondition.Foggy
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> WeatherCondition.Rainy
        71, 73, 75, 77, 85, 86 -> WeatherCondition.Snowy
        95, 96, 99 -> WeatherCondition.Stormy
        else -> WeatherCondition.Unknown
    }
}
