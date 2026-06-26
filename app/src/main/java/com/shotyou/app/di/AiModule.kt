package com.shotyou.app.di

import com.shotyou.app.data.remote.ai.PlaceholderAiProviderFactory
import com.shotyou.app.domain.ai.AiProviderFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the AI provider factory. P2 (networking agent) swaps the bound implementation
 * to the real Gemini/OpenAI factory — the binding target is the only line that changes.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindAiProviderFactory(impl: PlaceholderAiProviderFactory): AiProviderFactory
}
