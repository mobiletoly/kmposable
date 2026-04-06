---
layout: doc
title: Legacy: Compose NavHost + NavFlow
permalink: /cookbook/recipes/reactive-navhost/
---

This is a **legacy 0.2.x-style recipe**.

For `0.3.x`, the recommended app-shell architecture is **Navigation 3 KMP + kmposable**, not
Compose `NavHost` + kmposable.

This page is kept only for teams that deliberately stay on a non-Nav3 shell. If you are starting a
new Compose KMP app, prefer the [Navigation 3 KMP architecture page]({{ site.baseurl }}/navigation3-kmp/).

When to use:
- Apps that already use NavHost for top-level routes/tabs but want Kmposable flows per destination.
- You want headless flows reusable across platforms while keeping Android navigation at the edges.

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

    val renderer = remember { contactsRenderer() } // registers nodes to Hosts (not Screens)
    NavFlowHost(navFlow = navFlow, renderer = renderer)
}

private fun contactsRenderer(): NodeRenderer<ContactsFlowEvent> = nodeRenderer {
    register<ContactsListNode> { node -> ContactsListHost(node) }
    register<ContactDetailsNode> { node -> ContactDetailsHost(node) }
    register<EditContactNode> { node -> EditContactHost(node) }
}
```

### Why it still exists

- Keeps NavHost responsible for top-level navigation (tabs, deep links).
- Kmposable stays reusable/headless inside each destination.
- NavFlow navigation stays internal; NavHost navigation stays at the edges.
- Renderer wires nodes → Hosts so Screens stay pure UI. See [Layering](/cookbook/recipes/reactive-layering/).
