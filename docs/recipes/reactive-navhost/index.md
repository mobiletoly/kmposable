---
layout: doc
title: Compose + NavHost + NavFlow
permalink: /cookbook/recipes/reactive-navhost/
---

Full example from `sample-app-compose`: NavHost owns tabs/routes while Kmposable powers inner flows.

This mirrors the structure in `sample-app-compose`: NavHost owns tabs/routes while Kmposable handles
inner flows.

```kotlin
@Composable
fun App() {
    val navController = rememberNavController()
    val repository = remember { InMemoryContactsRepository(seedData) }

    MaterialTheme {
        Scaffold(bottomBar = { AppBottomBar(navController) }) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = "contacts",
                modifier = Modifier.padding(paddingValues)
            ) {
                composable("contacts") {
                    ContactsDestination(repository, navController)
                }
                composable("settings") {
                    SettingsDestination()
                }
            }
        }
    }
}
```

### Contacts destination

```kotlin
@Composable
fun ContactsDestination(
    repository: InMemoryContactsRepository,
    navController: NavHostController
) {
    val navFlow = rememberNavFlow(key = repository) { scope ->
        ContactsNavFlow(repository = repository, appScope = scope)
    }

    val renderer = remember {
        nodeRenderer<ContactsFlowEvent> {
            register<ContactsListNode> { node ->
                val state by node.state.collectAsState()
                ContactsListScreen(state = state, onEvent = node::onEvent)
            }
            register<ContactDetailsNode> { node ->
                val state by node.state.collectAsState()
                ContactDetailsScreen(state = state, onEvent = node::onEvent)
            }
        }
    }

    LaunchedEffect(navFlow) {
        navFlow.outputs.collect { output ->
            when (output) {
                is ContactsFlowEvent.OpenContact ->
                    navController.navigate("contactDetails/${output.id}")
                ContactsFlowEvent.Done -> navController.popBackStack()
            }
        }
    }

    NavFlowHost(navFlow = navFlow, renderer = renderer)
}
```

### Why it matters

- Keeps NavHost responsible for top-level navigation (tabs, deep links).
- Kmposable stays reusable/headless inside each destination.
- Outputs map directly to NavController actions, no wrapper interfaces.
