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
| `ResultNode`, `ResultfulStatefulNode`, `ResultOnlyNode`, `KmposableResult` | Opt-in "start for result" contract; `ResultfulStatefulNode` provides the flow + emit helpers; `ResultOnlyNode` is the OUTPUT=Nothing variant for result-only screens. |

## Navigation Runtime (`library-core/runtime`)

- `NavFlow<OUT, ENTRY>` - headless runtime that owns a navigator (`KmposableNavigator`). exposes
  `navState: StateFlow<KmposableNavState<OUT, ENTRY>>`, `outputs: Flow<OUT>`, stack operations
  (`push`, `pop`, `replaceAll`, `popTo`...), `sendEvent`, and lifecycle (`start`/`dispose`).
- `KmposableNavigator` / `KmposableStackNavigator` - stack implementation used by NavFlow. Accepts
  custom stack entry types when you need metadata.
- `KmposableNavState` - snapshot with `stack`, `top`, `root`, `size` accessors.
- `KmposableStackEntry` / `DefaultStackEntry` - pair node with metadata; override when you need tags
  or IDs for testing/analytics.
- `NavDiff` / `diffNavState` - structural diff between nav states (pushed/popped entries).
- `NavFlowScriptScope` - script-facing API (`showRoot`, `pushNode`, `replaceTop`, `awaitOutput*`,
  `trace`, `createEntry`, `navState`). Run scripts via
  `NavFlow.launchNavFlowScript(scope, onTrace, script)`
  or the `runScript` alias (SimpleNavFlow variants provided).
- Helpers: `runCatchingNodeCall`, `awaitOutputCase`, `pushForResult`,
  `pushAndAwaitResult(factory/onResult)`, result-only helpers `pushAndAwaitResultOnly` /
  `launchPushAndAwaitResultOnly`,
  safe stack ops `pushIfStarted`/`popIfStarted`, `withNode`, etc.

## Compose Adapters (`library-compose`)

- `rememberNavFlow(key, factory)` - creates/starts/disposes a NavFlow tied to a composable scope.
- `NavFlowHost(navFlow, renderer, enableBackHandler)` - observes `navFlow.navState` and renders the
  top node. Automatically wires `KmposableBackHandler` unless disabled.
- `nodeRenderer { register<MyNode> { ... } }` - DSL mapping node types to composables.
- `KmposableBackHandler` - expect/actual back handling that delegates to NavFlow (
  Android/iOS/Desktop).
- `CollectEffects(node) { ... }` - lifecycle-aware helper to collect `EffectSource.effects` in
  Compose
  and route them to UI (e.g., snackbar, dialog, logging).

## Testing (`library-test`)

`FlowTestScenario` is the main DSL:

- `start()` - begins collecting outputs and calls `NavFlow.start()`.
- `send(event)` / `pop()` / `assertCanPop()` - manipulate the stack.
- `assertTopNodeTag`, `assertStackSize`, `assertStackTags` - synchronous assertions.
- `awaitTopNodeIs<T>()`, `awaitStackSize`, `awaitStackTags` - suspend until NavFlow reaches a state.
- `awaitNextOutput`, `awaitMappedOutput`, `awaitOutputOfType<T>()` - wait for outputs.
- `launchScript(onTrace) { ... }` - run NavFlow scripts in tests.
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
