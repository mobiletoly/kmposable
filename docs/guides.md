---
layout: page
title: Guides
permalink: /guides/
---

## Start Here

- **[Hello Kmposable]({{ site.baseurl }}/guides/hello-kmposable/)** – the smallest flow: install, node, NavFlow,
  Compose host, headless test.
- **Core Guides** (below) – deep dives into Compose integration, scripts, testing, and advanced patterns.

## Choose A Compose Path

- **Recommended**: [Navigation 3 KMP]({{ site.baseurl }}/navigation3-kmp/)
- **Supported fallback**: [Non-Nav3 Compose Hosting]({{ site.baseurl }}/non-nav3-compose/)
- **Quick comparison**: [Nav3 vs Non-Nav3]({{ site.baseurl }}/compose-paths/)

## 1. Getting Started

### Install

```kotlin
dependencies {
    implementation("dev.goquick.kmposable:core:<version>")
    implementation("dev.goquick.kmposable:compose:<version>")
    implementation("dev.goquick.kmposable:navigation3:<version>") // when using Navigation 3 KMP
    testImplementation("dev.goquick.kmposable:test:<version>")
}
```

Replace `<version>` with the latest release from Maven Central. `core` is mandatory, `compose` is
needed only if you render with Jetpack Compose, `navigation3` is the preferred Compose KMP
integration layer, and `test` pulls in `FlowTestScenario` utilities.

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
navFlow.updateTopNode<CounterNode> { onEvent(CounterEvent.Increment) }
println(navFlow.navState.value.top.state.value) // 1
```

How this reaches UI:
- UI observes `node.state` (e.g., `collectAsState()` in Compose) and renders it.
- UI sends user actions back as `EVENT`s via `node.onEvent(...)` or `navFlow.updateTopNode<MyNode> { ... }`
  when it only has a NavFlow reference.
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

### Navigation 3 KMP rules

1. Navigation 3 KMP owns app routes, outer back stack, and destination save/restore.
2. Inside each destination, create a kmposable feature runtime and render it via `NavFlowHost`.
3. Map feature outputs to app-owned back stack mutations in the app layer, not inside nodes.
4. Use `rememberKmposableNavEntryDecorators()` so Navigation 3 owns destination-scoped saveable state and `ViewModelStore`s.
5. Keep node/business logic Navigation 3 free. The adapter layer is where app routing happens.

See [Navigation 3 KMP]({{ site.baseurl }}/navigation3-kmp/) for the current recommended shape.

### Non-Nav3 Compose rules

1. Use `NavFlowHost` as a feature host, not as the preferred future-facing app-shell story.
2. Keep the outer shell explicit in your app code.
3. Treat overlays as feature-local helpers rather than shell infrastructure.
4. Plan to migrate app-shell concerns to Navigation 3 KMP if you want the primary `0.3.x` architecture.

See [Non-Nav3 Compose Hosting]({{ site.baseurl }}/non-nav3-compose/) for the fallback path.

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

- `start()`, `updateTopNode<T> { ... }`, `pop()` – basic stack control.
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
- **Side effects** – implement `EffectSource` or extend `EffectfulStatefulNode` when you need a
  one-off effects stream (analytics, toasts) separate from navigation outputs. In Compose, collect
  them with `CollectEffects(node) { effect -> /* show snackbar / log */ }` to keep hosts lean and
  lifecycle-aware.
- **Result-only subflows** – extend `ResultOnlyNode<STATE, EVENT, RESULT>` when a screen should only
  return a result (OUTPUT = Nothing). Push it with `pushAndAwaitResultOnly` / `launchPushAndAwaitResultOnly`
  so you don't have to thread the NavFlow's OUT through result-only nodes.

For scripts specifically, see [NavFlow Scripts]({{ site.baseurl }}/guides/flowscripts/) and the
[Cookbook]({{ site.baseurl }}/cookbook/).
