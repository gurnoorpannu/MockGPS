package com.gurnoor.locationchanger.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet

private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

// Esri World Imagery: free, no key, shows individual rooftops in Punjab villages at zoom 18.
// Above 18 rural Punjab returns "Map data not yet available", so the source is capped at 18
// and MapLibre stretches those tiles when zooming further. Attribution is shown on screen.
private const val SATELLITE_URL =
    "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
private const val SAT_SOURCE = "satellite"
private const val SAT_LAYER = "satellite-layer"

/**
 * Google-Maps-style: the selection is always the map centre (a fixed pin is drawn over the centre
 * by the screen). The map reports its centre whenever it stops moving.
 */
class MapController {
    var map: MapLibreMap? = null
        private set
    private var style: Style? = null

    /** Satellite photo (houses visible) vs plain street map. On by default. */
    var satellite by mutableStateOf(true)
        private set

    fun bind(map: MapLibreMap) {
        this.map = map
    }

    fun onStyleLoaded(style: Style) {
        this.style = style
        applySatellite()
    }

    fun toggleSatellite() {
        satellite = !satellite
        applySatellite()
    }

    private fun applySatellite() {
        style?.getLayer(SAT_LAYER)?.setProperties(
            PropertyFactory.visibility(if (satellite) Property.VISIBLE else Property.NONE),
        )
    }

    fun moveTo(lat: Double, lon: Double, zoom: Double = 17.0) {
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), zoom))
    }

    fun zoomIn() {
        map?.animateCamera(CameraUpdateFactory.zoomIn())
    }

    fun zoomOut() {
        map?.animateCamera(CameraUpdateFactory.zoomOut())
    }
}

@Composable
fun rememberMapController(): MapController = remember { MapController() }

@Composable
fun MapPicker(
    controller: MapController,
    initialLat: Double,
    initialLon: Double,
    initialZoom: Double,
    onCenterChanged: (lat: Double, lon: Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapLibre.getInstance(context) // init the runtime (no API key needed)
        MapView(context).apply {
            onCreate(null)
            getMapAsync { map ->
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(initialLat, initialLon))
                    .zoom(initialZoom)
                    .build()
                map.setMaxZoomPreference(20.0)
                map.setStyle(Style.Builder().fromUri(STYLE_URL)) { style ->
                    val tiles = TileSet("2.2.0", SATELLITE_URL).apply { maxZoom = 18f }
                    style.addSource(RasterSource(SAT_SOURCE, tiles, 256))
                    // Below the first label layer: photo covers roads/fills, names stay on top.
                    val layer = RasterLayer(SAT_LAYER, SAT_SOURCE)
                    val firstLabel = style.layers.firstOrNull { it is SymbolLayer }?.id
                    if (firstLabel != null) style.addLayerBelow(layer, firstLabel) else style.addLayer(layer)
                    controller.onStyleLoaded(style)
                }
                map.uiSettings.isRotateGesturesEnabled = false
                map.uiSettings.isTiltGesturesEnabled = false
                map.uiSettings.isLogoEnabled = false
                map.uiSettings.isAttributionEnabled = false
                controller.bind(map)
                map.addOnCameraIdleListener {
                    val c = map.cameraPosition.target ?: return@addOnCameraIdleListener
                    onCenterChanged(c.latitude, c.longitude)
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(Unit) {
        onDispose { mapView.onDestroy() }
    }

    AndroidView(modifier = modifier, factory = { mapView })
}
