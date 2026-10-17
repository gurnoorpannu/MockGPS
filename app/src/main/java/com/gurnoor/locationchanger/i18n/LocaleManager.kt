package com.gurnoor.locationchanger.i18n

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/** Per-app language, switched instantly without a manual restart. */
object LocaleManager {
    const val EN = "en"
    const val PA = "pa"

    fun apply(tag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    /** The language in effect right now, defaulting to English. */
    fun current(): String {
        val tags = AppCompatDelegate.getApplicationLocales()
        if (tags.isEmpty) return EN
        return tags.toLanguageTags().substringBefore('-').ifEmpty { EN }
    }
}
