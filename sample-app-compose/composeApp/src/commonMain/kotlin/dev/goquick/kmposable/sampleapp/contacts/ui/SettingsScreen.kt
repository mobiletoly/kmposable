package dev.goquick.kmposable.sampleapp.contacts.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Switch
import dev.goquick.kmposable.sampleapp.settings.SettingsEvent
import dev.goquick.kmposable.sampleapp.settings.SettingsState

@Composable
fun SettingsScreen(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    onShowOverlay: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = "Settings", style = MaterialTheme.typography.headlineMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Allow sync", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = state.allowSync,
                    onCheckedChange = { onEvent(SettingsEvent.AllowSyncChanged(it)) }
                )
            }
            if (onShowOverlay != null) {
                Button(onClick = onShowOverlay) {
                    Text("Show overlay")
                }
            }
        }
    }
}
