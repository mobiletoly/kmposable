---
layout: doc
title: Script – Contacts Flow
permalink: /cookbook/recipes/script-contacts/
---

```kotlin
suspend fun NavFlowScriptScope<ContactsOutput, *>.runContactsFlow(
    repository: ContactsRepository,
    nodeScope: CoroutineScope
) {
    while (true) {
        showContactsList(repository)
        when (val action = awaitOutputCase<ListAction> {
            on<ContactsOutput.OpenContact> { ListAction.Open(it.id) }
            on<ContactsOutput.CreateContact> { ListAction.Create }
        }) {
            is ListAction.Open -> runDetailsFlow(action.id, repository, nodeScope)
            ListAction.Create -> runEditorFlow(null, repository, nodeScope)
        }
    }
}
```

When to use:
- Reusable list/details/editor flows where UI should stay reactive and navigation policy stays central.

Why it matters:
- The list/details/editor policy stays in one loop; Compose just renders the current node.
- Shows how to reuse the same editor sub-flow for “open contact” and “create contact.”
- Full implementation in `sample-app-flowscript` includes refresh + sub-flow reuse without UI branching.
