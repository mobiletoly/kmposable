---
layout: page
title: Cookbook
permalink: /cookbook/
---

Pick a recipe to dive deeper:

## Reactive

- [Navigation 3 KMP Architecture](../navigation3-kmp/) – recommended `0.3.x` app-shell pattern.
- [Non-Nav3 Compose Hosting](../non-nav3-compose/) – supported fallback path if you are not on Navigation 3.
- [Nav3 vs Non-Nav3](../compose-paths/) – which Compose path to choose.
- [Single-Screen Node](./recipes/reactive-single-screen/)
- [Node → Host → Screen Layering](./recipes/reactive-layering/)
- [Renderer Patterns](./recipes/reactive-renderers/)
- [Legacy: Compose NavHost + NavFlow](./recipes/reactive-navhost/)
- [FlowTestScenario Assertions](./recipes/reactive-testing/)
- [Reactive Overlays](./recipes/reactive-overlays/) – feature-local overlays with `OverlayNavFlowHost`, scrims, and optional animations.

## Sequential Scripts (experimental)

- [Onboarding Wizard](./recipes/script-onboarding/)
- [Contacts Flow](./recipes/script-contacts/)
- [Reusable Sub-flow](./recipes/script-subflow/)
- [Auth Retry with Backoff](./recipes/script-retry/)
- [Branching Wizard + Tracing](./recipes/script-wizard/)
- [Checkout Flow](./recipes/script-checkout/)
- [Testing Scripts](./recipes/script-testing/)

## Project Layout

- [Feature Package Layout](./recipes/project-structure/) – recommended `flow/ui` split for new projects.
