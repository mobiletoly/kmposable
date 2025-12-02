- [x] Design NodeHost/rememberNode composable that auto-handles scope + onAttach/onDetach + state/effects collection; add sample usage. *(helper + sample SettingsHost)*
- [x] Create OverlayHost/overlay DSL to bundle overlay nav flow creation, renderer registration, animations, and auto-close on Unit; prove with sample overlay migration. *(OverlayController/OverlayHost + docs + sample Settings overlay)*
- [x] (Dropped) Add built-in effect queue helper (node + Compose adapter); current pattern with `CollectEffects` + simple flows is sufficient.
- [x] Introduce Result helper utilities for ResultfulStatefulNode (loading/error wrapping, retries) and demonstrate usage in sample nodes (e.g., details/save flows) without touching external apps.
- [x] Provide parent-child state sync helper (child state mirror or binding) with a sample-friendly demonstration (doc snippet).
- [x] Simplify close semantics (auto-close on ok/Unit) for overlays; demonstrate in sample overlays.
- [x] Add type-safe auto-registration for nodeRenderer/OverlayNavFlow (convention-based Host binding) to reduce boilerplate; removed helper after review (manual `nodeRenderer` preferred).
- [x] Add testing hooks/sugar for overlay flows (push/await output) and document how to test dialogs/history/provider roster headlessly.
- [x] Integrate optional logging/error-report hook so nodes can emit standardized telemetry without repeated appLogger wiring.

Migration checklist (apply once APIs stabilize)
- [x] Migrate sample overlays to OverlayController/OverlayHost once APIs stabilize. *(Settings overlay uses it)*
- [x] Migrate sample hosts to CollectEffects/NodeHost once APIs stabilize. *(SettingsHost/Contacts hosts updated)*
