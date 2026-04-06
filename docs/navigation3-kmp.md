---
layout: page
title: Navigation 3 KMP
permalink: /navigation3-kmp/
---

## Recommended Architecture

For kmposable `0.3.x`, the primary Compose KMP architecture is:

- **Navigation 3 KMP** owns app routes, outer back stack, save/restore, scene composition, and destination-scoped ViewModel ownership.
- **kmposable** owns feature-local workflows, `Node`s, `NavFlow`, outputs, scripts, and headless tests.

This keeps the app shell explicit while preserving kmposable's main value: reusable feature runtime
logic that is not trapped inside UI code.

## Modules

- `library-core` – headless workflow/runtime, Navigation 3 agnostic.
- `library-compose` – generic Compose hosting for kmposable features.
- `library-navigation3` – Navigation 3 KMP integration helpers.

`library-core` must not depend on `NavKey`, `NavBackStack`, `NavEntry`, `NavDisplay`,
`SavedStateConfiguration`, or route serialization concerns.

## Dependency Baseline

The current `0.3.x` baseline is:

- `org.jetbrains.androidx.navigation3:navigation3-ui:1.1.0-beta01`
- `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3:2.10.0`
- `org.jetbrains.compose.material3.adaptive:adaptive-navigation3:1.3.0-alpha06` (optional)

The sample and `library-navigation3` are aligned to this matrix.

## Feature Hosting Pattern

The app owns Navigation 3 destinations and back stack mutations. Each destination can host a
kmposable feature runtime inside its content:

```kotlin
@Composable
fun App() {
    val backStack = rememberNavBackStack(sampleAppNavConfiguration, ContactsRoute)
    val decorators = rememberKmposableNavEntryDecorators<NavKey>()

    NavDisplay(
        backStack = backStack,
        entryDecorators = decorators,
        entryProvider = { route ->
            when (route) {
                ContactsRoute -> NavEntry(route) {
                    ContactsDestination(
                        repository = repository,
                        onFlowOutput = navigationController::onContactsOutput,
                    )
                }
                SettingsRoute -> NavEntry(route) {
                    SettingsDestination(onNavigateBack = navigationController::navigateBack)
                }
                else -> error("Unsupported route: $route")
            }
        }
    )
}
```

The feature runtime stays Navigation 3 free. Only the app-layer adapter reacts to feature outputs
and mutates the outer back stack.

## ViewModel Ownership

When kmposable runs inside Navigation 3:

- use `rememberKmposableNavEntryDecorators()` so Navigation 3 owns `rememberSaveable` state and entry-scoped `ViewModelStore`s
- create the flow with `rememberNavigation3NavFlow(...)`
- do not create a second app-level ViewModel ownership scheme parallel to Navigation 3

Kmposable's Compose ViewModel helpers now reuse a `LocalViewModelStoreOwner` when present, which
lets Navigation 3 entry scoping work across the supported KMP targets.

## When To Use Kmposable With Navigation 3 KMP

- You want feature logic that is reusable outside Compose UI.
- You want deterministic, headless tests for multi-step flows.
- You want output-driven feature workflows under a standard app shell.
- You want feature-local stacks without making the app shell depend on that internal shape.

## When Not To Use Kmposable

- You only need app-level routing and ordinary destination screens.
- You do not need headless feature workflows, scripts, or reusable node logic.
- You want kmposable to replace Navigation 3 as the primary app router in a Compose KMP app.

## Current Web Position

Web is **not** part of the first sample target matrix for `0.3.x`.

Reason:

- the sample currently validates JVM desktop, Android, and iOS simulator compile paths
- Navigation 3 KMP is still beta
- we are not claiming a production-ready browser story until the Navigation 3 UI target story is
  stable enough to support it credibly
