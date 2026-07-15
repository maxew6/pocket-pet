package com.pocketpet.core.domain.usecase

import com.pocketpet.core.domain.repository.WeatherRepository
import com.pocketpet.core.model.WeatherUiState
import kotlinx.coroutines.CancellationException

class FetchWeatherUseCase(
    private val weatherRepository: WeatherRepository,
) {
    /** Fetches weather for known coordinates (typically from the device-location path). */
    suspend operator fun invoke(latitude: Double, longitude: Double, locationLabel: String): WeatherUiState =
        runCatchingWeather { weatherRepository.fetchWeather(latitude, longitude, locationLabel) }

    /** Resolves a typed place name first, then fetches weather for it — the manual-entry path. */
    suspend fun forCityName(query: String): WeatherUiState = runCatchingWeather {
        val place = weatherRepository.geocodeLocation(query)
            ?: throw NoSuchElementException("Couldn't find \"$query\".")
        weatherRepository.fetchWeather(place.latitude, place.longitude, place.label)
    }

    private suspend fun runCatchingWeather(block: suspend () -> com.pocketpet.core.model.WeatherSnapshot): WeatherUiState =
        try {
            WeatherUiState.Loaded(block())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            WeatherUiState.Unavailable(
                reason = error.message ?: "Weather is unavailable right now.",
                cachedSnapshot = weatherRepository.cachedWeather(),
            )
        }
}
