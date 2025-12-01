---
layout: doc
title: Renderer Patterns
permalink: /cookbook/recipes/reactive-renderers/
---

Keep renderers maintainable by splitting registrations per feature.

When to use:
- Any NavFlow with multiple nodes; avoid giant inline lambdas by grouping registrations.

```kotlin
fun contactsRenderer(): NodeRenderer<ContactsFlowEvent> = nodeRenderer {
    registerContactsList()
    registerContactDetails()
    registerEditContact()
}

private fun NodeRendererBuilder<ContactsFlowEvent>.registerContactsList() =
    register<ContactsListNode> { node ->
        ContactsListHost(node) // Host collects state/effects; Screen stays pure UI
    }
```

Tips:

- Always wrap renderer creation in `remember { … }` so Compose keeps it stable.
- Group registrations by feature to avoid giant lambdas.
- If you need DI/ViewModel access, grab the dependency outside the renderer and pass it to the nodes when building the NavFlow.
- Consider exposing renderer builders per feature module so code is reusable across apps.
- Register Hosts, not Screens, so you keep the Node → Host → Screen layering intact. See [Layering](/cookbook/recipes/reactive-layering/).
