---
layout: doc
title: Script – Network Retry
permalink: /cookbook/recipes/script-retry/
---

```kotlin
private suspend fun NavFlowScriptScope<AuthOutput, *>.runSignInFlow(
    authRepository: AuthRepository,
    scope: CoroutineScope
) {
    repeat(3) { attempt ->
        val result = runCatching { authRepository.signIn() }
        if (result.isSuccess) {
            pushNode { SuccessNode(scope) }
            return
        }

        pushNode { ErrorBannerNode(scope, attempt = attempt + 1) }
        awaitOutputOfType<AuthOutput.BannerDismissed>()
        navFlow.pop()
    }

    showRoot { PermanentFailureNode(scope) }
}
```

- Shows how to push temporary nodes (success banner, error banner) and pop them when done.
- Retries stay in one place rather than scattered across nodes.
