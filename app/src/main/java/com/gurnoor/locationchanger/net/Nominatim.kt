package com.gurnoor.locationchanger.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class GeoResult(val name: String, val latitude: Double, val longitude: Double)

/**
 * OpenStreetMap Nominatim geocoding. Respects the usage policy: one custom User-Agent, and a
 * single shared throttle guaranteeing at most ~1 request/second across search and reverse.
 * Searching only happens on the Search button (the ViewModel enforces that).
 */
object Nominatim {
    private const val BASE = "https://nominatim.openstreetmap.org"
    // Identifies the app per Nominatim policy. Add a contact email here if you run it heavily.
    private const val USER_AGENT = "LocationChanger/1.0 (Android; personal app)"

    private val throttleLock = Mutex()
    private var lastCallAt = 0L

    private suspend fun throttle() = throttleLock.withLock {
        val wait = 1100 - (System.currentTimeMillis() - lastCallAt)
        if (wait > 0) delay(wait)
        lastCallAt = System.currentTimeMillis()
    }

    private fun acceptLanguage(lang: String) = if (lang == "pa") "pa,en" else "en"

    suspend fun search(query: String, lang: String): List<GeoResult> = withContext(Dispatchers.IO) {
        throttle()
        val url = "$BASE/search?q=${enc(query)}&format=jsonv2&limit=8&addressdetails=1" +
            "&accept-language=${enc(acceptLanguage(lang))}"
        val body = get(url) ?: return@withContext emptyList()
        val arr = runCatching { JSONArray(body) }.getOrNull() ?: return@withContext emptyList()
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val lat = o.optString("lat").toDoubleOrNull() ?: continue
                val lon = o.optString("lon").toDoubleOrNull() ?: continue
                add(GeoResult(displayName(o), lat, lon))
            }
        }
    }

    /** Reverse-geocode a point into a human place name, or null if it can't be found. */
    suspend fun reverse(lat: Double, lon: Double, lang: String): String? =
        withContext(Dispatchers.IO) {
            throttle()
            val url = "$BASE/reverse?lat=$lat&lon=$lon&format=jsonv2&zoom=14" +
                "&accept-language=${enc(acceptLanguage(lang))}"
            val body = get(url) ?: return@withContext null
            val o = runCatching { JSONObject(body) }.getOrNull() ?: return@withContext null
            displayName(o).ifBlank { null }
        }

    private fun displayName(o: JSONObject): String {
        o.optJSONObject("address")?.let { a ->
            val primary = listOf("village", "town", "city", "hamlet", "suburb", "county")
                .firstNotNullOfOrNull { k -> a.optString(k).takeIf { it.isNotBlank() } }
            val region = listOf("state", "country")
                .firstNotNullOfOrNull { k -> a.optString(k).takeIf { it.isNotBlank() } }
            if (primary != null) return if (region != null) "$primary, $region" else primary
        }
        // Fall back to the first couple of parts of the long display name.
        return o.optString("display_name").split(",").take(2).joinToString(",").trim()
    }

    private fun get(urlStr: String): String? = runCatching {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        try {
            if (conn.responseCode != 200) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
