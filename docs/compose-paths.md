---
layout: page
title: Nav3 vs Non-Nav3
permalink: /compose-paths/
---

## Which Path Should You Use?

For Compose KMP in `0.3.x`, there are two supported paths:

### 1. Navigation 3 KMP + kmposable

This is the **recommended** path.

Use it when:

- you are building a new Compose KMP app
- you want a clear app shell with user-owned back stack and scene composition
- you want kmposable focused on feature workflows inside that shell

Read: [Navigation 3 KMP]({{ site.baseurl }}/navigation3-kmp/)

### 2. Non-Nav3 Compose + kmposable

This is the **supported fallback** path.

Use it when:

- you are not on Navigation 3 yet
- you have an existing shell that should stay as-is for now
- you want kmposable feature hosting without adding Nav3 immediately

Read: [Non-Nav3 Compose Hosting]({{ site.baseurl }}/non-nav3-compose/)

## Short Rule

- New Compose KMP app: choose **Navigation 3 KMP + kmposable**
- Existing non-Nav3 app or simpler standalone hosting: choose **Non-Nav3 Compose + kmposable**

## Responsibility Split

### Navigation 3 KMP path

- Navigation 3 owns app routes, outer back stack, save/restore, scenes, and shell-level overlays.
- kmposable owns feature-local workflows, nodes, outputs, scripts, and headless tests.

### Non-Nav3 path

- kmposable can host the feature runtime directly in Compose with `NavFlowHost`.
- the outer shell remains your responsibility
- this path is valid, but no longer the primary product story

## Overlays

- If the overlay is part of the **app shell**, prefer Navigation 3 KMP.
- If the overlay is part of a **feature-local workflow**, kmposable overlays are still fine.
