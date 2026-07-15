package com.pocketpet.service.overlay

import android.content.Context
import com.pocketpet.core.domain.action.OverlayController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OverlayModule {

    @Provides
    @Singleton
    fun provideOverlayController(@ApplicationContext context: Context): OverlayController =
        OverlayControllerImpl(context)
}
