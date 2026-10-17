package com.gurnoor.locationchanger.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gurnoor.locationchanger.data.SettingsRepository
import com.gurnoor.locationchanger.system.AndroidGlobalSettingsGateway
import com.gurnoor.locationchanger.system.DevOptionsController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Safety net after a reboot. Mocking never survives a restart, so: clear the active flag, and if
 * the app had turned Developer options on, turn it back off. The phone must never be left with
 * Developer options on because the app crashed or the phone restarted.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val appContext = context.applicationContext
        val settings = SettingsRepository(appContext)
        val devOptions = DevOptionsController(AndroidGlobalSettingsGateway(appContext))

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snap = settings.current()
                settings.setActive(false)
                if (snap.weEnabledDevOptions) {
                    runCatching { devOptions.disableIfWeEnabled(true) }
                    settings.setWeEnabledDevOptions(false)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
