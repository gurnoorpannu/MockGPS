package com.gurnoor.locationchanger.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gurnoor.locationchanger.R
import com.gurnoor.locationchanger.data.SavedPlace
import com.gurnoor.locationchanger.net.GeoResult
import com.gurnoor.locationchanger.ui.HomeUiState
import com.gurnoor.locationchanger.ui.components.MapPicker
import com.gurnoor.locationchanger.ui.components.rememberMapController
import com.gurnoor.locationchanger.ui.theme.ActiveGreen
import com.gurnoor.locationchanger.ui.theme.StopRed

private const val DEFAULT_LAT = 30.9
private const val DEFAULT_LON = 75.85
private const val DEFAULT_ZOOM = 6.0

@Composable
fun HomeScreen(
    state: HomeUiState,
    onToggleLanguage: () -> Unit,
    onToggleDevOptions: () -> Unit,
    onHelp: () -> Unit,
    onSearch: (String) -> Unit,
    onQueryChanged: (String) -> Unit,
    onSelectResult: (GeoResult) -> Unit,
    onCenterChanged: (Double, Double) -> Unit,
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
    var showPlaces by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<SavedPlace?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.moveToSelectedTick) {
        if (state.moveToSelectedTick == 0) return@LaunchedEffect
        val sel = state.selected ?: return@LaunchedEffect
        var tries = 0
        while (controller.map == null && tries < 40) {
            kotlinx.coroutines.delay(50); tries++
        }
        controller.moveTo(sel.latitude, sel.longitude)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapPicker(
            controller = controller,
            initialLat = state.selected?.latitude ?: DEFAULT_LAT,
            initialLon = state.selected?.longitude ?: DEFAULT_LON,
            initialZoom = if (state.selected != null) 15.0 else DEFAULT_ZOOM,
            onCenterChanged = onCenterChanged,
            modifier = Modifier.fillMaxSize(),
        )

        // Fixed centre pin: tip points at the exact map centre.
        Image(
            painter = painterResource(R.drawable.ic_pin),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(44.dp)
                .offset(y = (-22).dp),
        )

        // Right-side controls.
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 84.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Satellite (rooftops visible) <-> street map.
            RoundControl(if (controller.satellite) "🗺" else "🛰") { controller.toggleSatellite() }
            RoundControl("+") { controller.zoomIn() }
            RoundControl("−") { controller.zoomOut() }
            RoundControl("◎") { realLocation()?.let { controller.moveTo(it.first, it.second) } }
        }

        // Required map credits (OpenStreetMap data, Esri imagery), kept small.
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 74.dp, start = 16.dp),
            shape = RoundedCornerShape(6.dp),
            color = Color.White.copy(alpha = 0.75f),
        ) {
            Text(
                text = if (controller.satellite) "© OpenStreetMap · Esri, Maxar" else "© OpenStreetMap · OpenFreeMap",
                fontSize = 10.sp,
                color = Color.Black,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        // Top: compact search bar + language pill, then results.
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 10.dp),
                    ) {
                        Text("🔎", fontSize = 16.sp)
                        Spacer(Modifier.size(10.dp))
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it; onQueryChanged(it) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 17.sp,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                keyboard?.hide(); onSearch(query)
                            }),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (query.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.search_hint),
                                            fontSize = 17.sp,
                                            maxLines = 1,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    inner()
                                }
                            },
                        )
                        if (state.searching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                }
                Pill(
                    text = if ((state.language ?: "en") == "pa")
                        stringResource(R.string.lang_english) else stringResource(R.string.lang_punjabi),
                    onClick = onToggleLanguage,
                )
            }

            if (state.searchResults.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp,
                ) {
                    Column(modifier = Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState())) {
                        state.searchResults.forEachIndexed { i, result ->
                            if (i > 0) HorizontalDivider()
                            Text(
                                text = result.name,
                                fontSize = 17.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { keyboard?.hide(); query = ""; onSelectResult(result) }
                                    .padding(16.dp),
                            )
                        }
                    }
                }
            }
        }

        BottomCard(
            state = state,
            onChange = onChange,
            onStop = onStop,
            onSave = { showSaveDialog = true },
            onPlaces = { showPlaces = true },
            onToggleDevOptions = onToggleDevOptions,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(12.dp),
        )
    }

    if (showSaveDialog) {
        SaveDialog(
            initialName = state.selected?.name.orEmpty(),
            onConfirm = { name -> onSavePlace(name); showSaveDialog = false },
            onCancel = { showSaveDialog = false },
        )
    }
    if (showPlaces) {
        SavedPlacesDialog(
            places = state.savedPlaces,
            onSelect = { onSelectSaved(it); showPlaces = false },
            onDelete = { deleteTarget = it },
            onClose = { showPlaces = false },
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
private fun BottomCard(
    state: HomeUiState,
    onChange: () -> Unit,
    onStop: () -> Unit,
    onSave: () -> Unit,
    onPlaces: () -> Unit,
    onToggleDevOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Place name + small subtitle / status.
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = state.selected?.name ?: stringResource(R.string.selected_place_fallback),
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (state.active) ActiveGreen else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (state.active) stringResource(R.string.status_active, "").trimEnd(' ', ':')
                    else stringResource(R.string.set_hint),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            PrimaryButton(
                text = if (state.active) stringResource(R.string.stop_short) else stringResource(R.string.change_short),
                onClick = if (state.active) onStop else onChange,
                enabled = state.active || state.selected != null,
                color = if (state.active) StopRed else ActiveGreen,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Chip(stringResource(R.string.save), onSave, Modifier.weight(1f), enabled = state.selected != null)
                Chip(stringResource(R.string.saved_short), onPlaces, Modifier.weight(1f))
            }

            // Developer options + USB debugging switch (banking apps refuse to run while on).
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.dev_options_label), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(
                        stringResource(R.string.dev_options_hint),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.devOptionsOn,
                    onCheckedChange = { onToggleDevOptions() },
                    colors = SwitchDefaults.colors(checkedTrackColor = StopRed),
                )
            }
        }
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean, color: Color) {
    val haptics = LocalHapticFeedback.current
    Button(
        onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Chip(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
private fun Pill(text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        modifier = Modifier.height(52.dp).clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(text, fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable
private fun RoundControl(symbol: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        modifier = Modifier.size(46.dp).clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(symbol, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}
