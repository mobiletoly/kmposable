package dev.goquick.kmposable.sampleapp.contacts.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.goquick.kmposable.compose.CollectEffects
import dev.goquick.kmposable.sampleapp.contacts.flow.ContactDetailsEffect
import dev.goquick.kmposable.sampleapp.contacts.flow.ContactDetailsNode
import dev.goquick.kmposable.sampleapp.contacts.flow.ContactsListNode
import dev.goquick.kmposable.sampleapp.contacts.flow.EditContactEffect
import dev.goquick.kmposable.sampleapp.contacts.flow.EditContactNode

/**
 * Hosts sit between Nodes (headless logic) and Screens (pure UI).
 *
 * Responsibilities:
 * - collect node state/effects
 * - handle transient UI (snackbars/dialogs) + DI/wiring
 * - pass clean callbacks + state down to the Screen
 */
@Composable
fun ContactsListHost(node: ContactsListNode) {
    // This host is intentionally thin: the node exposes only state, so we just collect and pass it through.
    val state by node.state.collectAsState()
    ContactsListScreen(state = state, onEvent = node::onEvent)
}

@Composable
fun ContactDetailsHost(
    node: ContactDetailsNode,
    snackbarHostState: SnackbarHostState
) {
    val state by node.state.collectAsState()
    CollectEffects(node) { effect ->
        if (effect is ContactDetailsEffect.ShowMessage) {
            snackbarHostState.showSnackbar(effect.text)
        }
    }

    ContactDetailsScreen(
        state = state,
        onEvent = node::onEvent,
        modifier = Modifier
    )
}

@Composable
fun EditContactHost(
    node: EditContactNode,
    snackbarHostState: SnackbarHostState
) {
    val state by node.state.collectAsState()
    CollectEffects(node) { effect ->
        if (effect is EditContactEffect.ShowMessage) {
            snackbarHostState.showSnackbar(effect.text)
        }
    }

    EditContactScreen(
        state = state,
        onEvent = node::onEvent,
        modifier = Modifier
    )
}
