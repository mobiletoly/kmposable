---
layout: page
title: NavFlow Scripts
permalink: /guides/flowscripts/
---

NavFlow scripts let you orchestrate complex flows as coroutines: `showRoot`, `pushNode`, wait for
outputs, emit trace logs, and finish when the story is done. They leave nodes focused on UI state.

## Launching a Script

```kotlin
class OnboardingViewModel(
    private val repository: AuthRepository
) : ViewModel() {
    val navFlow = NavFlow(viewModelScope, SplashNode(viewModelScope)).apply { start() }

    private val job = navFlow.runFlow(viewModelScope, onTrace = { println(it) }) {
        step("Onboarding") {
            showRoot { SplashNode(viewModelScope) }
            awaitOutputOfType<OnboardingOutput.SplashFinished>()

            showRoot { SignInNode(viewModelScope) }
            when (awaitOutputOfType<OnboardingOutput.AuthResult>()) {
                OnboardingOutput.AuthResult.Success -> showRoot { DashboardNode(viewModelScope) }
                is OnboardingOutput.AuthResult.Error -> showRoot { ErrorNode(viewModelScope) }
            }
            finish()
        }
    }

    override fun onCleared() {
        job.cancel()
        super.onCleared()
    }
}
```

You can still use the lower-level entry point if you prefer:

```kotlin
val job = navFlow.launchNavFlowScript(viewModelScope, onTrace = { println(it) }) {
    // same flow logic as above
}
```

## Script Scope Helpers

Inside `NavFlowScriptScope` you get:

| Helper | Description |
| --- | --- |
| `showRoot { node }`, `pushNode { node }`, `replaceTop { node }` | Stack operations without touching navigator internals. |
| `awaitOutputOfType<T>()`, `awaitOutputCase { … }`, `awaitMappedOutput { … }` | Wait for future outputs. |
| `withNode { … }`, `pushForResult { … }` | Push a temporary node, run work, pop automatically. |
| `runCatchingNodeCall` | Show loading/success/error states while performing suspend work. |
| `trace { "message" }` | Emit debug logs via the `onTrace` callback passed to `launchNavFlowScript`. |

## Testing Scripts

Use `FlowTestScenario`:

```kotlin
val scenario = factory.createTestScenario(this).start()
val job = scenario.launchScript { runContactsScript(fakeRepo, this@runTest) }
scenario.awaitTopNodeIs<ContactDetailsNode>()
job.cancelAndJoin()
```

For full recipes (contacts flow, checkout, retries, reusable sub-flows) see the
[Cookbook]({{ site.baseurl }}/cookbook/).
