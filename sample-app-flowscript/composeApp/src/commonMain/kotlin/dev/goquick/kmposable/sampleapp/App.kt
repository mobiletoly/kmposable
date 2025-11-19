package dev.goquick.kmposable.sampleapp

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.goquick.kmposable.compose.NavFlowHost
import dev.goquick.kmposable.compose.NodeRenderer
import dev.goquick.kmposable.compose.nodeRenderer
import dev.goquick.kmposable.compose.rememberNavFlow
import dev.goquick.kmposable.sampleapp.contacts.Contact
import dev.goquick.kmposable.sampleapp.contacts.ContactDetailsNode
import dev.goquick.kmposable.sampleapp.contacts.ContactId
import dev.goquick.kmposable.sampleapp.contacts.ContactsFlowEvent
import dev.goquick.kmposable.sampleapp.contacts.ContactsListNode
import dev.goquick.kmposable.sampleapp.contacts.createContactsNavFlow
import dev.goquick.kmposable.sampleapp.contacts.EditContactNode
import dev.goquick.kmposable.sampleapp.contacts.InMemoryContactsRepository
import dev.goquick.kmposable.sampleapp.contacts.launchContactsScript
import dev.goquick.kmposable.sampleapp.contacts.ui.ContactDetailsScreen
import dev.goquick.kmposable.sampleapp.contacts.ui.ContactsListScreen
import dev.goquick.kmposable.sampleapp.contacts.ui.EditContactScreen
import dev.goquick.kmposable.sampleapp.contacts.ui.SettingsScreen
import dev.goquick.kmposable.sampleapp.settings.SettingsNode
import kotlinx.coroutines.CoroutineScope

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
        Scaffold(bottomBar = { AppBottomBar(navController) }) { paddingValues ->
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
    val nodeScopeState = remember { mutableStateOf<CoroutineScope?>(null) }
    val navFlow = rememberNavFlow(key = repository) { scope ->
        nodeScopeState.value = scope
        createContactsNavFlow(repository = repository, appScope = scope)
    }
    val nodeScope = nodeScopeState.value

    LaunchedEffect(navFlow, nodeScope) {
        if (nodeScope != null) {
            navFlow.launchContactsScript(this, nodeScope, repository)
        }
    }

    val renderer: NodeRenderer<ContactsFlowEvent> = remember {
        nodeRenderer {
            register<ContactsListNode> { node ->
                val state by node.state.collectAsState()
                ContactsListScreen(state = state, onEvent = node::onEvent)
            }
            register<ContactDetailsNode> { node ->
                val state by node.state.collectAsState()
                ContactDetailsScreen(state = state, onEvent = node::onEvent)
            }
            register<EditContactNode> { node ->
                val state by node.state.collectAsState()
                EditContactScreen(state = state, onEvent = node::onEvent)
            }
        }
    }

    NavFlowHost(navFlow = navFlow, renderer = renderer)
}

@Composable
private fun SettingsDestination() {
    val scope = rememberCoroutineScope()
    val node = remember(scope) { SettingsNode(scope) }
    val state by node.state.collectAsState()

    SettingsScreen(state = state, onEvent = node::onEvent)
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
