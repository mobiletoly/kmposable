# kmposable Architecture

kmposable is a **headless flow engine** for Kotlin Multiplatform (KMP).  
It manages a stack (or tree) of **Nodes** – each Node owns state, handles events, and emits
outputs – without depending on any specific UI framework.

Think of kmposable as:

> A navigation + state orchestration layer **under** your UI, not **inside** it.

You can plug Compose, SwiftUI, web, or even a CLI on top.

---

## High-Level Layers

kmposable is split into three conceptual layers:

1. **Core (`library-core`)**
    - `Node<STATE, EVENT, OUTPUT>` / `StatefulNode` (state + outputs + child scope)
    - `LifecycleAwareNode` hook for attach/detach callbacks
    - Platform-neutral and fully testable.

2. **Navigation (`library-core` nav package)**
    - `KmposableStackEntry` + `DefaultStackEntry` (wrap a node + optional tag)
    - `KmposableNavState<OUT, ENTRY>` (immutable snapshot, stack never empty)
    - `KmposableNavigator<OUT, ENTRY>` + `KmposableStackNavigator` (entry-based push/pop/replace/popTo)

3. **Runtime (`library-core` runtime package)**
    - `NavFlow<OUT, ENTRY>` owns:
        - a navigator instance
        - the app-level `CoroutineScope`
        - lifecycle wiring (`attachNode` / `detachNode`)
        - output multiplexing (`outputs: Flow<OUT>`)
    - `NavFlowFactory` + `SimpleNavFlowFactory` expose a standard way to build flows for UI/tests.
    - `FlowTestScenario` (in `library-test`) drives a runtime headlessly in unit tests.

UI adapters (Compose, SwiftUI, etc.) live in **separate modules** and observe the runtime to render
the current Node(s). The Compose adapter (`library-compose`) supplies `NavFlowHost`,
`NodeRenderer`, `NavFlowViewModel`, and lifecycle helpers so Compose code stays declarative.

---

## Core Concepts

### Node

A **Node** is the core unit of logic:

- Owns **state** (`STATE`)
- Accepts **events** (`EVENT`)
- Emits **outputs** (`OUTPUT`), often navigation or domain events

### Navigator & NavState

- `KmposableStackEntry` wraps a node instance (plus metadata like `tag`).
- `KmposableNavState` exposes `stack`, `topEntry`, `rootEntry`, guarantees at least one element.
- `KmposableNavigator` mutates the stack via entries (push/pop/replace/popTo/popAll). `KmposableStackNavigator`
  is a simple mutable-list implementation backed by `MutableStateFlow`.

### NavFlow

- Creates entries for nodes (`createEntry`) and hands them to the navigator.
- Observes node outputs, forwards them via `outputs: Flow<OUT>`, and allows subclasses to
  interpret them (`onNodeOutput`).
- Lifecycle aware: attaches each node when it enters the stack and detaches it when removed.
- Has convenience APIs such as `push`, `pop`, `replaceTop`, `popTo`, `popToRoot`, `canPop`, and
  exposes `navState: StateFlow<KmposableNavState<OUT, ENTRY>>`.
- Factories (`NavFlowFactory`) and test harnesses (`FlowTestScenario`) ensure shared construction
  between UI and headless tests.

### Compose Adapter

- `NavFlowHost(navFlow, renderer, enableBackHandler)` collects `navState` and renders the top
  node using a `NodeRenderer` registry. The host wires `KmposableBackHandler` by default so
  Compose screens get consistent back handling without extra boilerplate.
- `rememberNavFlow` ties runtime `start()`/`dispose()` to composition.
- `NavFlowViewModel` / `navFlowViewModel` make flows lifecycle-aware in Compose.
- Sample modules:
    - `sample-app-compose`: uses the Compose adapter to demonstrate the ergonomic API.

---

## Relationship to UI Navigation

kmposable is **headless** and **UI-agnostic**, unlike Jetpack/Compose Navigation.

Compose Navigation = UI router  
kmposable = flow engine

---

## Design Principles

1. Headless first
2. KMP-first
3. Output-driven navigation
4. Predictable lifecycle
5. Composable layers
6. Small surface area
7. Testability and parity across platforms (FlowTestScenario, shared FlowFactory)
