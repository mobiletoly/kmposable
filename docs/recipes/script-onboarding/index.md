---
layout: doc
title: Script – Onboarding Wizard
permalink: /cookbook/recipes/script-onboarding/
---

```kotlin
fun NavFlow<OnboardingOutput, *>.launchOnboardingScript(
    scope: CoroutineScope,
    viewModelScope: CoroutineScope
) = launchNavFlowScript(scope) {
    showRoot { SplashNode(viewModelScope) }

    awaitOutputOfType<OnboardingOutput.SplashFinished>()

    showRoot { SignInNode(viewModelScope) }
    when (awaitOutputOfType<OnboardingOutput.AuthResult>()) {
        OnboardingOutput.AuthResult.Success -> showRoot { DashboardNode(viewModelScope) }
        is OnboardingOutput.AuthResult.Error -> showRoot { ErrorNode(viewModelScope) }
    }
}
``` 

When to use:
- Linear onboarding “stories” (splash → auth → dashboard) where policy belongs in one coroutine.

Why it matters:
- Keeps splash → sign-in → dashboard policy in one spot; Compose just renders whatever the script shows.
- Nodes stay dumb (emit outputs only); the script owns the story and branching.
- Uses the same script in tests via `FlowTestScenario.launchScript { … }` for parity.
