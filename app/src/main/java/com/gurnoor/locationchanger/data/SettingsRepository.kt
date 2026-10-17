package com.gurnoor.locationchanger.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Everything the app remembers across restarts. */
data class AppSettings(
    val language: String?,               // null until the user has chosen one
    val active: Boolean,                 // is the location currently changed
    val weEnabledDevOptions: Boolean,    // did WE turn Developer options on (so we may turn it off)
    val worksWithoutDevOptions: Boolean?, // learned once: can we mock without Developer options
    val lastPlace: SavedPlace?,          // last chosen pin
    val savedPlaces: List<SavedPlace>,
    val batteryAsked: Boolean,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
    private val ds = context.applicationContext.dataStore
    private val json = Json { ignoreUnknownKeys = true }
    private val listSerializer = ListSerializer(SavedPlace.serializer())

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val ACTIVE = booleanPreferencesKey("active")
        val WE_ENABLED_DEV = booleanPreferencesKey("we_enabled_dev_options")
        val WORKS_WITHOUT_DEV = stringPreferencesKey("works_without_dev") // "yes" / "no" / absent
        val LAST_NAME = stringPreferencesKey("last_name")
        val LAST_LAT = doublePreferencesKey("last_lat")
        val LAST_LON = doublePreferencesKey("last_lon")
        val SAVED = stringPreferencesKey("saved_places")
        val BATTERY_ASKED = booleanPreferencesKey("battery_asked")
    }

    val settings: Flow<AppSettings> = ds.data.map { p -> p.toAppSettings() }

    suspend fun current(): AppSettings = settings.first()

    private fun Preferences.toAppSettings(): AppSettings {
        val lastName = this[Keys.LAST_NAME]
        val lastLat = this[Keys.LAST_LAT]
        val lastLon = this[Keys.LAST_LON]
        val last = if (lastName != null && lastLat != null && lastLon != null)
            SavedPlace(lastName, lastLat, lastLon) else null
        val worksRaw = this[Keys.WORKS_WITHOUT_DEV]
        return AppSettings(
            language = this[Keys.LANGUAGE],
            active = this[Keys.ACTIVE] ?: false,
            weEnabledDevOptions = this[Keys.WE_ENABLED_DEV] ?: false,
            worksWithoutDevOptions = when (worksRaw) {
                "yes" -> true; "no" -> false; else -> null
            },
            lastPlace = last,
            savedPlaces = decodeSaved(this[Keys.SAVED]),
            batteryAsked = this[Keys.BATTERY_ASKED] ?: false,
        )
    }

    private fun decodeSaved(raw: String?): List<SavedPlace> =
        if (raw.isNullOrBlank()) emptyList()
        else runCatching { json.decodeFromString(listSerializer, raw) }.getOrDefault(emptyList())

    suspend fun setLanguage(tag: String) = ds.edit { it[Keys.LANGUAGE] = tag }
    suspend fun setActive(active: Boolean) = ds.edit { it[Keys.ACTIVE] = active }
    suspend fun setWeEnabledDevOptions(v: Boolean) = ds.edit { it[Keys.WE_ENABLED_DEV] = v }
    suspend fun setWorksWithoutDevOptions(v: Boolean) =
        ds.edit { it[Keys.WORKS_WITHOUT_DEV] = if (v) "yes" else "no" }
    suspend fun setBatteryAsked(v: Boolean) = ds.edit { it[Keys.BATTERY_ASKED] = v }

    suspend fun setLastPlace(place: SavedPlace) = ds.edit {
        it[Keys.LAST_NAME] = place.name
        it[Keys.LAST_LAT] = place.latitude
        it[Keys.LAST_LON] = place.longitude
    }

    suspend fun addSavedPlace(place: SavedPlace) = ds.edit { prefs ->
        val list = decodeSaved(prefs[Keys.SAVED]).toMutableList()
        list.add(place)
        prefs[Keys.SAVED] = json.encodeToString(listSerializer, list)
    }

    suspend fun deleteSavedPlace(place: SavedPlace) = ds.edit { prefs ->
        val list = decodeSaved(prefs[Keys.SAVED])
            .filterNot { it.name == place.name && it.latitude == place.latitude && it.longitude == place.longitude }
        prefs[Keys.SAVED] = json.encodeToString(listSerializer, list)
    }
}
