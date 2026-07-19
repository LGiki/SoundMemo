package net.lgiki.soundmemo

import android.app.Application
import android.content.res.Configuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.lgiki.soundmemo.data.storage.resolvedPendingPublications
import net.lgiki.soundmemo.util.resolveLocale

class SoundMemoApplication : Application() {
    val container by lazy { SoundMemoContainer(this) }
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    var currentLocale: String = "system"
        private set

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            updateLocale(container.settingsRepository.settings.first().locale)
        }
        applicationScope.launch(Dispatchers.IO) {
            container.recordingPublicationGate.withLock {
                val pending = container.recordingStorage.pendingPublications()
                val resolved = resolvedPendingPublications(
                    pending = pending,
                    hasSaveResult = container.recordingRepository::hasSaveResult,
                    deletePublishedRecording = container.recordingStorage::deletePublishedRecording,
                )
                container.recordingStorage.removePendingPublications(resolved)
            }
        }
    }

    fun updateLocale(tag: String) {
        currentLocale = tag
        applyLocale(tag)
    }

    @Suppress("DEPRECATION")
    private fun applyLocale(localeTag: String) {
        val locale = resolveLocale(localeTag)
        java.util.Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
