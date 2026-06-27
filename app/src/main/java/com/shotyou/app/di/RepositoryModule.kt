package com.shotyou.app.di

import com.shotyou.app.data.repository.GenerationRepositoryImpl
import com.shotyou.app.data.repository.GroupingRepositoryImpl
import com.shotyou.app.data.repository.PhotoRepositoryImpl
import com.shotyou.app.data.repository.SessionRepositoryImpl
import com.shotyou.app.data.repository.TemplateRepositoryImpl
import com.shotyou.app.data.repository.UsageRepositoryImpl
import com.shotyou.app.data.settings.SettingsRepositoryImpl
import com.shotyou.app.domain.repository.GenerationRepository
import com.shotyou.app.domain.repository.GroupingRepository
import com.shotyou.app.domain.repository.PhotoRepository
import com.shotyou.app.domain.repository.SessionRepository
import com.shotyou.app.domain.repository.SettingsRepository
import com.shotyou.app.domain.repository.TemplateRepository
import com.shotyou.app.domain.repository.UsageRepository
import com.shotyou.app.util.Clock
import com.shotyou.app.util.IdGenerator
import com.shotyou.app.util.SystemClock
import com.shotyou.app.util.UuidGenerator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPhotoRepository(impl: PhotoRepositoryImpl): PhotoRepository

    @Binds
    @Singleton
    abstract fun bindGroupingRepository(impl: GroupingRepositoryImpl): GroupingRepository

    @Binds
    @Singleton
    abstract fun bindTemplateRepository(impl: TemplateRepositoryImpl): TemplateRepository

    @Binds
    @Singleton
    abstract fun bindGenerationRepository(impl: GenerationRepositoryImpl): GenerationRepository

    @Binds
    @Singleton
    abstract fun bindUsageRepository(impl: UsageRepositoryImpl): UsageRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindClock(impl: SystemClock): Clock

    @Binds
    @Singleton
    abstract fun bindIdGenerator(impl: UuidGenerator): IdGenerator
}
