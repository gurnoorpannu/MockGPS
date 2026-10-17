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
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.gurnoor.locationchanger.ui.theme.StopRed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        title = { Text(stringResource(R.string.saved_places_title), fontSize = 19.sp) },
        text = {
            if (places.isEmpty()) {
                Text(stringResource(R.string.no_saved_places), fontSize = 16.sp)
            } else {
                Column(
                    modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    places.forEach { place ->
                        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = place.name,
                                    fontSize = 16.sp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSelect(place) }
                                        .padding(vertical = 10.dp),
                                )
                                OutlinedButton(
                                    onClick = { onDelete(place) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StopRed),
                                    border = BorderStroke(1.dp, StopRed),
                                ) {
                                    Text(stringResource(R.string.delete), fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onClose) {
                Text(stringResource(R.string.close), fontSize = 16.sp)
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
