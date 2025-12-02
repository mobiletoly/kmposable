---
layout: doc
title: Reactive – Overlays with NavFlow
permalink: /cookbook/recipes/reactive-overlays/
---

Render overlay nodes (dialogs/sheets) on top of a base screen with `OverlayNavFlowHost`, keeping the base visible and allowing custom animations.

Diagram:
```
Base entry (Screen/Host)
   ├─ overlay #1 (sheet/dialog)
   └─ overlay #2 (exiting, animating out)
OverlayNavFlowHost manages both and keeps base visible.
```

When to use:
- Modals/bottom sheets driven by nodes (`Presentation.Overlay`) where the base screen stays visible.
- You need enter/exit animations without manual overlay plumbing.
- Overlay-only stacks: use `rememberOverlayNavFlow()` so you don’t have to seed a dummy root.
 - Prefer the new `OverlayController` + `OverlayHost` helpers to avoid boilerplate.

## When to use

- Nodes declare `presentation = Presentation.Overlay` and you want to keep the underlying screen.
- You need per-overlay enter/exit animations without custom plumbing.

## Sample setup

```kotlin
// Node declares overlay + optional custom animations
class SheetNode(parent: CoroutineScope) : StatefulNode<State, Event, Out>(parent, State()),
    PresentationAware,
    OverlayAnimationAware {
    override val presentation = Presentation.Overlay
    override val overlayEnter = slideInVertically { it / 2 } + fadeIn()
    override val overlayExit = slideOutVertically { it / 2 } + fadeOut()
    override fun onEvent(event: Event) { /* ... */ }
}

@Composable
fun OverlayFlowHost(navFlow: NavFlow<Out, *>, renderer: NodeRenderer<Out>) {
    OverlayNavFlowHost(
        navFlow = navFlow,
        renderer = renderer,
        overlayEnter = fadeIn(),   // global defaults (used when node doesn’t override)
        overlayExit = fadeOut(),
        overlayScrim = { /* optional scrim composable */ }
    )
}
```

### Renderer setup

```kotlin
val renderer = remember {
    nodeRenderer<Out> {
        register<BaseNode> { BaseHost(it) }              // primary entries
        registerResultOnly<SheetNode> { SheetHost(it) }  // overlays; keep Screens pure
    }
}
OverlayFlowHost(navFlow, renderer)
```

Overlay-only helper (no dummy root):

```kotlin
val overlayFlow = rememberOverlayNavFlow<Out>() // seeds a hidden no-op root
OverlayNavFlowHost(navFlow = overlayFlow, renderer = renderer)
```

## Overlay controller shortcut

Use `rememberOverlayController` + `OverlayHost` to bundle overlay flow creation, hosting, and
result handling:

```kotlin
val overlay = rememberOverlayController<Unit>() // overlay-only flow seeded internally
val renderer = remember {
    nodeRenderer<Unit> {
        registerResultOnly<SheetNode> { SheetHost(it) }
    }
}

// Launch an overlay that returns Unit; auto-pop on first result
Button(onClick = { overlay.launch { SheetNode(parentScope = overlay.scope) } }) {
    Text("Open sheet")
}

OverlayHost(controller = overlay, renderer = renderer) // renders overlays with default fade
```

`overlay.launch(factory, autoPop)` is a thin wrapper around `launchPushAndAwaitResult` that uses the
controller's scope and nav flow. For tests, `FlowTestScenario.pushResultNode` mirrors this pattern.

### Testing overlays

- Use `FlowTestScenario.pushOverlayResult(autoPop)` to push a result-only overlay node and await its
  `KmposableResult` headlessly (mirrors host-side `pushAndAwaitResult`).
- `AutoCloseOverlay` overlays will also pop themselves when composed in `OverlayNavFlowHost`, so
  tests can rely on `KmposableResult` without asserting a manual pop.

### Auto-close overlays on first result

If an overlay node implements `AutoCloseOverlay<RESULT>`, `OverlayNavFlowHost` will pop it as soon as
the first `KmposableResult` is emitted (defaults to closing on both `Ok` and `Canceled`). This is
handy when you prefer `autoPop = false` or when overlays are driven by a longer-lived NavFlow:

```kotlin
class SheetNode(parentScope: CoroutineScope) :
    ResultfulStatefulNode<State, Event, Nothing, Unit>(parentScope, State()),
    AutoCloseOverlay<Unit>,
    PresentationAware {
    override val presentation = Presentation.Overlay
    override fun onEvent(event: Event) {
        scope.launch { emitOk(Unit) } // host will pop after this
    }
}

Button(onClick = { overlay.launch(autoPop = false) { SheetNode(overlay.scope) } }) { ... }
```
Override `shouldAutoClose(result)` if you need to gate auto-close on a subset of outcomes.

**When to use vs `autoPop = true`**

- `autoPop = true` (on push/launch) works when the caller pushes and awaits the overlay; it pops in
  the same coroutine after the first result.
- `AutoCloseOverlay` is host-driven: the host observes the overlay's `result` and pops when
  appropriate. Use this when callers *aren't* awaiting (fire-and-forget launches, long-lived flows,
  or `autoPop = false`) so the overlay still exits once it emits.
- You can combine both; `AutoCloseOverlay` is the safety net that prevents “forgot to pop” when
  overlays are launched without awaiting.

## Notes

- Overlays render above the last primary entry; if none are present, behavior matches `NavFlowHost`.
- Exiting overlays stay composed until exit animation completes (host tracks “exiting” overlays).
- If you omit `overlayEnter/overlayExit`, defaults apply (fade in/out).
- Register overlays via `registerResultOnly` to avoid threading the NavFlow OUT through result-only nodes.
***
