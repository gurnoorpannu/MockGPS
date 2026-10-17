package com.gurnoor.locationchanger.ui.screens

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gurnoor.locationchanger.R
import com.gurnoor.locationchanger.ui.components.BigButton

@Composable
fun HelpScreen(onClose: () -> Unit) {
    val steps = listOf(
        "📍" to R.string.help_step1,
        "📌" to R.string.help_step2,
        "✅" to R.string.help_step3,
        "📱" to R.string.help_step4,
        "↩️" to R.string.help_step5,
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.help_title), style = MaterialTheme.typography.headlineMedium)
        steps.forEach { (icon, resId) ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(icon, style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(resId), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        BigButton(text = stringResource(R.string.close), onClick = onClose)
    }
}
