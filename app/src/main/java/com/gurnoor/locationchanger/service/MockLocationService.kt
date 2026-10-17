package com.gurnoor.locationchanger.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.gurnoor.locationchanger.R
import com.gurnoor.locationchanger.data.SavedPlace
import com.gurnoor.locationchanger.data.SettingsRepository
import com.gurnoor.locationchanger.system.AndroidGlobalSettingsGateway
import com.gurnoor.locationchanger.system.DevOptionsController
import com.gurnoor.locationchanger.system.MockLocationController
import com.gurnoor.locationchanger.system.isLocationServiceEnabled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.content.res.Configuration
import java.util.Locale

/**
 * Foreground service (type location) that keeps pushing the chosen location once a second and
 * manages Developer options around it. Everything the father never has to touch happens here.
 */
class MockLocationService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var mock: MockLocationController
    private lateinit var devOptions: DevOptionsController
    private lateinit var settings: SettingsRepository
    private var pushJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        mock = MockLocationController(this)
        devOptions = DevOptionsController(AndroidGlobalSettingsGateway(this))
        settings = SettingsRepository(this)
        createChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopMocking()
                return START_NOT_STICKY
            }
            else -> start(intent)
        }
        // If the process is killed, come back with the same intent so mocking resumes.
        return START_REDELIVER_INTENT
    }

    private fun start(intent: Intent?) {
        val lang = intent?.getStringExtra(EXTRA_LANG) ?: "en"
        val name = intent?.getStringExtra(EXTRA_NAME).orEmpty()
        val lat = intent?.getDoubleExtra(EXTRA_LAT, Double.NaN) ?: Double.NaN
        val lon = intent?.getDoubleExtra(EXTRA_LON, Double.NaN) ?: Double.NaN

        // Go foreground immediately (required within a few seconds of startForegroundService).
        startForegroundSafely(buildNotification(name.ifBlank { getLocalized(lang, R.string.selected_place_fallback) }, lang))

        if (!isLocationServiceEnabled(this)) {
            ServiceBus.emit(ServiceEvent.LOCATION_OFF)
            stopSelfAndForeground()
            return
        }

        pushJob?.cancel()
        pushJob = scope.launch {
            // Resolve coordinates: explicit extras, else the last saved place (process restart).
            val place = if (!lat.isNaN() && !lon.isNaN()) SavedPlace(name, lat, lon)
            else settings.current().lastPlace
            if (place == null) {
                stopSelfAndForeground()
                return@launch
            }

            val snap = settings.current()
            var weEnabled = false
            // If last time we learned we needed Developer options, turn it on up front.
            if (snap.worksWithoutDevOptions == false) weEnabled = devOptions.enable()

            var ok = tryStart(place)
            if (!ok) {
                // Retry once with Developer options enabled.
                weEnabled = devOptions.enable() || weEnabled
                ok = tryStart(place)
            }

            if (!ok) {
                devOptions.disableIfWeEnabled(weEnabled)
                settings.setWeEnabledDevOptions(false)
                settings.setActive(false)
                ServiceBus.emit(ServiceEvent.FAILED)
                stopSelfAndForeground()
                return@launch
            }

            // Record which path worked so next time is direct.
            settings.setWorksWithoutDevOptions(!weEnabled)
            settings.setWeEnabledDevOptions(weEnabled)
            settings.setActive(true)
            settings.setLastPlace(place)
            ServiceBus.emit(ServiceEvent.STARTED)

            // Keep pushing ~1/second until stopped.
            while (isActive) {
                mock.push(place.latitude, place.longitude)
                delay(1000)
            }
        }
    }

    private fun tryStart(place: SavedPlace): Boolean = runCatching {
        mock.start()
        mock.push(place.latitude, place.longitude)
    }.isSuccess

    private fun stopMocking() {
        pushJob?.cancel()
        pushJob = null
        scope.launch {
            runCatching { mock.stop() }
            val snap = settings.current()
            runCatching { devOptions.disableIfWeEnabled(snap.weEnabledDevOptions) }
            settings.setWeEnabledDevOptions(false)
            settings.setActive(false)
            ServiceBus.emit(ServiceEvent.STOPPED)
            stopSelfAndForeground()
        }
    }

    private fun stopSelfAndForeground() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        pushJob?.cancel()
        // Best-effort cleanup if the system kills us without a Stop action.
        runCatching { mock.stop() }
        super.onDestroy()
    }

    // --- Notification ---

    private fun startForegroundSafely(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this, NOTIF_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun buildNotification(placeName: String, lang: String): Notification {
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, MockLocationService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getLocalized(lang, R.string.notif_title))
            .setContentText(placeName)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(0, getLocalized(lang, R.string.notif_stop), stopIntent)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { setShowBadge(false) }
            mgr.createNotificationChannel(channel)
        }
    }

    /** Resolve a string in the chosen language regardless of the service's own locale. */
    private fun getLocalized(lang: String, resId: Int): String {
        val config = Configuration(resources.configuration)
        config.setLocale(Locale.forLanguageTag(lang))
        return createConfigurationContext(config).getString(resId)
    }

    companion object {
        private const val CHANNEL_ID = "location_changed"
        private const val NOTIF_ID = 1001
        const val ACTION_STOP = "com.gurnoor.locationchanger.STOP"
        private const val EXTRA_LAT = "lat"
        private const val EXTRA_LON = "lon"
        private const val EXTRA_NAME = "name"
        private const val EXTRA_LANG = "lang"

        fun startIntent(context: Context, place: SavedPlace, lang: String) =
            Intent(context, MockLocationService::class.java).apply {
                putExtra(EXTRA_LAT, place.latitude)
                putExtra(EXTRA_LON, place.longitude)
                putExtra(EXTRA_NAME, place.name)
                putExtra(EXTRA_LANG, lang)
            }

        fun stopIntent(context: Context) =
            Intent(context, MockLocationService::class.java).setAction(ACTION_STOP)
    }
}
