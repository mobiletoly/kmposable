---
layout: doc
title: Script – Auth Retry with Backoff
permalink: /cookbook/recipes/script-retry/
---

Retries, timeouts, and transient error UIs belong in a script, not scattered across nodes.

When to use:
- Auth flows that need retries/backoff/timeout and transient error UIs.
- Anytime you want retry policy to be testable headlessly.

```kotlin
sealed interface AuthOutput {
    data class Success(val userId: String) : AuthOutput
    data class Error(val reason: String) : AuthOutput
    data object RetryTapped : AuthOutput
}

// Headless node just emits outputs; no retry policy inside.
class SignInNode(parentScope: CoroutineScope) :
    EffectfulStatefulNode<SignInState, SignInEvent, AuthOutput, SignInEffect>(
        parentScope = parentScope,
        initialState = SignInState()
    ) { /* ... */ }

private suspend fun NavFlowScriptScope<AuthOutput, *>.runSignInFlow(
    authRepository: AuthRepository,
    scope: CoroutineScope
) {
    showRoot { SignInNode(scope) }

    repeat(3) { attempt ->
        val result = withTimeoutOrNull(10_000) { authRepository.signIn() }
        if (result != null) {
            showRoot { SuccessNode(scope) }
            return
        }

        val delayMs = 1_000L * (attempt + 1)
        trace { "auth retry attempt=${attempt + 1}, backoff=${delayMs}ms" }
        pushNode { ErrorBannerNode(scope, attempt = attempt + 1, backoffMs = delayMs) }
        awaitOutputOfType<AuthOutput.RetryTapped>()
        navFlow.pop()
        delay(delayMs)
    }

    showRoot { PermanentFailureNode(scope) }
}
```

Why it matters:
- Retry/backoff/timeout policy lives in one coroutine, not inside UI nodes.
- Nodes stay headless (emit outputs only), while the script decides when to show error banners and when to give up.
- `trace` + `withTimeoutOrNull` make the flow observable and testable via `FlowTestScenario.launchScript`.
