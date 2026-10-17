package com.gurnoor.locationchanger.system

import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import com.google.android.gms.location.LocationServices
import kotlin.random.Random

/**
 * Pushes a fake location through every channel apps read from:
 *  - LocationManager test providers (GPS, NETWORK, and FUSED on API 31+)
 *  - FusedLocationProviderClient mock mode (what most modern apps actually use)
 *
 * Requires the app to be the selected mock-location app (the ADB appops step). start() lets a
 * SecurityException propagate so the caller can decide whether to retry via Developer options.
 */
class MockLocationController(context: Context) {
    private val app = context.applicationContext
    private val lm = app.getSystemService(LocationManager::class.java)
    private val fused = LocationServices.getFusedLocationProviderClient(app)

    private val providers: List<String> = buildList {
        add(LocationManager.GPS_PROVIDER)
        add(LocationManager.NETWORK_PROVIDER)
        if (Build.VERSION.SDK_INT >= 31) add(LocationManager.FUSED_PROVIDER)
    }

    private var started = false

    /** @throws SecurityException when the app is not allowed to mock. */
    fun start() {
        // GPS + NETWORK are the signal: if we aren't the mock app, these throw SecurityException.
        addProvider(LocationManager.GPS_PROVIDER)
        addProvider(LocationManager.NETWORK_PROVIDER)
        // FUSED test provider is not supported on every device — never let it abort the start.
        if (Build.VERSION.SDK_INT >= 31) runCatching { addProvider(LocationManager.FUSED_PROVIDER) }
        runCatching { fused.setMockMode(true) }
        started = true
    }

    private fun addProvider(provider: String) {
        runCatching { lm.removeTestProvider(provider) }
        @Suppress("DEPRECATION")
        lm.addTestProvider(
            provider,
            /* requiresNetwork = */ false,
            /* requiresSatellite = */ false,
            /* requiresCell = */ false,
            /* hasMonetaryCost = */ false,
            /* supportsAltitude = */ true,
            /* supportsSpeed = */ true,
            /* supportsBearing = */ true,
            Criteria.POWER_LOW,
            Criteria.ACCURACY_FINE,
        )
        lm.setTestProviderEnabled(provider, true)
    }

    fun push(lat: Double, lon: Double) {
        if (!started) return
        val now = System.currentTimeMillis()
        providers.forEach { p ->
            runCatching { lm.setTestProviderLocation(p, buildLocation(p, lat, lon, now)) }
        }
        runCatching {
            fused.setMockLocation(buildLocation(LocationManager.GPS_PROVIDER, lat, lon, now))
        }
    }

    fun stop() {
        providers.forEach { p ->
            runCatching { lm.setTestProviderEnabled(p, false) }
            runCatching { lm.removeTestProvider(p) }
        }
        runCatching { fused.setMockMode(false) }
        started = false
    }

    private fun buildLocation(provider: String, lat: Double, lon: Double, time: Long): Location {
        // A few cm of jitter so the dot doesn't look frozen.
        val jitter = (Random.nextDouble() - 0.5) * 0.00001
        return Location(provider).apply {
            latitude = lat + jitter
            longitude = lon + jitter
            accuracy = 5f
            altitude = 100.0
            bearing = 0f
            speed = 0f
            this.time = time
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            if (Build.VERSION.SDK_INT >= 26) {
                bearingAccuracyDegrees = 0.1f
                speedAccuracyMetersPerSecond = 0.1f
                verticalAccuracyMeters = 5f
            }
        }
    }
}
