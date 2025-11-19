---
layout: doc
title: Renderer Patterns
permalink: /cookbook/recipes/reactive-renderers/
---

Keep renderers maintainable by splitting registrations per feature.

```kotlin
fun contactsRenderer(): NodeRenderer<ContactsFlowEvent> = nodeRenderer {
    registerContactsList()
    registerContactDetails()
    registerEditContact()
}

private fun NodeRendererBuilder<ContactsFlowEvent>.registerContactsList() =
    register<ContactsListNode> { node ->
        val state by node.state.collectAsState()
        ContactsListScreen(state = state, onEvent = node::onEvent)
    }
```

Tips:

- Always wrap renderer creation in `remember { … }` so Compose keeps it stable.
- Group registrations by feature to avoid giant lambdas.
- If you need DI/ViewModel access, grab the dependency outside the renderer and pass it to the nodes when building the NavFlow.
- Consider exposing renderer builders per feature module so code is reusable across apps.
