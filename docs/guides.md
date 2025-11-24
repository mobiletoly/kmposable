---
layout: page
title: Guides
permalink: /guides/
---

## Start Here

- **[Hello Kmposable]({{ site.baseurl }}/guides/hello-kmposable/)** – the smallest flow: install, node, NavFlow,
  Compose host, headless test.
- **Core Guides** (below) – deep dives into Compose integration, scripts, testing, and advanced patterns.

## 1. Getting Started

### Install

```kotlin
dependencies {
    implementation("dev.goquick.kmposable:core:<version>")
    implementation("dev.goquick.kmposable:compose:<version>")
    testImplementation("dev.goquick.kmposable:test:<version>")
}
```

Replace `<version>` with the latest release from Maven Central. `core` is mandatory, `compose` is
needed only if you render with Jetpack Compose, and `test` pulls in `FlowTestScenario` utilities.

### Smallest NavFlow

A NavFlow is a stack of **Nodes**. Each node is a tiny state machine:

- `STATE` – immutable UI state exposed via `StateFlow`
- `EVENT` – inputs from UI (`onEvent(event)`)
- `OUTPUT` – signals sent upward (navigation/results). Use `Nothing` if the node never emits outputs.

Flow of data: **UI → EVENT → Node updates STATE → (optional) OUTPUT → NavFlow reacts (push/pop/replace).**

`StatefulNode<STATE, EVENT, OUTPUT>` generics map to your types: `STATE` is the immutable state
(`StateFlow<STATE>`), `EVENT` is what the node accepts via `onEvent` (usually UI/user actions), and
`OUTPUT` is what the node emits upward to a parent flow/runtime (navigation/results). Use `Nothing`
when the node does not emit outputs.

```kotlin
class CounterNode(parentScope: CoroutineScope) :
    StatefulNode<CounterState, CounterEvent, Nothing>(parentScope, CounterState()) {
    override fun onEvent(event: CounterEvent) {
        when (event) {
            CounterEvent.Increment -> updateState { it.copy(value = it.value + 1) }
            CounterEvent.Decrement -> updateState { it.copy(value = it.value - 1) }
        }
    }
}

val navFlow = NavFlow(appScope = scope, rootNode = CounterNode(scope)).apply { start() }
navFlow.sendEvent(CounterEvent.Increment)
println(navFlow.navState.value.top.state.value) // 1
```

How this reaches UI:
- UI observes `node.state` (e.g., `collectAsState()` in Compose) and renders it.
- UI sends user actions back as `EVENT`s via `node.onEvent(...)` or `navFlow.sendEvent(...)`.
- If a node emits `OUTPUT`, NavFlow (or a parent) uses it to navigate or surface results.

## 2. Compose Integration

### Remembering a NavFlow

```kotlin
@Composable
fun CounterScreen() {
    val navFlow = rememberNavFlow { scope -> NavFlow(scope, CounterNode(scope)) }

    val renderer = remember {
        nodeRenderer<Nothing> {
            register<CounterNode> { node ->
                val state by node.state.collectAsState()
                CounterUi(state.value, onIncrement = { node.onEvent(CounterEvent.Increment) })
            }
        }
    }

    NavFlowHost(navFlow = navFlow, renderer = renderer)
}
```

### NavHost rules

1. NavHost owns tabs/top-level routes (`contacts`, `profile`, `contactDetails/{id}`).
2. Inside each destination, create a Kmposable ViewModel/NavFlow and render via `NavFlowHost`.
3. Map node outputs directly to `NavController.navigate(...)`/`popBackStack()`.
4. Use `KmposableBackHandler(navFlow)` (or `enableBackHandler=true` on `NavFlowHost`) so NavFlow can
   consume internal back events.
5. Keep Kotlin code multiplatform; no androidX ViewModels or platform-specific helpers.

Full structure is described in the [NavHost integration summary](../spec_docs/NAVHOST_INTEGRATION_SUMMARY.md).

## 3. Testing (Reactive or Scripted Flows)

`FlowTestScenario` drives NavFlow headlessly:

```kotlin
val scenario = factory.createTestScenario(this).start()
scenario.launchScript { runContactsFlowScript(repository, this@runTest) }
scenario.awaitTopNodeIs<ContactDetailsNode>()
scenario.assertStackTags("ContactsList", "ContactDetails")
scenario.finish()
```

Highlights:

- `start()`, `send(event)`, `pop()` – basic stack control.
- `awaitTopNodeIs<T>()`, `awaitStackSize`, `awaitStackTags` – synchronise with navigation.
- `awaitOutputOfType<T>()`, `awaitMappedOutput { … }` – wait for outputs.
- `launchScript(onTrace = …)` – run the same NavFlow script you ship.

## 4. Advanced Patterns

- **DelegatingNode** – derive from it to wrap another node and override only what you need
  (e.g., map outputs, hook `onDetach`).
- **Typed stack entries** – create custom entries when you need metadata alongside nodes.
- **DI/ViewModels** – combine `rememberNavFlow` with your DI container; no Android-specific
  dependencies required.
- **Tracing** – feed `onTrace` into scripts to integrate with analytics or loggers.

For scripts specifically, see [NavFlow Scripts]({{ site.baseurl }}/guides/flowscripts/) and the
[Cookbook]({{ site.baseurl }}/cookbook/).
