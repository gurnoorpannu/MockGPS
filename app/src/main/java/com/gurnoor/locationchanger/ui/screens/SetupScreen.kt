package com.gurnoor.locationchanger.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gurnoor.locationchanger.R
import com.gurnoor.locationchanger.system.SetupStatus
import com.gurnoor.locationchanger.ui.components.BigButton
import com.gurnoor.locationchanger.ui.components.LabeledButton

/** English-only, meant for the person installing the app — never shown once setup passes. */
@Composable
fun SetupScreen(status: SetupStatus?, onCheckAgain: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.setup_intro), style = MaterialTheme.typography.bodyLarge)

        CommandBox(stringResource(R.string.setup_cmd1)) { copy(context, it) }
        CommandBox(stringResource(R.string.setup_cmd2)) { copy(context, it) }

        StatusRow(stringResource(R.string.setup_status_write), status?.writeSecureSettings == true)
        StatusRow(stringResource(R.string.setup_status_mock), status?.mockLocationAllowed == true)

        Text(stringResource(R.string.setup_samsung), style = MaterialTheme.typography.bodyMedium)

        BigButton(text = stringResource(R.string.setup_check_again), onClick = onCheckAgain)
    }
}

@Composable
private fun CommandBox(command: String, onCopy: (String) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF11181F), contentColor = Color(0xFFE6F0F7))) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = command,
                fontFamily = FontFamily.Monospace,
                fontSize = 15.sp,
                lineHeight = 22.sp,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                LabeledButton(text = stringResource(R.string.setup_copy), onClick = { onCopy(command) })
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, done: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (done) "✓" else "✗", style = MaterialTheme.typography.titleLarge,
            color = if (done) Color(0xFF1B7F3B) else Color(0xFFC62828))
        Text(
            text = "$label — " + stringResource(if (done) R.string.setup_granted else R.string.setup_missing),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

private fun copy(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("adb command", text))
}
