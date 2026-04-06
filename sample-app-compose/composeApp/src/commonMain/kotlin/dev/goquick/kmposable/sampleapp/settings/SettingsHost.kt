package dev.goquick.kmposable.sampleapp.settings

import androidx.compose.runtime.Composable
import dev.goquick.kmposable.compose.NodeHost
import dev.goquick.kmposable.sampleapp.contacts.ui.SettingsScreen

/**
 * Settings host keeps UI pure and wires the node's state/events to the screen.
 * Even for small nodes, keeping the Host layer consistent makes NavFlow renderer wiring predictable.
 */
@Composable
fun SettingsHost(
    node: SettingsNode,
    onNavigateBack: (() -> Unit)? = null,
) {
    NodeHost(node = node) { state, onEvent, _ ->
        SettingsScreen(
            state = state,
            onEvent = onEvent,
            onNavigateBack = onNavigateBack,
        )
    }
}
