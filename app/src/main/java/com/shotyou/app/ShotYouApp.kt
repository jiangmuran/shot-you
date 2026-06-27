package com.shotyou.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.shotyou.app.domain.repository.SettingsRepository
import com.shotyou.app.domain.repository.TemplateRepository
import com.shotyou.app.util.RootKeepAlive
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ShotYouApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var templateRepository: TemplateRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        appScope.launch { runCatching { templateRepository.ensureSeeded() } }
        appScope.launch {
            runCatching {
                if (settingsRepository.current().rootKeepAlive) RootKeepAlive.apply(packageName)
            }
        }
    }
}
