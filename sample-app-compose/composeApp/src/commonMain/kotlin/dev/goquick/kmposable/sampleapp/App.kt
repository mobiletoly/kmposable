package dev.goquick.kmposable.sampleapp

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import dev.goquick.kmposable.compose.NavFlowHost
import dev.goquick.kmposable.compose.nodeRenderer
import dev.goquick.kmposable.compose.rememberNode
import dev.goquick.kmposable.navigation3.CollectNavFlowOutputs
import dev.goquick.kmposable.navigation3.rememberKmposableNavEntryDecorators
import dev.goquick.kmposable.navigation3.rememberNavigation3NavFlow
import dev.goquick.kmposable.sampleapp.contacts.Contact
import dev.goquick.kmposable.sampleapp.contacts.ContactId
import dev.goquick.kmposable.sampleapp.contacts.InMemoryContactsRepository
import dev.goquick.kmposable.sampleapp.contacts.flow.ContactDetailsNode
import dev.goquick.kmposable.sampleapp.contacts.flow.ContactsFlowEvent
import dev.goquick.kmposable.sampleapp.contacts.flow.ContactsListNode
import dev.goquick.kmposable.sampleapp.contacts.flow.ContactsNavFlow
import dev.goquick.kmposable.sampleapp.contacts.flow.EditContactNode
import dev.goquick.kmposable.sampleapp.contacts.ui.ContactDetailsHost
import dev.goquick.kmposable.sampleapp.contacts.ui.ContactsListHost
import dev.goquick.kmposable.sampleapp.contacts.ui.EditContactHost
import dev.goquick.kmposable.sampleapp.settings.SettingsHost
import dev.goquick.kmposable.sampleapp.settings.SettingsNode

// This sample intentionally follows the Node → Host → Screen layering:
// - Nodes live in shared logic and emit state/effects/outputs.
// - Hosts live in Compose, collect node state/effects, and pass clean callbacks to Screens.
// - Screens are pure UI (state in, events out), making them easy to preview/reuse.
@Composable
fun App() {
    val repository = remember {
        InMemoryContactsRepository(
            listOf(
                Contact(ContactId("1"), "Alice", "123", "alice@example.com"),
                Contact(ContactId("2"), "Bob", "456", null)
            )
        )
    }
    val backStack = rememberNavBackStack(sampleAppNavConfiguration, ContactsRoute)
    val navigationController = remember(backStack) {
        SampleAppNavigationController(backStack)
    }
    val entryDecorators = rememberKmposableNavEntryDecorators<NavKey>()

    MaterialTheme {
        Scaffold { _ ->
            NavDisplay(
                backStack = backStack,
                entryDecorators = entryDecorators,
                entryProvider = { route ->
                    when (route) {
                        ContactsRoute -> NavEntry(route) {
                            ContactsDestination(
                                repository = repository,
                                onFlowOutput = navigationController::onContactsOutput,
                            )
                        }
                        SettingsRoute -> NavEntry(route) {
                            SettingsDestination(onNavigateBack = navigationController::navigateBack)
                        }
                        else -> error("Unsupported route: $route")
                    }
                }
            )
        }
    }
}

@Composable
private fun ContactsDestination(
    repository: InMemoryContactsRepository,
    onFlowOutput: (ContactsFlowEvent) -> Unit,
) {
    val navFlow = rememberNavigation3NavFlow<ContactsFlowEvent> { scope ->
        ContactsNavFlow(repository = repository, appScope = scope)
    }
    val snackbarHostState = remember { SnackbarHostState() }

    // Renderer maps nodes → hosts. Hosts then wire state/effects to pure Screens.
    val renderer = remember {
        nodeRenderer {
            register<ContactsListNode> { node -> ContactsListHost(node) }
            register<ContactDetailsNode> { node -> ContactDetailsHost(node, snackbarHostState) }
            registerResultOnly<EditContactNode> { node -> EditContactHost(node, snackbarHostState) }
        }
    }
    CollectNavFlowOutputs(navFlow = navFlow, onOutput = onFlowOutput)

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { _ ->
        NavFlowHost(navFlow = navFlow, renderer = renderer)
    }
}

@Composable
private fun SettingsDestination(onNavigateBack: () -> Boolean) {
    val node = rememberNode { scope -> SettingsNode(parentScope = scope) }

    SettingsHost(
        node = node,
        onNavigateBack = { onNavigateBack() },
    )
}
