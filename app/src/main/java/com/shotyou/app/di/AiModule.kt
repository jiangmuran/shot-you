package com.shotyou.app.di

import com.shotyou.app.data.remote.ai.DefaultAiProviderFactory
import com.shotyou.app.domain.ai.AiProviderFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the AI provider factory. Bound to the OpenAI-compatible [DefaultAiProviderFactory],
 * which targets the user-configured API host (base URL).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindAiProviderFactory(impl: DefaultAiProviderFactory): AiProviderFactory
}
