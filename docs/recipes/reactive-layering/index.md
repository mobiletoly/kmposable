---
layout: doc
title: Node → Host → Screen Layering
permalink: /cookbook/recipes/reactive-layering/
---

Keep the reactive stack readable and testable by splitting responsibilities:

- **Node** (headless, `contacts.flow`) – state/events/outputs/effects only. Pure Kotlin, shared with tests and other UIs.
- **Host** (Compose, `contacts.ui`) – collects state/effects, handles transient UI (snackbars/dialogs), wires DI/subflows, and forwards clean callbacks.
- **Screen** (Compose, `contacts.ui`) – pure UI, `state in → events out`; no side effects or DI.

When to use:
- Any flow where you want headless logic + thin UI glue + pure UI, including single-node tabs.
- Teams that need predictable layering and easy grep-able imports across features.

### Naming & packages

- Stick to `FooNode`, `FooHost`, `FooScreen`.
- Keep nodes/NavFlow in a `*.flow` package; keep UI/hosts in `*.ui`. This makes renderer imports and grep-friendly searches predictable.

### Example (from the sample app)

```kotlin
// contacts.flow
class ContactsNavFlow(...) : NavFlow<ContactsFlowEvent, DefaultStackEntry<ContactsFlowEvent>>(
    appScope = appScope,
    rootNode = ContactsListNode(repository, appScope),
    navigatorFactory = { entry -> KmposableStackNavigator(entry) }
)

// contacts.ui
@Composable
fun ContactsListHost(node: ContactsListNode) {
    val state by node.state.collectAsState()
    ContactsListScreen(state = state, onEvent = node::onEvent)
}

@Composable
fun ContactDetailsHost(node: ContactDetailsNode) {
    val state by node.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    CollectEffects(node) { effect ->
        if (effect is ContactDetailsEffect.ShowMessage) snackbarHostState.showSnackbar(effect.text)
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        ContactDetailsScreen(state, node::onEvent, Modifier.padding(padding))
    }
}

// App renderer wiring
val renderer = remember {
    nodeRenderer {
        register<ContactsListNode> { ContactsListHost(it) }
        register<ContactDetailsNode> { ContactDetailsHost(it) }
        register<EditContactNode> { EditContactHost(it) }
    }
}
NavFlowHost(navFlow = navFlow, renderer = renderer)
```

### Tips

- Put persistent errors in state; use effects for one-shot signals (snackbar/toast/nav).
- Result-only overlays: use `presentation = Presentation.Overlay` on nodes and render via `OverlayNavFlowHost`; register with `registerResultOnly`.
- Keep hosts thin; if wiring grows (subflows, DI helpers), extract small helpers rather than bloating screens.
- Tests exercise nodes/navflow directly via `FlowTestScenario`; screens stay trivial to preview.
