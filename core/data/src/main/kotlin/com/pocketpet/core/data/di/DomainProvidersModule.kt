package com.pocketpet.core.data.di

import com.pocketpet.core.domain.behavior.PetBehaviorEngine
import com.pocketpet.core.domain.provider.ClockProvider
import com.pocketpet.core.domain.provider.DefaultRandomProvider
import com.pocketpet.core.domain.provider.RandomProvider
import com.pocketpet.core.domain.provider.SystemClockProvider
import com.pocketpet.core.domain.voice.VoiceCommandParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the pure, framework-agnostic `core:domain` objects. Kept deliberately separate from
 * [SystemModule]: everything here has zero Android dependency, which is exactly what makes
 * [PetBehaviorEngine] unit-testable without Robolectric — see `core:domain`'s test sources.
 */
@Module
@InstallIn(SingletonComponent::class)
object DomainProvidersModule {

    @Provides
    @Singleton
    fun provideClockProvider(): ClockProvider = SystemClockProvider()

    @Provides
    @Singleton
    fun provideRandomProvider(): RandomProvider = DefaultRandomProvider()

    @Provides
    @Singleton
    fun provideVoiceCommandParser(): VoiceCommandParser = VoiceCommandParser()

    @Provides
    @Singleton
    fun providePetBehaviorEngine(clock: ClockProvider, random: RandomProvider): PetBehaviorEngine =
        PetBehaviorEngine(clock, random)
}
