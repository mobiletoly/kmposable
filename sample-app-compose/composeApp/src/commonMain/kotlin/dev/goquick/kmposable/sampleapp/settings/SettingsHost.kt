package dev.goquick.kmposable.sampleapp.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.goquick.kmposable.sampleapp.contacts.ui.SettingsScreen

/**
 * Settings host keeps UI pure and wires the node's state/events to the screen.
 * Even for small nodes, keeping the Host layer consistent makes NavFlow renderer wiring predictable.
 */
@Composable
fun SettingsHost(node: SettingsNode) {
    val state by node.state.collectAsState()
    SettingsScreen(state = state, onEvent = node::onEvent)
}
