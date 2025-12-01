---
layout: doc
title: FlowTestScenario Assertions
permalink: /cookbook/recipes/reactive-testing/
---

Use `FlowTestScenario` to drive NavFlow without UI.

When to use:
- Fast headless tests for navigation/outputs without emulators or Compose.

```kotlin
@Test
fun contacts_shows_details() = runTest {
    val factory = SimpleNavFlowFactory<ContactsFlowEvent> {
        ContactsNavFlow(fakeRepository, appScope = this)
    }

    val scenario = factory.createTestScenario(this).start()

    scenario.awaitTopNodeIs<ContactsListNode>()
    scenario.send(ContactsListEvent.ContactClicked(ContactId("1")))

    scenario.awaitTopNodeIs<ContactDetailsNode>()
    scenario.finish()
}
```

Helpers:

- `start()` wires outputs and calls `NavFlow.start()`.
- `send(event)` injects events into the top node.
- `awaitTopNodeIs<T>()`, `awaitStackTags`, `awaitStackSize` synchronise tests with navigation.
- `awaitOutputOfType<T>()` waits for outputs emitted after the call begins.
- `launchScript(onTrace = …)` lets you reuse production scripts inside the same scenario.

Why it matters:
- Exercises NavFlow + nodes headlessly (no Compose/emulator), catching navigation regressions fast.
- Same helpers you’d use in scripts keep test code terse and aligned with production behaviour.
