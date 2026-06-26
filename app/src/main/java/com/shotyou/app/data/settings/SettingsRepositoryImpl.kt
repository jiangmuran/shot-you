package com.shotyou.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shotyou.app.domain.model.AiSettings
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
        val API_BASE_URL = stringPreferencesKey("api_base_url")
        val API_KEY = stringPreferencesKey("api_key")
        val VLM_MODEL = stringPreferencesKey("vlm_model")
        val LLM_MODEL = stringPreferencesKey("llm_model")
        val IMAGE_MODEL = stringPreferencesKey("image_model")
        val VLM_BATCH_SIZE = intPreferencesKey("vlm_batch_size")
        val CANDIDATES_PER_ITEM = intPreferencesKey("candidates_per_item")
        val MAX_CONCURRENT = intPreferencesKey("max_concurrent")
        val MIN_INTERVAL = longPreferencesKey("min_interval_ms")
        val AUTO_RETRY = booleanPreferencesKey("auto_retry")
        val MAX_RETRIES = intPreferencesKey("max_retries")
        val REQUIRE_WIFI = booleanPreferencesKey("require_wifi")
        val RUN_IN_BACKGROUND = booleanPreferencesKey("run_in_background")
        val PROGRESS_NOTIFICATIONS = booleanPreferencesKey("progress_notifications")
        val AUTO_OPTIMIZE = booleanPreferencesKey("auto_optimize")
        val DEFAULT_STYLE = stringPreferencesKey("default_style")
        val DEFAULT_INTENSITY = intPreferencesKey("default_intensity")
        val PRICE_PER_IMAGE = doublePreferencesKey("price_per_image")
        val PRICE_PER_1K_INPUT = doublePreferencesKey("price_per_1k_input")
        val PRICE_PER_1K_OUTPUT = doublePreferencesKey("price_per_1k_output")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    override val settings: Flow<AiSettings> = context.dataStore.data.map { it.toSettings() }

    override suspend fun current(): AiSettings = settings.first()

    override suspend fun update(transform: (AiSettings) -> AiSettings) {
        context.dataStore.edit { prefs ->
            val s = transform(prefs.toSettings())
            prefs[Keys.API_BASE_URL] = s.apiBaseUrl
            prefs[Keys.API_KEY] = s.apiKey
            prefs[Keys.VLM_MODEL] = s.vlmModel
            prefs[Keys.LLM_MODEL] = s.llmModel
            prefs[Keys.IMAGE_MODEL] = s.imageModel
            prefs[Keys.VLM_BATCH_SIZE] = s.vlmBatchSize
            prefs[Keys.CANDIDATES_PER_ITEM] = s.candidatesPerItem
            prefs[Keys.MAX_CONCURRENT] = s.maxConcurrentJobs
            prefs[Keys.MIN_INTERVAL] = s.minRequestIntervalMs
            prefs[Keys.AUTO_RETRY] = s.autoRetryOnFailure
            prefs[Keys.MAX_RETRIES] = s.maxRetries
            prefs[Keys.REQUIRE_WIFI] = s.requireWifi
            prefs[Keys.RUN_IN_BACKGROUND] = s.runInBackground
            prefs[Keys.PROGRESS_NOTIFICATIONS] = s.progressNotifications
            prefs[Keys.AUTO_OPTIMIZE] = s.autoOptimizePrompt
            prefs[Keys.DEFAULT_STYLE] = s.defaultStyle
            prefs[Keys.DEFAULT_INTENSITY] = s.defaultIntensity
            prefs[Keys.PRICE_PER_IMAGE] = s.pricePerImage
            prefs[Keys.PRICE_PER_1K_INPUT] = s.pricePer1kInputTokens
            prefs[Keys.PRICE_PER_1K_OUTPUT] = s.pricePer1kOutputTokens
            prefs[Keys.CURRENCY_SYMBOL] = s.currencySymbol
            prefs[Keys.APP_LANGUAGE] = s.appLanguage
        }
    }

    private fun Preferences.toSettings(): AiSettings {
        val d = AiSettings()
        return AiSettings(
            apiBaseUrl = this[Keys.API_BASE_URL] ?: d.apiBaseUrl,
            apiKey = this[Keys.API_KEY] ?: d.apiKey,
            vlmModel = this[Keys.VLM_MODEL] ?: d.vlmModel,
            llmModel = this[Keys.LLM_MODEL] ?: d.llmModel,
            imageModel = this[Keys.IMAGE_MODEL] ?: d.imageModel,
            vlmBatchSize = this[Keys.VLM_BATCH_SIZE] ?: d.vlmBatchSize,
            candidatesPerItem = this[Keys.CANDIDATES_PER_ITEM] ?: d.candidatesPerItem,
            maxConcurrentJobs = this[Keys.MAX_CONCURRENT] ?: d.maxConcurrentJobs,
            minRequestIntervalMs = this[Keys.MIN_INTERVAL] ?: d.minRequestIntervalMs,
            autoRetryOnFailure = this[Keys.AUTO_RETRY] ?: d.autoRetryOnFailure,
            maxRetries = this[Keys.MAX_RETRIES] ?: d.maxRetries,
            requireWifi = this[Keys.REQUIRE_WIFI] ?: d.requireWifi,
            runInBackground = this[Keys.RUN_IN_BACKGROUND] ?: d.runInBackground,
            progressNotifications = this[Keys.PROGRESS_NOTIFICATIONS] ?: d.progressNotifications,
            autoOptimizePrompt = this[Keys.AUTO_OPTIMIZE] ?: d.autoOptimizePrompt,
            defaultStyle = this[Keys.DEFAULT_STYLE] ?: d.defaultStyle,
            defaultIntensity = this[Keys.DEFAULT_INTENSITY] ?: d.defaultIntensity,
            pricePerImage = this[Keys.PRICE_PER_IMAGE] ?: d.pricePerImage,
            pricePer1kInputTokens = this[Keys.PRICE_PER_1K_INPUT] ?: d.pricePer1kInputTokens,
            pricePer1kOutputTokens = this[Keys.PRICE_PER_1K_OUTPUT] ?: d.pricePer1kOutputTokens,
            currencySymbol = this[Keys.CURRENCY_SYMBOL] ?: d.currencySymbol,
            appLanguage = this[Keys.APP_LANGUAGE] ?: d.appLanguage,
        )
    }
}
