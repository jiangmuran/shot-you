package com.shotyou.app.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Makes [WorkManager] injectable. WorkManager is initialised manually via Hilt
 * ([com.shotyou.app.ShotYouApp] is a [androidx.work.Configuration.Provider]), so by the
 * time anything resolves this dependency the singleton instance is already configured.
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkManagerModule {

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)
}
