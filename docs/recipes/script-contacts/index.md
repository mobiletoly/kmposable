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

Full implementation in `sample-app-flowscript`: the script loads contacts, drives details/editor flows, and refreshes list nodes while Compose UI stays simple.
