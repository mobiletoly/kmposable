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

**Why**: keeps splash → sign-in → dashboard policy in one spot; Compose just renders whatever the
script shows. Tests call the same script via `FlowTestScenario.launchScript { … }`.
