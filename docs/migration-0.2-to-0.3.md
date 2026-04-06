---
layout: page
title: Migration 0.2.x -> 0.3.x
permalink: /migration-0-2-to-0-3/
---

## Breaking Release

Kmposable `0.3.x` is intentionally **not backward compatible** with `0.2.x`.

This is not a routine cleanup release. It is a product-boundary reset:

- Navigation 3 KMP becomes the primary Compose app-shell story.
- Kmposable narrows toward headless feature workflows and runtime orchestration.
- Overlapping “kmposable as the whole app router” messaging is removed.

## What Changed

### Recommended architecture

Before:

- Compose navigation and kmposable could both appear to own the app shell.

Now:

- Navigation 3 KMP owns app routes, outer back stack, save/restore, and scene composition.
- Kmposable owns feature-local `NavFlow`, nodes, outputs, scripts, and headless tests.

### New module

- `library-navigation3` is the new integration module for Navigation 3 KMP hosting helpers.

### Compose sample

- `sample-app-compose` now uses a Navigation 3 KMP shell and hosts a kmposable feature flow inside a destination.

### ViewModel ownership

- When a `LocalViewModelStoreOwner` is present, kmposable Compose ViewModel helpers reuse it so Navigation 3 entry scoping remains the single ownership model.

## Migration Steps

1. Move app-shell routing to Navigation 3 KMP.
2. Keep kmposable inside feature destinations rather than at the whole-app boundary.
3. Add `library-navigation3`.
4. Register app routes in a `SavedStateConfiguration` serializers module.
5. Use `rememberKmposableNavEntryDecorators()` with `NavDisplay`.
6. Host feature flows with `rememberNavigation3NavFlow(...)` or `kmposableNavFlowEntry(...)`.
7. Move feature-to-app navigation into an app-owned adapter that reacts to feature outputs.

## Still Supported

- `NavFlowHost` remains a valid Compose feature host.
- Standalone non-Nav3 Compose hosting still works.
- Headless tests and script-driven flows remain part of the product.
- Advanced core runtime contracts such as `NavFlow<OUT, ENTRY>`, `KmposableStackEntry`,
  `KmposableNavState`, and `KmposableNavigator` still survive in `0.3.0-alpha01` because they
  power scripts, tests, and custom hosts. They are no longer the primary Compose app-shell story.

## No Longer Primary

- Using `NavFlowHost` as the recommended whole-app Compose router.
- Presenting `OverlayNavFlowHost` as the preferred app-shell overlay system.
- Treating route serialization or app back stack ownership as kmposable core responsibilities.

## Web

Web is not part of the first `0.3.x` sample target matrix. Do not treat the `0.3.x` docs or sample
as a claim of production-ready browser support yet.
