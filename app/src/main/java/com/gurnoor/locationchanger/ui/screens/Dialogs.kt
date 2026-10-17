package com.gurnoor.locationchanger.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gurnoor.locationchanger.R
import com.gurnoor.locationchanger.data.SavedPlace

@Composable
fun SaveDialog(initialName: String, onConfirm: (String) -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.save_dialog_title), style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.name_label)) },
                textStyle = MaterialTheme.typography.bodyLarge,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) {
                Text(stringResource(R.string.save), style = MaterialTheme.typography.titleMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel), style = MaterialTheme.typography.titleMedium)
            }
        },
    )
}

@Composable
fun SavedPlacesDialog(
    places: List<SavedPlace>,
    onSelect: (SavedPlace) -> Unit,
    onDelete: (SavedPlace) -> Unit,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.saved_places_title), style = MaterialTheme.typography.titleLarge) },
        text = {
            if (places.isEmpty()) {
                Text(stringResource(R.string.no_saved_places), style = MaterialTheme.typography.bodyLarge)
            } else {
                Column(
                    modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    places.forEach { place ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = place.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelect(place) }
                                    .padding(vertical = 10.dp),
                            )
                            TextButton(onClick = { onDelete(place) }) {
                                Text(stringResource(R.string.delete), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.close), style = MaterialTheme.typography.titleMedium)
            }
        },
    )
}

@Composable
fun ConfirmDeleteDialog(place: SavedPlace, onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.delete_confirm_title), style = MaterialTheme.typography.titleLarge) },
        text = { Text(stringResource(R.string.delete_confirm_message, place.name), style = MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.yes_delete), style = MaterialTheme.typography.titleMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.no), style = MaterialTheme.typography.titleMedium)
            }
        },
    )
}
