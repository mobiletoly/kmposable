---
layout: doc
title: Single-Screen Node
permalink: /cookbook/recipes/reactive-single-screen/
---

Some screens don’t need NavFlow. Render the node directly.

```kotlin
@Composable
fun SettingsDestination() {
    val scope = rememberCoroutineScope()
    val node = remember { SettingsNode(scope) }
    val state by node.state.collectAsState()

    SettingsScreen(state = state, onEvent = node::onEvent)
}
```

- `SettingsNode` extends `StatefulNode`, so it already handles state/output.
- Use `remember` to keep the node instance stable across recompositions.
- Send events by calling `node.onEvent` directly.
- Great for tabs like “Settings” or “Profile” where there’s no internal navigation.
