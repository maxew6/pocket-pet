package com.pocketpet.core.data.di

import com.pocketpet.core.domain.action.InstalledAppResolver
import com.pocketpet.core.domain.action.PetActionExecutor
import com.pocketpet.core.domain.behavior.PetBehaviorEngine
import com.pocketpet.core.domain.provider.ClockProvider
import com.pocketpet.core.domain.repository.PetMemoryRepository
import com.pocketpet.core.domain.repository.PetPreferencesRepository
import com.pocketpet.core.domain.repository.PetRepository
import com.pocketpet.core.domain.repository.SystemStatusRepository
import com.pocketpet.core.domain.repository.WeatherRepository
import com.pocketpet.core.domain.usecase.ExecuteSystemActionUseCase
import com.pocketpet.core.domain.usecase.FetchWeatherUseCase
import com.pocketpet.core.domain.usecase.ParseAndExecuteVoiceCommandUseCase
import com.pocketpet.core.domain.usecase.ProcessBehaviorTickUseCase
import com.pocketpet.core.domain.usecase.RestorePetStateUseCase
import com.pocketpet.core.domain.voice.VoiceCommandParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideProcessBehaviorTickUseCase(
        engine: PetBehaviorEngine,
        petRepository: PetRepository,
        preferencesRepository: PetPreferencesRepository,
        systemStatusRepository: SystemStatusRepository,
        memoryRepository: PetMemoryRepository,
        clock: ClockProvider,
    ): ProcessBehaviorTickUseCase = ProcessBehaviorTickUseCase(
        engine, petRepository, preferencesRepository, systemStatusRepository, memoryRepository, clock,
    )

    @Provides
    @Singleton
    fun provideRestorePetStateUseCase(
        engine: PetBehaviorEngine,
        petRepository: PetRepository,
        memoryRepository: PetMemoryRepository,
    ): RestorePetStateUseCase = RestorePetStateUseCase(engine, petRepository, memoryRepository)

    @Provides
    @Singleton
    fun provideExecuteSystemActionUseCase(executor: PetActionExecutor): ExecuteSystemActionUseCase =
        ExecuteSystemActionUseCase(executor)

    @Provides
    @Singleton
    fun provideParseAndExecuteVoiceCommandUseCase(
        parser: VoiceCommandParser,
        executor: PetActionExecutor,
        appResolver: InstalledAppResolver,
    ): ParseAndExecuteVoiceCommandUseCase = ParseAndExecuteVoiceCommandUseCase(parser, executor, appResolver)

    @Provides
    @Singleton
    fun provideFetchWeatherUseCase(weatherRepository: WeatherRepository): FetchWeatherUseCase =
        FetchWeatherUseCase(weatherRepository)
}
