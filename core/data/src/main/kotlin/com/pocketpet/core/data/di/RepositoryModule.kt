package com.pocketpet.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.pocketpet.core.data.remote.WeatherRepositoryImpl
import com.pocketpet.core.data.repository.PetMemoryRepositoryImpl
import com.pocketpet.core.data.repository.PetPreferencesRepositoryImpl
import com.pocketpet.core.data.repository.PetRepositoryImpl
import com.pocketpet.core.database.dao.HistoryEventDao
import com.pocketpet.core.database.dao.PetStateDao
import com.pocketpet.core.domain.provider.DispatcherProvider
import com.pocketpet.core.domain.repository.PetMemoryRepository
import com.pocketpet.core.domain.repository.PetPreferencesRepository
import com.pocketpet.core.domain.repository.PetRepository
import com.pocketpet.core.domain.repository.WeatherRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun providePetRepository(
        petStateDao: PetStateDao,
        dispatchers: DispatcherProvider,
    ): PetRepository = PetRepositoryImpl(petStateDao, dispatchers)

    @Provides
    @Singleton
    fun providePetPreferencesRepository(
        dataStore: DataStore<Preferences>,
    ): PetPreferencesRepository = PetPreferencesRepositoryImpl(dataStore)

    @Provides
    @Singleton
    fun providePetMemoryRepository(
        historyEventDao: HistoryEventDao,
        dispatchers: DispatcherProvider,
    ): PetMemoryRepository = PetMemoryRepositoryImpl(historyEventDao, dispatchers)

    @Provides
    @Singleton
    fun provideWeatherRepository(
        httpClient: OkHttpClient,
        json: Json,
        dataStore: DataStore<Preferences>,
        dispatchers: DispatcherProvider,
    ): WeatherRepository = WeatherRepositoryImpl(httpClient, json, dataStore, dispatchers)
}
