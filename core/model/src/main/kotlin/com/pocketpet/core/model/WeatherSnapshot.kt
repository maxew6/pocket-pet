package com.pocketpet.core.model

/** Coarse weather condition buckets used to pick a speech line and a small weather glyph. */
enum class WeatherCondition {
    Clear,
    Cloudy,
    Rainy,
    Snowy,
    Stormy,
    Foggy,
    Unknown,
}

/**
 * A weather reading. Always produced by an actual network fetch (see `WeatherRepository`) —
 * there is no synthetic/fallback data path that fabricates a condition or temperature.
 */
data class WeatherSnapshot(
    val locationLabel: String,
    val temperatureCelsius: Double,
    val condition: WeatherCondition,
    val fetchedAtEpochMillis: Long,
)

/** The weather feature's current UI state: no request yet, loading, a real reading, or an error. */
sealed class WeatherUiState {
    data object NotRequested : WeatherUiState()
    data object Loading : WeatherUiState()
    data class Loaded(val snapshot: WeatherSnapshot) : WeatherUiState()
    data class Unavailable(val reason: String, val cachedSnapshot: WeatherSnapshot? = null) : WeatherUiState()
}
