package com.pocketpet.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.pocketpet.core.data.repository.petPreferencesDataStore
import com.pocketpet.core.database.PocketPetDatabase
import com.pocketpet.core.database.dao.HistoryEventDao
import com.pocketpet.core.database.dao.PetStateDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun providePocketPetDatabase(@ApplicationContext context: Context): PocketPetDatabase =
        Room.databaseBuilder(context, PocketPetDatabase::class.java, PocketPetDatabase.DATABASE_NAME)
            .addMigrations(*PocketPetDatabase.DATABASE_MIGRATIONS)
            .build()

    @Provides
    fun providePetStateDao(database: PocketPetDatabase): PetStateDao = database.petStateDao()

    @Provides
    fun provideHistoryEventDao(database: PocketPetDatabase): HistoryEventDao = database.historyEventDao()

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.petPreferencesDataStore

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }
}
