package com.gurnoor.locationchanger.ui

import android.app.Application
import android.content.Context
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gurnoor.locationchanger.R
import com.gurnoor.locationchanger.data.SavedPlace
import com.gurnoor.locationchanger.data.SettingsRepository
import com.gurnoor.locationchanger.i18n.LocaleManager
import com.gurnoor.locationchanger.net.GeoResult
import com.gurnoor.locationchanger.net.Nominatim
import com.gurnoor.locationchanger.service.MockLocationService
import com.gurnoor.locationchanger.service.ServiceBus
import com.gurnoor.locationchanger.service.ServiceEvent
import com.gurnoor.locationchanger.system.AndroidGlobalSettingsGateway
import com.gurnoor.locationchanger.system.AndroidSetupEnvironment
import com.gurnoor.locationchanger.system.DevOptionsController
import com.gurnoor.locationchanger.system.SetupChecker
import com.gurnoor.locationchanger.system.SetupStatus
import com.gurnoor.locationchanger.system.isLocationServiceEnabled
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiMessage(@StringRes val resId: Int, val arg: String? = null, val isError: Boolean = false)

data class HomeUiState(
    val language: String? = null,
    val active: Boolean = false,
    val selected: SavedPlace? = null,
    val activePlaceName: String? = null,
    val savedPlaces: List<SavedPlace> = emptyList(),
    val searchResults: List<GeoResult> = emptyList(),
    val searching: Boolean = false,
    val message: UiMessage? = null,
    val setupStatus: SetupStatus? = null,
    val needsSetup: Boolean = false,
    val locationOff: Boolean = false,
    val batteryAsked: Boolean = false,
    // Bumped whenever the map should recenter on `selected` (screen reacts to the change).
    val moveToSelectedTick: Int = 0,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = SettingsRepository(app)
    private val setupChecker = SetupChecker(AndroidSetupEnvironment(app))
    private val devOptions = DevOptionsController(AndroidGlobalSettingsGateway(app))

    private val _ui = MutableStateFlow(HomeUiState())
    val ui = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            settings.settings.collect { s ->
                _ui.update { state ->
                    state.copy(
                        language = s.language,
                        active = s.active,
                        savedPlaces = s.savedPlaces,
                        selected = state.selected ?: s.lastPlace,
                        activePlaceName = if (s.active) s.lastPlace?.name else null,
                        batteryAsked = s.batteryAsked,
                    )
                }
            }
        }
        refreshSetup()
        // Safety net: not mocking but we had turned Developer options on → turn it back off.
        viewModelScope.launch {
            val s = settings.current()
            if (!s.active && s.weEnabledDevOptions) {
                runCatching { devOptions.disableIfWeEnabled(true) }
                settings.setWeEnabledDevOptions(false)
            }
        }
        viewModelScope.launch {
            ServiceBus.events.collect { event -> handleServiceEvent(event) }
        }
    }

    private fun handleServiceEvent(event: ServiceEvent) {
        when (event) {
            ServiceEvent.FAILED -> _ui.update { it.copy(message = UiMessage(R.string.msg_error, isError = true)) }
            ServiceEvent.LOCATION_OFF -> _ui.update { it.copy(locationOff = true) }
            ServiceEvent.STARTED, ServiceEvent.STOPPED -> Unit
        }
    }

    fun refreshSetup() {
        val status = setupChecker.check()
        _ui.update { it.copy(setupStatus = status, needsSetup = !status.adbSetupComplete) }
    }

    fun chooseLanguage(tag: String) {
        LocaleManager.apply(tag)
        viewModelScope.launch { settings.setLanguage(tag) }
    }

    fun toggleLanguage() {
        val next = if ((_ui.value.language ?: LocaleManager.EN) == LocaleManager.PA)
            LocaleManager.EN else LocaleManager.PA
        chooseLanguage(next)
    }

    private fun currentLang() = _ui.value.language ?: LocaleManager.current()

    private var reverseJob: kotlinx.coroutines.Job? = null

    /** The map centre moved (Google-Maps style). Update the selection and refine its name. */
    fun onCenterChanged(lat: Double, lon: Double) {
        val placeholder = getApplication<Application>().getString(R.string.selected_place_fallback)
        _ui.update {
            val keptName = it.selected?.name?.takeIf { n -> n != placeholder }
            it.copy(selected = SavedPlace(keptName ?: placeholder, lat, lon))
        }
        reverseJob?.cancel()
        reverseJob = viewModelScope.launch {
            delay(350)
            val name = runCatching { Nominatim.reverse(lat, lon, currentLang()) }.getOrNull() ?: return@launch
            _ui.update { state ->
                val sel = state.selected
                if (sel != null && sel.latitude == lat && sel.longitude == lon)
                    state.copy(selected = sel.copy(name = name)) else state
            }
        }
    }

    fun search(query: String) {
        if (query.isBlank()) return
        if (!hasInternet()) {
            _ui.update { it.copy(message = UiMessage(R.string.msg_no_internet, isError = true)) }
            return
        }
        _ui.update { it.copy(searching = true, searchResults = emptyList()) }
        viewModelScope.launch {
            val results = runCatching { Nominatim.search(query, currentLang()) }
            results.onSuccess { list ->
                _ui.update {
                    it.copy(
                        searching = false,
                        searchResults = list,
                        message = if (list.isEmpty()) UiMessage(R.string.no_results) else null,
                    )
                }
            }.onFailure {
                _ui.update { it.copy(searching = false, message = UiMessage(R.string.search_failed, isError = true)) }
            }
        }
    }

    fun selectResult(result: GeoResult) {
        _ui.update {
            it.copy(
                selected = SavedPlace(result.name, result.latitude, result.longitude),
                searchResults = emptyList(),
                moveToSelectedTick = it.moveToSelectedTick + 1,
            )
        }
    }

    fun selectSaved(place: SavedPlace) {
        _ui.update { it.copy(selected = place, moveToSelectedTick = it.moveToSelectedTick + 1) }
    }

    fun savePlace(name: String) {
        val sel = _ui.value.selected ?: return
        viewModelScope.launch { settings.addSavedPlace(sel.copy(name = name.ifBlank { sel.name })) }
    }

    fun deletePlace(place: SavedPlace) {
        viewModelScope.launch { settings.deleteSavedPlace(place) }
    }

    fun changeLocation() {
        val sel = _ui.value.selected ?: return
        val context = getApplication<Application>()
        if (!isLocationServiceEnabled(context)) {
            _ui.update { it.copy(locationOff = true) }
            return
        }
        ContextCompat.startForegroundService(
            context, MockLocationService.startIntent(context, sel, currentLang()),
        )
        _ui.update { it.copy(message = UiMessage(R.string.msg_changed), activePlaceName = sel.name) }
    }

    fun stop() {
        val context = getApplication<Application>()
        context.startService(MockLocationService.stopIntent(context))
        _ui.update { it.copy(message = UiMessage(R.string.msg_back_to_real)) }
    }

    fun dismissMessage() = _ui.update { it.copy(message = null) }
    fun dismissLocationOff() = _ui.update { it.copy(locationOff = false) }
    fun markBatteryAsked() {
        viewModelScope.launch { settings.setBatteryAsked(true) }
    }

    /** Last known real location for the "My real location" button, or null if unknown. */
    fun realLocation(): Pair<Double, Double>? {
        val lm = getApplication<Application>().getSystemService(LocationManager::class.java) ?: return null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        for (p in providers) {
            val loc = runCatching { lm.getLastKnownLocation(p) }.getOrNull()
            if (loc != null) return loc.latitude to loc.longitude
        }
        return null
    }

    fun hasInternet(): Boolean {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
