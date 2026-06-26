package com.shotyou.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shotyou.app.domain.model.AiProviderType
import com.shotyou.app.domain.model.AiSettings
import com.shotyou.app.domain.model.DefaultModels
import com.shotyou.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "shot_you_settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    private object Keys {
        val VLM_PROVIDER = stringPreferencesKey("vlm_provider")
        val LLM_PROVIDER = stringPreferencesKey("llm_provider")
        val IMAGE_PROVIDER = stringPreferencesKey("image_provider")
        val VLM_MODEL = stringPreferencesKey("vlm_model")
        val LLM_MODEL = stringPreferencesKey("llm_model")
        val IMAGE_MODEL = stringPreferencesKey("image_model")
        val GEMINI_KEY = stringPreferencesKey("gemini_key")
        val OPENAI_KEY = stringPreferencesKey("openai_key")
        val MAX_CONCURRENT = intPreferencesKey("max_concurrent")
        val MIN_INTERVAL = longPreferencesKey("min_interval_ms")
        val AUTO_RETRY = booleanPreferencesKey("auto_retry")
        val MAX_RETRIES = intPreferencesKey("max_retries")
        val AUTO_OPTIMIZE = booleanPreferencesKey("auto_optimize")
        val REQUIRE_WIFI = booleanPreferencesKey("require_wifi")
    }

    override val settings: Flow<AiSettings> = context.dataStore.data.map { it.toSettings() }

    override suspend fun current(): AiSettings = settings.first()

    override suspend fun update(transform: (AiSettings) -> AiSettings) {
        context.dataStore.edit { prefs ->
            val updated = transform(prefs.toSettings())
            prefs[Keys.VLM_PROVIDER] = updated.vlmProvider.name
            prefs[Keys.LLM_PROVIDER] = updated.llmProvider.name
            prefs[Keys.IMAGE_PROVIDER] = updated.imageProvider.name
            prefs[Keys.VLM_MODEL] = updated.vlmModel
            prefs[Keys.LLM_MODEL] = updated.llmModel
            prefs[Keys.IMAGE_MODEL] = updated.imageModel
            prefs[Keys.GEMINI_KEY] = updated.geminiKey
            prefs[Keys.OPENAI_KEY] = updated.openAiKey
            prefs[Keys.MAX_CONCURRENT] = updated.maxConcurrentJobs
            prefs[Keys.MIN_INTERVAL] = updated.minRequestIntervalMs
            prefs[Keys.AUTO_RETRY] = updated.autoRetryOnFailure
            prefs[Keys.MAX_RETRIES] = updated.maxRetries
            prefs[Keys.AUTO_OPTIMIZE] = updated.autoOptimizePrompt
            prefs[Keys.REQUIRE_WIFI] = updated.requireWifi
        }
    }

    private fun Preferences.toSettings(): AiSettings {
        val defaults = AiSettings()
        fun provider(key: Preferences.Key<String>, fallback: AiProviderType): AiProviderType =
            this[key]?.let { runCatching { AiProviderType.valueOf(it) }.getOrNull() } ?: fallback
        return AiSettings(
            vlmProvider = provider(Keys.VLM_PROVIDER, defaults.vlmProvider),
            llmProvider = provider(Keys.LLM_PROVIDER, defaults.llmProvider),
            imageProvider = provider(Keys.IMAGE_PROVIDER, defaults.imageProvider),
            vlmModel = this[Keys.VLM_MODEL] ?: DefaultModels.GEMINI_VLM,
            llmModel = this[Keys.LLM_MODEL] ?: DefaultModels.GEMINI_LLM,
            imageModel = this[Keys.IMAGE_MODEL] ?: DefaultModels.GEMINI_IMAGE,
            geminiKey = this[Keys.GEMINI_KEY] ?: "",
            openAiKey = this[Keys.OPENAI_KEY] ?: "",
            maxConcurrentJobs = this[Keys.MAX_CONCURRENT] ?: defaults.maxConcurrentJobs,
            minRequestIntervalMs = this[Keys.MIN_INTERVAL] ?: defaults.minRequestIntervalMs,
            autoRetryOnFailure = this[Keys.AUTO_RETRY] ?: defaults.autoRetryOnFailure,
            maxRetries = this[Keys.MAX_RETRIES] ?: defaults.maxRetries,
            autoOptimizePrompt = this[Keys.AUTO_OPTIMIZE] ?: defaults.autoOptimizePrompt,
            requireWifi = this[Keys.REQUIRE_WIFI] ?: defaults.requireWifi,
        )
    }
}
