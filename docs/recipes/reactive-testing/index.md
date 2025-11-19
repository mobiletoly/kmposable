---
layout: doc
title: FlowTestScenario Assertions
permalink: /cookbook/recipes/reactive-testing/
---

Use `FlowTestScenario` to drive NavFlow without UI.

```kotlin
@Test
fun contacts_shows_details() = runTest {
    val factory = SimpleNavFlowFactory<ContactsFlowEvent> {
        ContactsNavFlow(fakeRepository, appScope = this)
    }

    factory.createTestScenario(this)
        .start()
        .send(ContactsEvent.Select(ContactId("1")))
        .awaitStackTags("ContactsList", "ContactDetails")
        .assertNoMoreOutputs()
        .finish()
}
```

Helpers:

- `start()` wires outputs and calls `NavFlow.start()`.
- `send(event)` injects events into the top node.
- `awaitTopNodeIs<T>()`, `awaitStackTags`, `awaitStackSize` synchronise tests with navigation.
- `awaitOutputOfType<T>()` waits for outputs emitted after the call begins.
- `launchScript(onTrace = …)` lets you reuse production scripts inside the same scenario.
