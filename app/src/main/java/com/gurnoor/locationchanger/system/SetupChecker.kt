package com.gurnoor.locationchanger.system

data class SetupStatus(
    val writeSecureSettings: Boolean,
    val mockLocationAllowed: Boolean,
    val locationPermission: Boolean,
) {
    /** The one-time ADB setup (done by the son). Gates the "Setup needed" screen. */
    val adbSetupComplete: Boolean get() = writeSecureSettings && mockLocationAllowed

    /** Everything needed before the location can actually be changed. */
    val readyToMock: Boolean get() = adbSetupComplete && locationPermission
}

class SetupChecker(private val env: SetupEnvironment) {
    fun check(): SetupStatus = SetupStatus(
        writeSecureSettings = env.hasWriteSecureSettings(),
        mockLocationAllowed = env.isMockLocationAllowed(),
        locationPermission = env.hasLocationPermission(),
    )
}
