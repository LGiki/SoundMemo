package net.lgiki.soundmemo

import android.app.Application
import android.content.res.Configuration
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.lgiki.soundmemo.util.resolveLocale

class SoundMemoApplication : Application() {
    val container by lazy { SoundMemoContainer(this) }

    var currentLocale: String = "system"
        private set

    override fun onCreate() {
        super.onCreate()
        currentLocale = runBlocking {
            container.settingsRepository.settings.first().locale
        }
        applyLocale(currentLocale)
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
