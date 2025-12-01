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

When to use:
- Any sub-flow you want to reuse across multiple scripts (address, picker, auth challenge).

Why it matters:
- Reusable sub-flow keeps address collection consistent across features.
- `pushForResult` handles push → await → pop, so callers stay compact.
