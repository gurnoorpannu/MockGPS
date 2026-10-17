package com.gurnoor.locationchanger.system

import android.provider.Settings

/**
 * Reads and writes only Settings.Global.DEVELOPMENT_SETTINGS_ENABLED. Never touches ADB_ENABLED
 * or anything else. The goal: the father's phone must never be left with Developer options on
 * if the app turned it on, because his banking/UPI apps complain about it.
 */
class DevOptionsController(private val gateway: GlobalSettingsGateway) {

    fun isEnabled(): Boolean = gateway.getInt(KEY, 0) == 1

    /**
     * Turns Developer options on. Returns true only if this call actually changed it
     * (it was off before). A false return means it was already on — so we must NOT turn it
     * off later, because the user had it on themselves.
     */
    fun enable(): Boolean {
        if (isEnabled()) return false
        gateway.putInt(KEY, 1)
        return true
    }

    /** Turns Developer options off, but only when the app is the one that turned it on. */
    fun disableIfWeEnabled(weEnabled: Boolean) {
        if (weEnabled) gateway.putInt(KEY, 0)
    }

    companion object {
        // Compile-time String constant, so this is safe to reference in plain JVM unit tests.
        const val KEY = Settings.Global.DEVELOPMENT_SETTINGS_ENABLED
    }
}
