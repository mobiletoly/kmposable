---
layout: home
title: Kmposable Documentation
---

Kmposable is a headless-first navigation/runtime for Kotlin Multiplatform apps. Nodes encapsulate
business logic, NavFlow manages the stack, Compose (or any UI) simply renders state. This site
collects the guides, references, and recipes that were previously scattered across README files.

## Quick Links

- [Overview]({{ site.baseurl }}/overview/) – philosophy, mental model, modules, samples.
- [Guides]({{ site.baseurl }}/guides/) – install, Compose integration, scripts, testing.
- [Reference]({{ site.baseurl }}/reference/) – API summaries for Node/NavFlow/renderer/test helpers.
- [Cookbook]({{ site.baseurl }}/cookbook/) – practical patterns (reactive and script-driven).

Looking for code right away? Clone the repo and explore:

- `sample-app-compose` – NavHost + Kmposable tabs with a reactive flow.
- `sample-app-flowscript` – same UI but orchestrated via a NavFlow script.

Need help or want to contribute? File an issue on
[GitHub]({{ site.github.repository_url }}) or jump into the `spec_docs/` folder to review the latest
RFCs.
