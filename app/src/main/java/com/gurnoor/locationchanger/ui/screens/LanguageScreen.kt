package com.gurnoor.locationchanger.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gurnoor.locationchanger.R
import com.gurnoor.locationchanger.i18n.LocaleManager
import com.gurnoor.locationchanger.ui.components.BigButton

@Composable
fun LanguageScreen(onChoose: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.choose_language),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        BigButton(text = stringResource(R.string.lang_english), onClick = { onChoose(LocaleManager.EN) })
        BigButton(text = stringResource(R.string.lang_punjabi), onClick = { onChoose(LocaleManager.PA) })
    }
}
