---
layout: doc
title: Script – Reusable Sub-flow
permalink: /cookbook/recipes/script-subflow/
---

```kotlin
suspend fun NavFlowScriptScope<AppOutput, *>.collectAddress(
    nodeScope: CoroutineScope
): Address = pushForResult(
    factory = { AddressFormNode(nodeScope) },
    mapper = { output ->
        when (output) {
            is AppOutput.AddressSubmitted -> output.address
            AppOutput.AddressCancelled -> null
            else -> null
        }
    }
)
```

Call `collectAddress()` from profile setup, checkout, etc. to keep the steps in one place.
