# Kmposable

[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-blue?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![Maven Central](https://img.shields.io/maven-central/v/dev.goquick.kmposable/core?logo=apache-maven&label=Maven%20Central)](https://central.sonatype.com/artifact/dev.goquick.kmposable/core)
[![CI](https://img.shields.io/github/actions/workflow/status/mobiletoly/kmposable/gradle.yml?branch=main&logo=github&label=CI)](https://github.com/mobiletoly/kmposable/actions/workflows/gradle.yml)
[![License](https://img.shields.io/github/license/mobiletoly/kmposable?logo=apache&label=License)](LICENSE)

A **workflow/runtime layer** for Kotlin Multiplatform features.  
Kmposable lets you structure feature logic as pure, testable **Nodes** (state + events + outputs)
and run them headlessly with `NavFlow`.  
For Compose KMP apps, the recommended architecture is **Navigation 3 KMP for the app shell** and
**kmposable for inner feature workflows**.

> Build feature workflows without UI.  
> Plug them into Navigation 3 KMP or another shell later.  
> Test the business logic headlessly.

---

# Why Kmposable?

Modern Compose apps often struggle with:

- Feature logic tightly coupled to UI navigation
- Bloated ViewModels
- Hard-to-test flows (list → details → edit → confirm…)
- Reusing flows across platforms or screens
- Business logic hiding inside UI code

**Kmposable fixes all of this** by giving you:

### ✔ Pure business logic (Nodes)

State + events + outputs. No UI code.

### ✔ A headless NavFlow runtime that manages feature flow state

Push, pop, replace — all headless.

### ✔ Full flow testing without UI

Use `FlowTestScenario` to test deep navigation and logic headlessly (e.g., `awaitTopNodeIs<DetailsNode>()`, `awaitStackTags("root", "details")`).

### ✔ Clean UI adapters

Compose just *observes* state from your nodes.

# The Mental Model (20 Seconds)

You only need three concepts:

### **1. Node**

A screen/feature logic unit:  
`state + events + outputs`.

### **2. Navigator**

Manages a stack of nodes.

### **3. NavFlow**

Runs the feature workflow, drives node transitions, exposes state.

```
Node (state, events, outputs)
        |
        | outputs
        v
Navigator <---- NavFlow ----> UI Renderer
```

That’s the core of Kmposable.

---

# Recommended Architecture

For `0.3.x`, the primary Compose story is:

- Navigation 3 KMP owns app routes, outer back stack, save/restore, and scene composition.
- Kmposable owns feature-local workflow state, node orchestration, outputs, and headless tests.

Use `library-navigation3` to host kmposable flows inside Navigation 3 destinations.

## When To Use Kmposable

- You want headless, deterministic feature workflows that survive outside Compose.
- You want the same feature logic reusable across Android, iOS, desktop, or tests.
- You want a thin UI layer that renders node state instead of owning the flow.

## When Not To Use Kmposable

- You only need app-level routing and standard destination screens.
- Your feature logic is trivial enough that a full workflow runtime adds no value.
- You want kmposable to replace Navigation 3 as the app-shell router in a Compose KMP app.

## Compose Paths

- `Navigation 3 KMP + kmposable`
  Recommended for new Compose KMP apps.
- `Non-Nav3 Compose + kmposable`
  Supported fallback for teams that are not on Navigation 3 yet or intentionally want a smaller setup.

# Documentation & Guides

Detailed docs now live at **https://mobiletoly.github.io/kmposable**:

- [Overview](https://mobiletoly.github.io/kmposable/overview/) – philosophy, modules, samples.
- [Hello Kmposable](https://mobiletoly.github.io/kmposable/guides/hello-kmposable/) – first flow end-to-end.
- [Guides](https://mobiletoly.github.io/kmposable/guides/) – Compose integration, testing, patterns.
- [Navigation 3 KMP Architecture](https://mobiletoly.github.io/kmposable/navigation3-kmp/) – recommended `0.3.x` app-shell split.
- [Non-Nav3 Compose Hosting](https://mobiletoly.github.io/kmposable/non-nav3-compose/) – supported fallback path for standalone Compose hosting.
- [Nav3 vs Non-Nav3](https://mobiletoly.github.io/kmposable/compose-paths/) – which path to choose.
- [0.2.x -> 0.3.x Migration](https://mobiletoly.github.io/kmposable/migration-0-2-to-0-3/) – breaking changes and migration path.
- [NavFlow Scripts](https://mobiletoly.github.io/kmposable/guides/flowscripts/) – sequential orchestration.
- [Cookbook](https://mobiletoly.github.io/kmposable/cookbook/) – reactive + script recipes.
- [Reference](https://mobiletoly.github.io/kmposable/reference/) – API summaries for core/compose/test.
- [Specs](https://mobiletoly.github.io/kmposable/specs/) – direct links to the RFCs in `spec_docs/`.

# Quick Peek

Define a node once, render or test it anywhere:

```kotlin
data class CounterState(val value: Int = 0)

sealed interface CounterEvent {
    object Increment : CounterEvent
    object Decrement : CounterEvent
}

class CounterNode(parentScope: CoroutineScope) :
    StatefulNode<CounterState, CounterEvent, Nothing>(parentScope, CounterState()) {
    override fun onEvent(event: CounterEvent) {
        when (event) {
            CounterEvent.Increment -> updateState { it.copy(value = it.value + 1) }
            CounterEvent.Decrement -> updateState { it.copy(value = it.value - 1) }
        }
    }
}

val navFlow = NavFlow(scope, CounterNode(scope)).apply { start() }
navFlow.updateTopNode<CounterNode> { onEvent(CounterEvent.Increment) }
```

Hook it up to Compose with a renderer:

```kotlin
@Composable
fun CounterScreen() {
    val navFlow = rememberNavFlow { scope -> NavFlow(scope, CounterNode(scope)) }
    val renderer = remember {
        nodeRenderer<Nothing> {
            register<CounterNode> { node ->
                val state by node.state.collectAsState()
                CounterUi(state.value) { node.onEvent(CounterEvent.Increment) }
            }
        }
    }
    NavFlowHost(navFlow = navFlow, renderer = renderer)
}
```

Tests reuse the exact same flow via `SimpleNavFlowFactory` + `FlowTestScenario`. Scripts reuse it via
`navFlow.runScript { … }` (alias for `launchNavFlowScript`).
See the docs for full walkthroughs.

# Samples

- `sample-app-compose` — Compose Multiplatform app with a Navigation 3 KMP shell and inner kmposable flow.
- `sample-app-flowscript` — Same UI but orchestrated via a NavFlow script.

Both samples include READMEs with run/test instructions.

# Support

- Maven Central: `dev.goquick.kmposable:*`
- Docs: https://mobiletoly.github.io/kmposable
- Issues / PRs: [GitHub](https://github.com/mobiletoly/kmposable)
- Specs/RFCs: [`spec_docs/`](spec_docs)
