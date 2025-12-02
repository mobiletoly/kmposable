---
layout: doc
title: Feature Package Layout
permalink: /cookbook/recipes/project-structure/
---

Start clean on new projects so headless flow code stays reusable and UI stays organized.

Recommended structure (per feature):

```
	feature/
	  contacts/
	    flow/      // NavFlow + nodes (headless, no Compose imports)
	    ui/        // Hosts + Screens + renderer wiring (Compose-only)
	    data/      // optional: repositories, DTOs
```

Top-level:

- `app/` (or root `App.kt`) owns NavHost, DI, and assembles renderers per feature.
- Shared design helpers live outside features (theme/components) to avoid coupling.

Visual cheat sheet:
```
flow (headless) ──> ui/Host (glue) ──> ui/Screen (pure UI)
     ^ tests via FlowTestScenario          ^ previews live here
```

Guidelines:

- `flow` stays platform-agnostic: state, events, outputs, effects, NavFlow factories. Test with `FlowTestScenario`.
- `ui` owns Hosts (collect state/effects, DI/subflows) and Screens (pure UI). Register nodes → Hosts in renderer builders.
- Overlays: overlay nodes in `flow`, overlay hosts/screens in `ui`, rendered via `OverlayNavFlowHost` with `registerResultOnly`.
 - Overlays: overlay nodes in `flow`, overlay hosts/screens in `ui`, rendered via `OverlayNavFlowHost` with `registerResultOnly` (use `rememberOverlayNavFlow` for overlay-only stacks, or `OverlayController`/`OverlayHost` for reduced boilerplate).
- Keep naming consistent: `FooNode`, `FooHost`, `FooScreen`; renderer functions like `fooRenderer()`.
- If you add `data`, keep it DI-friendly and injected into nodes/NavFlows; don’t pull UI dependencies into `flow`.

Why it matters:

- Clear boundary: headless logic stays reusable across platforms; UI glue stays Compose-only.
- Greppable and predictable imports for onboarding.
- Tests and scripts target `flow` directly; previews and UI tweaks live in `ui`.
