---
layout: doc
title: Single-Screen Node
permalink: /cookbook/recipes/reactive-single-screen/
---

Some screens don’t need NavFlow. Render the node directly (Node → Host → Screen still applies).

When to use:
- Tabs like Settings/Profile where there’s no inner stack.
- You still want the Node → Host → Screen convention for consistency.

```kotlin
@Composable
fun SettingsDestination() {
    val scope = rememberCoroutineScope()
    val node = remember { SettingsNode(scope) }

    // Host collects state/effects; Screen stays pure UI.
    val state by node.state.collectAsState()

    SettingsScreen(state = state, onEvent = node::onEvent)
}
```

- `SettingsNode` extends `StatefulNode`, so it already handles state/output.
- Use `remember` to keep the node instance stable across recompositions.
- Send events by calling `node.onEvent` directly.
- Great for tabs like “Settings” or “Profile” where there’s no internal navigation.
- If you prefer symmetry with the rest of your app, add a tiny `SettingsHost` and register it in your renderer; the layering stays consistent even without NavFlow.
