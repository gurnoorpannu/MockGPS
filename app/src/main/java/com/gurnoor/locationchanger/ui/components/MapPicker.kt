package com.gurnoor.locationchanger.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

/** Holds the live map so the zoom / recenter buttons and the pin can drive it from Compose. */
class MapController {
    var map: MapLibreMap? = null
        private set
    private var marker: Marker? = null

    fun bind(map: MapLibreMap) {
        this.map = map
    }

    fun setPin(lat: Double, lon: Double, moveCamera: Boolean) {
        val m = map ?: return
        marker?.let { m.removeMarker(it) }
        marker = m.addMarker(MarkerOptions().position(LatLng(lat, lon)))
        if (moveCamera) moveTo(lat, lon)
    }

    fun moveTo(lat: Double, lon: Double, zoom: Double = 14.0) {
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
    onTap: (lat: Double, lon: Double) -> Unit,
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
                map.setStyle(Style.Builder().fromUri(STYLE_URL))
                map.uiSettings.isRotateGesturesEnabled = false
                map.uiSettings.isTiltGesturesEnabled = false
                controller.bind(map)
                map.addOnMapClickListener { point ->
                    onTap(point.latitude, point.longitude)
                    true
                }
            }
        }
    }

    // MapLibre's MapView needs the Activity lifecycle forwarded to it.
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
