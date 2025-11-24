# Kmposable

[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-blue?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![Maven Central](https://img.shields.io/maven-central/v/dev.goquick.kmposable/core?logo=apache-maven&label=Maven%20Central)](https://central.sonatype.com/artifact/dev.goquick.kmposable/core)
[![CI](https://img.shields.io/github/actions/workflow/status/mobiletoly/kmposable/gradle.yml?branch=main&logo=github&label=CI)](https://github.com/mobiletoly/kmposable/actions/workflows/gradle.yml)
[![License](https://img.shields.io/github/license/mobiletoly/kmposable?logo=apache&label=License)](LICENSE)

A **UI-agnostic navigation + flow engine** for Kotlin Multiplatform.  
Kmposable lets you structure your app as pure, testable **Nodes** (state + events + outputs).  
Compose UI becomes a thin rendering layer on top.

> **Build your entire app flow without UI.  
> Add UI later.  
> Test everything headlessly.**

---

# Why Kmposable?

Modern Compose apps often struggle with:

- Navigation tightly coupled to UI
- Bloated ViewModels
- Hard-to-test flows (list → details → edit → confirm…)
- Reusing flows across platforms or screens
- Business logic hiding inside UI code

**Kmposable fixes all of this** by giving you:

### ✔ Pure business logic (Nodes)

State + events + outputs. No UI code.

### ✔ A tiny NavFlow engine that manages navigation

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

Runs the flow, drives navigation, exposes state.

```
Node (state, events, outputs)
        |
        | outputs
        v
Navigator <---- NavFlow ----> UI Renderer
```

That’s the core of Kmposable.

---

# Documentation & Guides

Detailed docs now live at **https://mobiletoly.github.io/kmposable**:

- [Overview](https://mobiletoly.github.io/kmposable/overview/) – philosophy, modules, samples.
- [Hello Kmposable](https://mobiletoly.github.io/kmposable/guides/hello-kmposable/) – first flow end-to-end.
- [Guides](https://mobiletoly.github.io/kmposable/guides/) – Compose integration, testing, patterns.
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
navFlow.sendEvent(CounterEvent.Increment)
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
`navFlow.launchNavFlowScript { … }` or the DSL-friendly `navFlow.runFlow { step("name") { … } }`.
See the docs for full walkthroughs.

# Samples

- `sample-app-compose` — Compose Multiplatform app with NavHost tabs + Kmposable flows.
- `sample-app-flowscript` — Same UI but orchestrated via a NavFlow script.

Both samples include READMEs with run/test instructions.

# Support

- Maven Central: `dev.goquick.kmposable:*`
- Docs: https://mobiletoly.github.io/kmposable
- Issues / PRs: [GitHub](https://github.com/mobiletoly/kmposable)
- Specs/RFCs: [`spec_docs/`](spec_docs)
