package dev.goquick.kmposable.sampleapp

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.goquick.kmposable.compose.NavFlowHost
import dev.goquick.kmposable.compose.nodeRenderer
import dev.goquick.kmposable.compose.rememberNode
import dev.goquick.kmposable.compose.viewmodel.rememberNavFlowViewModel
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
    val navController = rememberNavController()
    val repository = remember {
        InMemoryContactsRepository(
            listOf(
                Contact(ContactId("1"), "Alice", "123", "alice@example.com"),
                Contact(ContactId("2"), "Bob", "456", null)
            )
        )
    }

    MaterialTheme {
        Scaffold(
            bottomBar = { AppBottomBar(navController) }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = AppRoute.Contacts.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(AppRoute.Contacts.route) {
                    ContactsDestination(repository)
                }
                composable(AppRoute.Settings.route) {
                    SettingsDestination()
                }
            }
        }
    }
}

@Composable
private fun ContactsDestination(
    repository: InMemoryContactsRepository
) {
    // Use a ViewModel-backed flow so the nav stack survives configuration changes.
    val navFlowVm = rememberNavFlowViewModel<ContactsFlowEvent> { scope ->
        ContactsNavFlow(repository = repository, appScope = scope)
    }
    val navFlow = navFlowVm.navFlow

    // Renderer maps nodes → hosts. Hosts then wire state/effects to pure Screens.
    val renderer = remember {
        nodeRenderer {
            register<ContactsListNode> { node -> ContactsListHost(node) }
            register<ContactDetailsNode> { node -> ContactDetailsHost(node) }
            register<EditContactNode> { node -> EditContactHost(node) }
        }
    }

    NavFlowHost(navFlow = navFlow, renderer = renderer)
}

@Composable
private fun SettingsDestination() {
    val node = rememberNode { scope -> SettingsNode(parentScope = scope) }

    SettingsHost(node)
}

@Composable
private fun AppBottomBar(navController: NavHostController) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val currentRoute = currentDestination?.route
    NavigationBar {
        AppRoute.bottomTabs.forEach { tab ->
            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Text(tab.shortLabel) },
                label = { Text(tab.label) }
            )
        }
    }
}

private sealed class AppRoute(val route: String) {
    data object Contacts : AppRoute("contacts")
    data object Settings : AppRoute("settings")

    companion object {
        val bottomTabs = listOf(
            BottomTab(route = Contacts.route, label = "Contacts", shortLabel = "C"),
            BottomTab(route = Settings.route, label = "Settings", shortLabel = "S")
        )
    }

    data class BottomTab(val route: String, val label: String, val shortLabel: String)
}
