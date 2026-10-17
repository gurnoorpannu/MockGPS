package com.gurnoor.locationchanger.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.gurnoor.locationchanger.R
import com.gurnoor.locationchanger.data.SavedPlace
import com.gurnoor.locationchanger.net.GeoResult
import com.gurnoor.locationchanger.ui.HomeUiState
import com.gurnoor.locationchanger.ui.components.BigButton
import com.gurnoor.locationchanger.ui.components.LabeledButton
import com.gurnoor.locationchanger.ui.components.MapController
import com.gurnoor.locationchanger.ui.components.MapPicker
import com.gurnoor.locationchanger.ui.components.StatusCard
import com.gurnoor.locationchanger.ui.components.rememberMapController
import com.gurnoor.locationchanger.ui.theme.ActiveGreen
import com.gurnoor.locationchanger.ui.theme.StopRed
import kotlinx.coroutines.delay

// Default view when nothing is chosen yet: roughly Punjab, zoomed out.
private const val DEFAULT_LAT = 30.9
private const val DEFAULT_LON = 75.85
private const val DEFAULT_ZOOM = 6.0

@Composable
fun HomeScreen(
    state: HomeUiState,
    onToggleLanguage: () -> Unit,
    onHelp: () -> Unit,
    onSearch: (String) -> Unit,
    onSelectResult: (GeoResult) -> Unit,
    onMapTap: (Double, Double) -> Unit,
    onSelectSaved: (SavedPlace) -> Unit,
    onSavePlace: (String) -> Unit,
    onDeletePlace: (SavedPlace) -> Unit,
    onChange: () -> Unit,
    onStop: () -> Unit,
    realLocation: () -> Pair<Double, Double>?,
) {
    val controller = rememberMapController()
    var query by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<SavedPlace?>(null) }
    val lastTick = remember { mutableIntStateOf(state.moveToSelectedTick) }

    // Draw / move the pin when the selection changes (move camera only on search / saved pick).
    LaunchedEffect(state.selected, state.moveToSelectedTick) {
        val sel = state.selected ?: return@LaunchedEffect
        var tries = 0
        while (controller.map == null && tries < 40) {
            delay(50); tries++
        }
        val move = state.moveToSelectedTick != lastTick.intValue
        lastTick.intValue = state.moveToSelectedTick
        controller.setPin(sel.latitude, sel.longitude, moveCamera = move)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Top row: Help + language toggle, both labelled.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LabeledButton(text = stringResource(R.string.help), onClick = onHelp, modifier = Modifier.weight(1f))
            LabeledButton(
                text = if ((state.language ?: "en") == "pa")
                    stringResource(R.string.lang_english) else stringResource(R.string.lang_punjabi),
                onClick = onToggleLanguage,
                modifier = Modifier.weight(1f),
            )
        }

        StatusCard(
            active = state.active,
            text = if (state.active)
                stringResource(R.string.status_active, state.activePlaceName ?: state.selected?.name.orEmpty())
            else stringResource(R.string.status_inactive),
        )

        // Search
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.search_hint), style = MaterialTheme.typography.bodyLarge) },
            textStyle = MaterialTheme.typography.bodyLarge,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
        )
        BigButton(
            text = if (state.searching) stringResource(R.string.searching) else stringResource(R.string.search_button),
            onClick = { onSearch(query) },
            enabled = !state.searching,
        )

        // Results (big tappable cards)
        state.searchResults.forEach { result ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickableBig { onSelectResult(result) }
                        .padding(18.dp),
                ) {
                    Text(result.name, style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        // Map + overlay controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
        ) {
            MapPicker(
                controller = controller,
                initialLat = state.selected?.latitude ?: DEFAULT_LAT,
                initialLon = state.selected?.longitude ?: DEFAULT_LON,
                initialZoom = if (state.selected != null) 14.0 else DEFAULT_ZOOM,
                onTap = onMapTap,
                modifier = Modifier.fillMaxSize(),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MapIconButton(stringResource(R.string.zoom_in), "+") { controller.zoomIn() }
                MapIconButton(stringResource(R.string.zoom_out), "−") { controller.zoomOut() }
            }
        }
        LabeledButton(
            text = stringResource(R.string.my_real_location),
            onClick = { realLocation()?.let { controller.moveTo(it.first, it.second) } },
            modifier = Modifier.fillMaxWidth(),
        )

        // Selected place name
        Text(
            text = state.selected?.name ?: stringResource(R.string.tap_map_first),
            style = MaterialTheme.typography.titleLarge,
        )

        // Main action
        if (state.active) {
            BigButton(
                text = stringResource(R.string.stop_go_back),
                onClick = onStop,
                container = StopRed,
            )
        } else {
            BigButton(
                text = stringResource(R.string.change_here),
                onClick = onChange,
                enabled = state.selected != null,
                container = ActiveGreen,
            )
            if (state.selected == null) {
                Text(stringResource(R.string.tap_map_first), style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Save
        LabeledButton(
            text = stringResource(R.string.save_place),
            onClick = { showSaveDialog = true },
            enabled = state.selected != null,
            modifier = Modifier.fillMaxWidth(),
        )

        // Saved places
        Text(stringResource(R.string.saved_places_title), style = MaterialTheme.typography.headlineSmall)
        if (state.savedPlaces.isEmpty()) {
            Text(stringResource(R.string.no_saved_places), style = MaterialTheme.typography.bodyLarge)
        } else {
            state.savedPlaces.forEach { place ->
                SavedPlaceCard(place = place, onSelect = { onSelectSaved(place) }, onDelete = { deleteTarget = place })
            }
        }
    }

    if (showSaveDialog) {
        SaveDialog(
            initialName = state.selected?.name.orEmpty(),
            onConfirm = { name -> onSavePlace(name); showSaveDialog = false },
            onCancel = { showSaveDialog = false },
        )
    }
    deleteTarget?.let { target ->
        ConfirmDeleteDialog(
            place = target,
            onConfirm = { onDeletePlace(target); deleteTarget = null },
            onCancel = { deleteTarget = null },
        )
    }
}

@Composable
private fun SavedPlaceCard(place: SavedPlace, onSelect: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickableBig(onSelect)
                    .padding(8.dp),
            ) {
                Text(place.name, style = MaterialTheme.typography.titleMedium)
            }
            LabeledButton(text = stringResource(R.string.delete), onClick = onDelete)
        }
    }
}

@Composable
private fun MapIconButton(label: String, symbol: String, onClick: () -> Unit) {
    // Labelled, not icon-only: the label is the accessibility/description text.
    Card(colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Color.Black)) {
        Box(
            modifier = Modifier
                .clickableBig(onClick)
                .padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(symbol, style = MaterialTheme.typography.headlineSmall)
                Text(label, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun Modifier.clickableBig(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
