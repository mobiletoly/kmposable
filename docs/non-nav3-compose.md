---
layout: page
title: Non-Nav3 Compose Hosting
permalink: /non-nav3-compose/
---

## Status

This path is **supported**, but it is **not** the primary `0.3.x` recommendation.

Use it when:

- your app is not on Navigation 3 KMP
- you want a smaller standalone Compose setup
- you are embedding kmposable into an existing non-Nav3 shell

If you are starting a new Compose KMP app, prefer [Navigation 3 KMP]({{ site.baseurl }}/navigation3-kmp/).

## What You Use

- `library-core`
- `library-compose`
- `NavFlowHost`
- `rememberNavFlow`
- `NodeRenderer`
- `navFlowViewModel` / `rememberNavFlowViewModel`

You do **not** need `library-navigation3` for this path.

## Basic Shape

```kotlin
@Composable
fun ContactsFeature(repository: ContactsRepository) {
    val navFlow = rememberNavFlow { scope ->
        ContactsNavFlow(repository = repository, appScope = scope)
    }

    val renderer = remember {
        nodeRenderer<ContactsFlowEvent> {
            register<ContactsListNode> { node -> ContactsListHost(node) }
            register<ContactDetailsNode> { node -> ContactDetailsHost(node, snackbarHostState) }
            registerResultOnly<EditContactNode> { node -> EditContactHost(node, snackbarHostState) }
        }
    }

    NavFlowHost(navFlow = navFlow, renderer = renderer)
}
```

## What This Path Is Good At

- feature-local workflows hosted directly in Compose
- reusable headless feature logic without adopting Navigation 3 first
- simple apps or modules where the outer shell is already decided elsewhere

## What This Path Is Not

- the recommended whole-app routing story for new Compose KMP apps
- a substitute for Navigation 3's app-shell save/restore and scene model
- the preferred path for shell overlays or outer back stack ownership

## Overlays On This Path

Feature-local overlays are still valid here:

- `OverlayNavFlowHost`
- `OverlayHost`
- `rememberOverlayController`

But if you later move the app shell to Navigation 3 KMP, treat those as feature-local helpers, not
as the shell overlay system.
