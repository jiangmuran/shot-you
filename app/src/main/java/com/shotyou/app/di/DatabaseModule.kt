package com.shotyou.app.di

import android.content.Context
import androidx.room.Room
import com.shotyou.app.data.local.GenerationJobDao
import com.shotyou.app.data.local.ShotYouDatabase
import com.shotyou.app.data.local.TemplateDao
import com.shotyou.app.data.local.UsageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ShotYouDatabase =
        Room.databaseBuilder(context, ShotYouDatabase::class.java, ShotYouDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideTemplateDao(db: ShotYouDatabase): TemplateDao = db.templateDao()

    @Provides
    fun provideGenerationJobDao(db: ShotYouDatabase): GenerationJobDao = db.generationJobDao()

    @Provides
    fun provideUsageDao(db: ShotYouDatabase): UsageDao = db.usageDao()
}
