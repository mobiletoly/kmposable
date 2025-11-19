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
    scenario.send(ContactsEvent.Select(ContactId("42")))

    scenario.awaitTopNodeIs<ContactDetailsNode>()
    job.cancelAndJoin()
}
```

`FlowTestScenario.launchScript { … }` reuses the production script. Combine with `awaitStackTags`, `awaitOutputOfType`, etc. for full coverage.
