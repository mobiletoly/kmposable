---
layout: page
title: Overview
permalink: /overview/
---

## Why Kmposable?

Modern Compose/KMP apps struggle when feature workflow logic lives inside UI layers:

- ViewModels become dumping grounds for flows, validation, and navigation state.
- Navigation APIs are platform-specific, making reuse across Android/iOS/Desktop awkward.
- Testing a “flow” (list → details → edit) usually requires Compose and an emulator.

Kmposable flips the script:

1. **Nodes** hold state + events + outputs. They are pure Kotlin, no UI dependencies.
2. **NavFlow** is a headless runtime that manages a stack of nodes (push, pop, replace, popTo…).
3. **Adapters** (Compose, Navigation 3 integration, FlowTestScenario, scripts) simply observe NavFlow.

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
| `library-compose` | Generic Compose adapter (renderers, `NavFlowHost`, `KmposableBackHandler`). |
| `library-navigation3` | Navigation 3 KMP integration (`NavEntry` decorators, entry-scoped `NavFlow`, output collection helpers). |
| `library-test` | `FlowTestScenario` DSL for headless testing + script helpers. |
| `sample-app-compose` | Compose Multiplatform sample (Navigation 3 KMP shell + inner kmposable flow). |
| `sample-app-flowscript` | Same UI but orchestrated via NavFlow scripts to show sequential flows. |

Artifacts are published to Maven Central under `dev.goquick.kmposable:*`. See the
[Getting Started guide]({{ site.baseurl }}/guides/#getting-started) for coordinates.

## Recommended Compose Architecture

For `0.3.x`, the recommended Compose KMP split is:

- Navigation 3 KMP owns top-level routes, outer back stack, save/restore, and scene composition.
- Kmposable owns feature-local workflows, nodes, outputs, and headless tests.

That keeps app routing explicit while preserving kmposable's cross-platform workflow value.

## Compose Paths

- **Recommended**: [Navigation 3 KMP]({{ site.baseurl }}/navigation3-kmp/)
- **Supported fallback**: [Non-Nav3 Compose Hosting]({{ site.baseurl }}/non-nav3-compose/)
- **Chooser page**: [Nav3 vs Non-Nav3]({{ site.baseurl }}/compose-paths/)

## Samples

- **Compose sample** – uses Navigation 3 KMP for the app shell and kmposable for the Contacts feature flow.
- **FlowScript sample** – reuses the same nodes but drives everything via a NavFlow script.

Both samples live in the repository root and include ready-to-run Gradle tasks. Follow the
instructions inside each `sample-app-*` directory.

## Where to go next

- [Guides]({{ site.baseurl }}/guides/) – install, Compose integration, scripts, and testing.
- [Navigation 3 KMP]({{ site.baseurl }}/navigation3-kmp/) – recommended architecture and dependency baseline.
- [Cookbook]({{ site.baseurl }}/cookbook/) – real-world recipes and patterns.
- [Reference]({{ site.baseurl }}/reference/) – API notes for core/compose/test modules.
