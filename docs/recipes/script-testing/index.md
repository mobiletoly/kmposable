---
layout: doc
title: Script – Testing
permalink: /cookbook/recipes/script-testing/
---

```kotlin
@Test
fun contacts_script_opens_details() = runTest {
    val scenario = contactsFactory.createTestScenario(this).start()

    val job = scenario.launchScript { runContactsFlow(fakeRepository, this@runTest) }
    scenario.send(ContactsListEvent.ContactClicked(ContactId("42")))

    scenario.awaitTopNodeIs<ContactDetailsNode>()
    job.cancelAndJoin()
}
```

When to use:
- Validate scripts headlessly; assert navigation and outputs without UI/emulators.

`FlowTestScenario.launchScript { … }` reuses the production script. Combine with `awaitStackTags`, `awaitOutputOfType`, etc. for full coverage.

Why it matters:
- You test the exact script shipped to production (no duplicating logic).
- Scenario APIs (`awaitTopNodeIs`, `awaitStackTags`, `awaitOutputOfType`) keep tests synchronous and readable.
