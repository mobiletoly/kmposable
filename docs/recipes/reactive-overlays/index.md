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

## Notes

- Overlays render above the last primary entry; if none are present, behavior matches `NavFlowHost`.
- Exiting overlays stay composed until exit animation completes (host tracks “exiting” overlays).
- If you omit `overlayEnter/overlayExit`, defaults apply (fade in/out).
- Register overlays via `registerResultOnly` to avoid threading the NavFlow OUT through result-only nodes.
***
