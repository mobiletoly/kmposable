package dev.goquick.kmposable.sampleapp.contacts.ui

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
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
fun ContactDetailsHost(node: ContactDetailsNode) {
    val state by node.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // One-shot effects (toast/snackbar) are handled here so the Screen stays stateless.
    CollectEffects(node) { effect ->
        when (effect) {
            is ContactDetailsEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.text)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        ContactDetailsScreen(
            state = state,
            onEvent = node::onEvent,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
fun EditContactHost(node: EditContactNode) {
    val state by node.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    CollectEffects(node) { effect ->
        when (effect) {
            is EditContactEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.text)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        EditContactScreen(
            state = state,
            onEvent = node::onEvent,
            modifier = Modifier.padding(paddingValues)
        )
    }
}
