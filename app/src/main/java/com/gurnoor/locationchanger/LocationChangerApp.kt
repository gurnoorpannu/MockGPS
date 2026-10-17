package com.gurnoor.locationchanger

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.gurnoor.locationchanger.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LocationChangerApp : Application() {
    val settings: SettingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        // Re-apply the saved language before any UI shows, so the first frame is correct.
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        scope.launch {
            settings.current().language?.let { tag ->
                androidx.core.os.LocaleListCompat.forLanguageTags(tag).let {
                    AppCompatDelegate.setApplicationLocales(it)
                }
            }
        }
    }
}
