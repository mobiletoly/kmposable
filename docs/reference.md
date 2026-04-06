---
layout: page
title: Reference
permalink: /reference/
---

This page summarizes the most important APIs. For full details open the corresponding files in the
repository - each section below links to source directories.

## Core Primitives (`library-core/src/commonMain/kotlin/dev/goquick/kmposable/core`)

| Type                                                                       | Purpose                                                                                                                                                               |
|----------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Node<STATE, EVENT, OUTPUT>`                                               | Minimal interface (state `StateFlow`, `onEvent`, outputs `Flow`).                                                                                                     |
| `LifecycleAwareNode`                                                       | Optional hooks (`onAttach`, `onDetach`) invoked when NavFlow pushes/pops nodes.                                                                                       |
| `StatefulNode`                                                             | Base class that manages state via `MutableStateFlow`, exposes `updateState`, `emitOutput`.                                                                            |
| `DelegatingNode`                                                           | Wraps another node and delegates everything by default - override only the parts you need (e.g., map outputs).                                                        |
| `EffectSource` / `EffectfulStatefulNode`                                   | Opt-in effects channel for one-off side effects (analytics, toasts, etc.) separate from outputs.                                                                      |
| `EffectfulResultfulStatefulNode` / `EffectfulResultOnlyNode`               | Convenience base for flows that need both a typed result channel and transient effects (snackbar/dialog/analytics) in the same node.                                   |
| `ResultNode`, `ResultfulStatefulNode`, `ResultOnlyNode`, `KmposableResult` | Opt-in "start for result" contract; `ResultfulStatefulNode` provides the flow + emit helpers; `ResultOnlyNode` is the OUTPUT=Nothing variant for result-only screens. |
| `runCatchingState`                                                         | Helper on `StatefulNode` to standardize loading/success/error updates around suspending calls (reducers for start/success/error).                                      |
| `mirrorChildState`                                                         | Helper on `StatefulNode` to mirror a child `StateFlow` into parent state via a mapper.                                                                                |

## Navigation Runtime (`library-core/runtime`)

- `NavFlow<OUT, ENTRY>` - headless runtime that owns a navigator (`KmposableNavigator`). Exposes
  `navState: StateFlow<KmposableNavState<OUT, ENTRY>>`, `outputs: Flow<OUT>`, stack operations
  (`push`, `pop`, `replaceAll`, `popTo`...), low-level `sendEvent`, and lifecycle (`start`/`dispose`).
  The public `ENTRY` generic survives in `0.3.x` because scripts, tests, and advanced hosts still
  need typed stack entries without coupling core to Navigation 3.
- `KmposableNavigator` / `KmposableStackNavigator` - advanced stack implementation hooks used by
  `NavFlow`. These remain public in `0.3.x`, but they are not the recommended app-shell routing
  model for Compose KMP apps.
- `KmposableNavState` - read-only stack snapshot with `stack`, `top`, `root`, `size` accessors for
  renderers, tests, and scripts.
- `KmposableStackEntry` / `DefaultStackEntry` - pair node with metadata such as tags and saveable
  state identity. These remain advanced runtime customization points in `0.3.x`.
- `NavDiff` / `diffNavState` - structural diff between nav states (pushed/popped entries).
- `NavFlowScriptScope` - script-facing API (`showRoot`, `pushNode`, `replaceTop`, `awaitOutput*`,
  `trace`, `createEntry`, `navState`). Run scripts via
  `NavFlow.launchNavFlowScript(scope, onTrace, script)`
  or the `runScript` alias (SimpleNavFlow variants provided).
- Helpers: `runCatchingNodeCall`, `awaitOutputCase`, `pushForResult`,
  `pushAndAwaitResult(factory/onResult)`, result-only helpers `pushAndAwaitResultOnly` /
  `launchPushAndAwaitResultOnly`,
  safe stack ops `pushIfStarted`/`popIfStarted`, typed top-node helpers
  `withTopNode` / `updateTopNode`, `withNode`, etc.

## Compose Adapters (`library-compose`)

- `rememberNavFlow(key, factory)` - creates/starts/disposes a NavFlow tied to a composable scope.
- `NavFlowHost(navFlow, renderer, enableBackHandler)` - observes `navFlow.navState` and renders the
  top node. In `0.3.x`, this is primarily the host for a feature-local runtime, not the
  recommended whole-app router.
- `nodeRenderer { register<MyNode> { ... } }` - DSL mapping node types to composables.
- `KmposableBackHandler` - multiplatform back handler; delegates iOS swipe-back / desktop `Esc` /
  Android back to NavFlow when the stack can pop.
- `CollectEffects(node) { ... }` - lifecycle-aware helper to collect `EffectSource.effects` in
  Compose and route them to UI (e.g., snackbar, dialog, logging).
- `rememberNode` / `NodeHost` - create/host standalone nodes (not managed by NavFlow) with automatic
  attach/detach and state collection; optionally collect outputs inline.
- `rememberOverlayNavFlow` + `OverlayNavFlowHost` - overlay-only stacks without dummy roots for
  feature-local overlays.
- `rememberOverlayController` + `OverlayHost` - bundle overlay NavFlow creation, push/await helpers,
  and hosting for feature-local overlays. This is a legacy/non-primary path for app-shell overlay
  concerns in `0.3.x`.
- `AutoCloseOverlay` - opt-in marker for overlay result nodes; hosts pop the overlay once a result
  is emitted (`Ok`/`Canceled` by default). Complements `autoPop = true` (caller-driven pop) by
  covering fire-and-forget launches or `autoPop = false`.
- `FlowTestScenario.pushOverlayResult(autoPop)` - test helper to push a result-only overlay and
  await its `KmposableResult` headlessly.
- `NavFlowLogger` / `NodeErrorLogger` - optional hooks for logging telemetry (attach/detach/output)
  and node-level errors without threading loggers through every host.
- `OverlayNavFlowHost` respects per-node animation hints via `OverlayAnimationAware` if provided.

## Navigation 3 KMP Integration (`library-navigation3`)

- `rememberKmposableNavEntryDecorators()` - installs the saveable-state and entry-scoped ViewModel
  decorators needed to host kmposable flows inside Navigation 3 destinations.
- `rememberNavigation3NavFlow(factory)` - creates a kmposable `NavFlow` that reuses the current
  `LocalViewModelStoreOwner` when hosted under Navigation 3.
- `kmposableNavFlowEntry(...)` - helper to create a `NavEntry` whose content hosts a kmposable
  feature runtime.
- `CollectNavFlowOutputs(navFlow, onOutput)` - collect feature outputs at the app adapter layer.
- `navKeySavedStateConfiguration(serializersModule)` - helper for building the required
  `SavedStateConfiguration` while keeping route serialization ownership in the app shell.
- `pushSingleTop(destination)` - back stack mutation helper for app-owned Navigation 3 routes.

## Testing (`library-test`)

`FlowTestScenario` is the main DSL:

- `start()` - begins collecting outputs and calls `NavFlow.start()`.
- `updateTopNode<T> { ... }` / `pop()` / `assertCanPop()` - manipulate the stack.
- `assertTopNodeTag`, `assertStackSize`, `assertStackTags` - synchronous assertions.
- `awaitTopNodeIs<T>()`, `awaitStackSize`, `awaitStackTags` - suspend until NavFlow reaches a state.
- `awaitNextOutput`, `awaitMappedOutput`, `awaitOutputOfType<T>()` - wait for outputs.
- `launchScript(onTrace) { ... }` - run NavFlow scripts in tests.
- `pushResultNode(factory, autoPop)` - push a result node, await `KmposableResult`, optional auto-pop
  (handy for dialogs/overlays/subflows).
- `finish()` - cancel collectors and dispose NavFlow.

Factory helpers: `NavFlowFactory.createTestScenario(scope)` builds a new runtime per test.

## Script Helpers (summary)

| Helper                                                      | Description                                                                           |
|-------------------------------------------------------------|---------------------------------------------------------------------------------------|
| `showRoot`, `pushNode`, `replaceTop`                        | Stack operations without dealing with entries.                                        |
| `awaitOutputOfType`, `awaitOutputCase`, `awaitMappedOutput` | Wait for future outputs.                                                              |
| `pushForResult`, `withNode`, `updateTopNode`                | Push a temporary node for a block/result, pop automatically, or act on the top node.  |
| `runCatchingNodeCall`                                       | Display loading/success/error state while running suspending work.                    |
| `trace { ... }`                                             | Send debug logs via `onTrace` callback passed to `runScript` / `launchNavFlowScript`. |

Combine these helpers to orchestrate complex flows headlessly. See the [Cookbook]({{ site.baseurl
}}/cookbook/)
for end-to-end examples and the source tree for implementation details.
