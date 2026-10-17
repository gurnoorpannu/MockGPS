package com.gurnoor.locationchanger.system

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Process
import android.provider.Settings

/** Thin seam over Settings.Global so DevOptionsController can be unit-tested with a fake. */
interface GlobalSettingsGateway {
    fun getInt(name: String, default: Int): Int
    fun putInt(name: String, value: Int)
}

/** What the setup screen needs to know. A seam so SetupChecker is unit-testable. */
interface SetupEnvironment {
    fun hasWriteSecureSettings(): Boolean
    fun isMockLocationAllowed(): Boolean
    fun hasLocationPermission(): Boolean
}

class AndroidGlobalSettingsGateway(context: Context) : GlobalSettingsGateway {
    private val resolver = context.applicationContext.contentResolver
    override fun getInt(name: String, default: Int): Int =
        Settings.Global.getInt(resolver, name, default)

    // Requires WRITE_SECURE_SETTINGS (granted once via ADB).
    override fun putInt(name: String, value: Int) {
        Settings.Global.putInt(resolver, name, value)
    }
}

class AndroidSetupEnvironment(context: Context) : SetupEnvironment {
    private val app = context.applicationContext

    override fun hasWriteSecureSettings(): Boolean =
        app.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    override fun isMockLocationAllowed(): Boolean {
        val appOps = app.getSystemService(AppOpsManager::class.java) ?: return false
        val uid = Process.myUid()
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_MOCK_LOCATION, uid, app.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_MOCK_LOCATION, uid, app.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun hasLocationPermission(): Boolean =
        app.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
}

/** Is the phone's location toggle on? Mock location is only delivered to apps when it is. */
fun isLocationServiceEnabled(context: Context): Boolean {
    val lm = context.getSystemService(LocationManager::class.java) ?: return false
    return lm.isLocationEnabled
}
