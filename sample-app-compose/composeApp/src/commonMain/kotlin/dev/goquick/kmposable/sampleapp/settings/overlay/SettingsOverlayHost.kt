package dev.goquick.kmposable.sampleapp.settings.overlay

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState

@Composable
fun SettingsOverlayHost(node: SettingsOverlayNode) {
    val state by node.state.collectAsState()

    AlertDialog(
        onDismissRequest = { node.onEvent(SettingsOverlayEvent.Dismiss) },
        title = { Text(state.title) },
        text = { Text(state.message) },
        confirmButton = {
            TextButton(onClick = { node.onEvent(SettingsOverlayEvent.Confirm) }) {
                Text("Got it")
            }
        },
        dismissButton = {
            TextButton(onClick = { node.onEvent(SettingsOverlayEvent.Dismiss) }) {
                Text("Close")
            }
        }
    )
}
