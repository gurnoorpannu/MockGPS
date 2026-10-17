package com.gurnoor.locationchanger

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gurnoor.locationchanger.ui.MainViewModel
import com.gurnoor.locationchanger.ui.screens.HelpScreen
import com.gurnoor.locationchanger.ui.screens.HomeScreen
import com.gurnoor.locationchanger.ui.screens.LanguageScreen
import com.gurnoor.locationchanger.ui.screens.SetupScreen
import com.gurnoor.locationchanger.ui.theme.LocationChangerTheme
import kotlinx.coroutines.delay

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LocationChangerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val vm: MainViewModel = viewModel()
    val state by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showHelp by rememberSaveable { mutableStateOf(false) }

    val notifLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}
    val locationLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { vm.refreshSetup() }

    // When the daily screen is reachable, make sure we have the runtime permissions and, once,
    // ask to ignore battery optimization so One UI doesn't kill the service.
    val ready = state.language != null && !state.needsSetup
    LaunchedEffect(ready) {
        if (!ready) return@LaunchedEffect
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // One-time: ask One UI to keep the app running (ignore battery optimization).
        if (!state.batteryAsked) {
            vm.markBatteryAsked()
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}"),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.language == null -> LanguageScreen(onChoose = vm::chooseLanguage)
            state.needsSetup -> SetupScreen(status = state.setupStatus, onCheckAgain = vm::refreshSetup)
            showHelp -> HelpScreen(onClose = { showHelp = false })
            else -> HomeScreen(
                state = state,
                onToggleLanguage = vm::toggleLanguage,
                onHelp = { showHelp = true },
                onSearch = vm::search,
                onSelectResult = vm::selectResult,
                onMapTap = vm::onMapTap,
                onSelectSaved = vm::selectSaved,
                onSavePlace = vm::savePlace,
                onDeletePlace = vm::deletePlace,
                onChange = vm::changeLocation,
                onStop = vm::stop,
                realLocation = vm::realLocation,
            )
        }

        // Big, few-second feedback message (not a tiny toast).
        state.message?.let { msg ->
            LaunchedEffect(msg) {
                delay(4000)
                vm.dismissMessage()
            }
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (msg.isError) Color(0xFF6A1B1B) else Color(0xFF14361F),
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = if (msg.arg != null) stringResource(msg.resId, msg.arg) else stringResource(msg.resId),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(18.dp),
                )
            }
        }
    }

    if (state.locationOff) {
        AlertDialog(
            onDismissRequest = vm::dismissLocationOff,
            title = { Text(stringResource(R.string.location_off_title), style = MaterialTheme.typography.titleLarge) },
            text = { Text(stringResource(R.string.location_off_message), style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                TextButton(onClick = {
                    vm.dismissLocationOff()
                    context.startActivity(
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }) { Text(stringResource(R.string.open_location_settings), style = MaterialTheme.typography.titleMedium) }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissLocationOff) {
                    Text(stringResource(R.string.cancel), style = MaterialTheme.typography.titleMedium)
                }
            },
        )
    }
}
