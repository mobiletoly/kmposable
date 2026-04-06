# kmposable Architecture

kmposable is a **headless workflow/runtime layer** for Kotlin Multiplatform (KMP).  
It manages a stack (or tree) of **Nodes** – each Node owns state, handles events, and emits
outputs – without depending on any specific UI framework or app-shell router.

Think of kmposable as:

> A feature workflow + state orchestration layer **under** your UI, not **instead of** your app shell.

You can plug Compose, SwiftUI, web, or even a CLI on top.

---

## High-Level Layers

kmposable is split into four conceptual layers:

1. **Core (`library-core`)**
    - `Node<STATE, EVENT, OUTPUT>` / `StatefulNode` (state + outputs + child scope)
    - `LifecycleAwareNode` hook for attach/detach callbacks
    - Platform-neutral and fully testable.

2. **Navigation internals (`library-core` nav package)**
    - `KmposableStackEntry` + `DefaultStackEntry` (wrap a node + optional tag)
    - `KmposableNavState<OUT, ENTRY>` (immutable snapshot, stack never empty)
    - `KmposableNavigator<OUT, ENTRY>` + `KmposableStackNavigator` (entry-based push/pop/replace/popTo)
    - Still part of `library-core`, but must remain Navigation 3 agnostic.

3. **Runtime (`library-core` runtime package)**
    - `NavFlow<OUT, ENTRY>` owns:
        - a navigator instance
        - the app-level `CoroutineScope`
        - lifecycle wiring (`attachNode` / `detachNode`)
        - output multiplexing (`outputs: Flow<OUT>`)
    - `NavFlowFactory` + `SimpleNavFlowFactory` expose a standard way to build flows for UI/tests.
    - `FlowTestScenario` (in `library-test`) drives a runtime headlessly in unit tests.

4. **UI adapters / integrations**
    - `library-compose`
        - generic Compose hosting (`NavFlowHost`, `NodeRenderer`, `NavFlowViewModel`,
          lifecycle helpers)
        - remains usable without Navigation 3 for standalone feature hosting
    - `library-navigation3`
        - Navigation 3 KMP integration
        - destination decorators
        - entry-scoped `NavFlow` creation
        - feature-output collection helpers

UI adapters (Compose, SwiftUI, etc.) live in **separate modules** and observe the runtime to render
the current Node(s). The Navigation 3 integration is intentionally outside `library-core` so the
headless runtime stays independent from `NavKey`, `NavBackStack`, `NavEntry`, route
serialization, or scene composition concerns.

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
    - `sample-app-compose`: uses Navigation 3 KMP for the app shell and kmposable for an inner
      feature workflow.

### Navigation 3 Integration

For Compose Multiplatform apps, the primary `0.3.x` architecture is:

- Navigation 3 KMP owns:
    - app routes
    - outer back stack
    - destination save/restore
    - scene composition
    - entry-scoped ViewModel ownership
- kmposable owns:
    - feature workflow runtime
    - feature-local node stack
    - outputs emitted by feature logic
    - headless flow tests and scripts

This means `library-core` must not depend on:

- `NavKey`
- `NavBackStack`
- `NavEntry`
- `NavDisplay`
- `SavedStateConfiguration`
- `SerializersModule`

---

## Relationship to App Navigation

kmposable is **headless** and **UI-agnostic**. It does not replace a Compose app shell.

Navigation 3 KMP = app-shell router  
kmposable = feature workflow runtime

---

## Design Principles

1. Headless first
2. KMP-first
3. Output-driven navigation
4. Predictable lifecycle
5. Composable layers
6. Small surface area
7. Testability and parity across platforms (FlowTestScenario, shared FlowFactory)
8. Navigation 3 compatibility at the integration layer, not in the core layer
