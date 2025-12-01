---
layout: doc
title: Script – Branching Wizard + Tracing
permalink: /cookbook/recipes/script-wizard/
---

Show how to keep multi-step wizards readable, branch on outputs, and emit traces/analytics in one place.

When to use:
- Multi-step flows with clear branches (success vs cancel vs retry) where analytics/traces matter.
- You want to keep branch logic centralized instead of scattering it across nodes.

```kotlin
sealed interface WizardOutput {
    data class StepACompleted(val data: A) : WizardOutput
    data class StepBCompleted(val data: B) : WizardOutput
    data object Cancelled : WizardOutput
}

fun NavFlow<WizardOutput, *>.launchWizard(scope: CoroutineScope, onTrace: (String) -> Unit) =
    launchNavFlowScript(scope, onTrace = onTrace) {
        trace { "wizard: start" }
        showRoot { StepANode(scope) }

        val stepA = awaitOutputOfType<WizardOutput.StepACompleted>().data
        trace { "wizard: stepA done" }

        showRoot { StepBNode(scope, stepA) }
        when (val stepB = awaitOutputCase<WizardResult> {
            on<WizardOutput.StepBCompleted> { WizardResult.Done(it.data) }
            on<WizardOutput.Cancelled> { WizardResult.Cancel }
        }) {
            is WizardResult.Done -> {
                trace { "wizard: success" }
                showRoot { SuccessNode(scope, stepB.data) }
            }
            WizardResult.Cancel -> {
                trace { "wizard: cancelled" }
                showRoot { CancelledNode(scope) }
            }
        }
    }
```

Why it matters:
- Branching and analytics/tracing live in one coroutine instead of scattered across nodes.
- Each step node stays focused on its UI/state; the script decides the story (retry, cancel, proceed).
- Tests can assert the trace or navigation using `FlowTestScenario.launchScript` + `awaitStackTags`.
