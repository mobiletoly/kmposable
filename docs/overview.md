---
layout: page
title: Overview
permalink: /overview/
---

## Why Kmposable?

Modern Compose/KMP apps struggle when navigation and business logic live inside UI layers:

- ViewModels become dumping grounds for flows, validation, and navigation state.
- Navigation APIs are platform-specific, making reuse across Android/iOS/Desktop awkward.
- Testing a “flow” (list → details → edit) usually requires Compose and an emulator.

Kmposable flips the script:

1. **Nodes** hold state + events + outputs. They are pure Kotlin, no UI dependencies.
2. **NavFlow** is a headless runtime that manages a stack of nodes (push, pop, replace, popTo…).
3. **Adapters** (Compose, FlowTestScenario, scripts) simply observe NavFlow.

As a result you can build entire flows headlessly, render them in Compose, and test them without UI.

## Mental Model

```
Node (state, events, outputs)
        |
        | outputs
        v
Navigator <---- NavFlow ----> UI Renderer
```

- Nodes emit outputs when they need to communicate (e.g., “open details”).
- NavFlow captures those outputs so scripts or adapters can react (navigate, trigger DI, etc.).
- Compose observes `navFlow.navState`, renders `nodeRenderer` output, and feeds user events back.

## Modules

| Module | Description |
| --- | --- |
| `library-core` | Headless runtime (`Node`, `StatefulNode`, `NavFlow`, script APIs, helpers). |
| `library-compose` | Thin Compose adapter (renderers, `NavFlowHost`, `KmposableBackHandler`). |
| `library-test` | `FlowTestScenario` DSL for headless testing + script helpers. |
| `sample-app-compose` | Compose Multiplatform sample (NavHost tabs + Kmposable flow). |
| `sample-app-flowscript` | Same UI but orchestrated via NavFlow scripts to show sequential flows. |

Artifacts are published to Maven Central under `dev.goquick.kmposable:*`. See the
[Getting Started guide]({{ site.baseurl }}/guides/#getting-started) for coordinates.

## Samples

- **Compose sample** – uses NavHost for top-level destinations and Kmposable for inner flows.
- **FlowScript sample** – reuses the same nodes but drives everything via a NavFlow script.

Both samples live in the repository root and include ready-to-run Gradle tasks. Follow the
instructions inside each `sample-app-*` directory.

## Where to go next

- [Guides]({{ site.baseurl }}/guides/) – install, Compose integration, scripts, and testing.
- [Cookbook]({{ site.baseurl }}/cookbook/) – real-world recipes and patterns.
- [Reference]({{ site.baseurl }}/reference/) – API notes for core/compose/test modules.
- [Specs]({{ site.baseurl }}/specs/) – architecture decisions and RFCs.
