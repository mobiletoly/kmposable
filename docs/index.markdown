---
layout: home
title: Kmposable Documentation
---

Kmposable is a headless-first workflow/runtime layer for Kotlin Multiplatform apps. Nodes
encapsulate business logic, `NavFlow` manages feature state transitions, and Compose (or any UI)
simply renders state. For Compose KMP apps, the recommended architecture is Navigation 3 KMP for
the app shell and kmposable for inner feature workflows.

## Quick Links

- [Overview]({{ site.baseurl }}/overview/) – philosophy, mental model, modules, samples.
- [Navigation 3 KMP]({{ site.baseurl }}/navigation3-kmp/) – recommended `0.3.x` architecture and dependency baseline.
- [Non-Nav3 Compose]({{ site.baseurl }}/non-nav3-compose/) – supported fallback path when you are not on Navigation 3.
- [Nav3 vs Non-Nav3]({{ site.baseurl }}/compose-paths/) – quick chooser for the two Compose paths.
- [Migration 0.2.x -> 0.3.x]({{ site.baseurl }}/migration-0-2-to-0-3/) – breaking changes and migration guidance.
- [Guides]({{ site.baseurl }}/guides/) – install, Compose integration, scripts, testing.
- [Reference]({{ site.baseurl }}/reference/) – API summaries for Node/NavFlow/renderer/test helpers.
- [Cookbook]({{ site.baseurl }}/cookbook/) – practical patterns (reactive and script-driven).

Looking for code right away? Clone the repo and explore:

- `sample-app-compose` – Navigation 3 KMP shell + inner kmposable feature flow.
- `sample-app-flowscript` – same UI but orchestrated via a NavFlow script.

Need help or want to contribute? File an issue on
[GitHub]({{ site.github.repository_url }}) or jump into the `spec_docs/` folder to review the latest
RFCs.
