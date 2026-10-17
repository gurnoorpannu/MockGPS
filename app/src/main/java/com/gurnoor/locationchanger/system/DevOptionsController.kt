package com.gurnoor.locationchanger.system

import android.provider.Settings

/**
 * Reads and writes Settings.Global.DEVELOPMENT_SETTINGS_ENABLED. ADB_ENABLED (USB debugging) is
 * touched only by the explicit in-app toggle (setEnabled), never automatically. The goal: the father's phone must never be left with Developer options on
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

    /** What banking apps see: Developer options OR USB debugging switched on. */
    fun isVisibleToOtherApps(): Boolean = isEnabled() || gateway.getInt(ADB_KEY, 0) == 1

    /**
     * Manual switch from the in-app toggle. Flips Developer options AND USB debugging together,
     * because banking apps (e.g. SBI YONO) also flag USB debugging. Writing the settings directly
     * does NOT reset the mock-location permission (verified on a Galaxy S24) — only the
     * Settings-app switch does. Turning it off disconnects adb.
     */
    fun setEnabled(on: Boolean) {
        val v = if (on) 1 else 0
        gateway.putInt(KEY, v)
        gateway.putInt(ADB_KEY, v)
    }

    /** Turns Developer options off, but only when the app is the one that turned it on. */
    fun disableIfWeEnabled(weEnabled: Boolean) {
        if (weEnabled) gateway.putInt(KEY, 0)
    }

    companion object {
        // Compile-time String constant, so this is safe to reference in plain JVM unit tests.
        const val KEY = Settings.Global.DEVELOPMENT_SETTINGS_ENABLED
        const val ADB_KEY = Settings.Global.ADB_ENABLED
    }
}
