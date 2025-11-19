---
layout: page
title: Specs & Decisions
permalink: /specs/
---

Detailed design documents live under [`spec_docs/`](https://github.com/mobiletoly/kmposable/tree/main/spec_docs).
Use this page as an index until each spec is converted into a fully rendered article.

| Topic | File | Notes |
| --- | --- | --- |
| NavFlow runtime rename + API | [REMEMBER_KMPOSABLE_RUNTIME_SPEC.md](https://github.com/mobiletoly/kmposable/blob/main/spec_docs/REMEMBER_KMPOSABLE_RUNTIME_SPEC.md) | Explains the `NavFlow` rename, public API surface, FlowFactory abstraction, and testing helpers. |
| NavHost integration rules | [NAVHOST_INTEGRATION_SUMMARY.md](https://github.com/mobiletoly/kmposable/blob/main/spec_docs/NAVHOST_INTEGRATION_SUMMARY.md) | Describes how NavHost owns top-level routes while Kmposable runs inner flows. |
| NavFlow scripts spec | [NAVFLOW_SCRIPT_SPEC.md](https://github.com/mobiletoly/kmposable/blob/main/spec_docs/NAVFLOW_SCRIPT_SPEC.md) | API contract for scripts, launch semantics, and helper requirements. |
| Script implementation checklist | [NAVFLOW_SCRIPT_IMPLEMENTATION.md](https://github.com/mobiletoly/kmposable/blob/main/spec_docs/NAVFLOW_SCRIPT_IMPLEMENTATION.md) | Tracks completed milestones for the script engine. |
| Script cookbook (source) | [NAVFLOW_SCRIPT_COOKBOOK.md](https://github.com/mobiletoly/kmposable/blob/main/spec_docs/NAVFLOW_SCRIPT_COOKBOOK.md) | Original recipes before they were copied to the site. |

Future RFCs (DI helpers, renderer DSL updates, etc.) will appear in this folder as they are drafted.
