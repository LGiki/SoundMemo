package net.lgiki.soundmemo.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import java.util.Locale

const val SYSTEM_LOCALE_TAG = "system"

fun resolveLocale(tag: String): Locale {
    return if (tag == SYSTEM_LOCALE_TAG) {
        val config = Resources.getSystem().configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.locales[0]
        } else {
            @Suppress("DEPRECATION")
            config.locale
        }
    } else {
        Locale.forLanguageTag(tag)
    }
}

fun Context.wrapWithLocale(localeTag: String): Context {
    val locale = resolveLocale(localeTag)
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    return createConfigurationContext(config)
}
