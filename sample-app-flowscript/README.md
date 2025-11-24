# Sample FlowScript App

This sample mirrors the contacts UI from `sample-app-compose`, but navigation and data
loading are driven entirely by a NavFlow script.

## Running

- Android/Desktop: `./gradlew :sample-app-flowscript:composeApp:run`
- Tests: `./gradlew :sample-app-flowscript:composeApp:jvmTest`

## Where to look

- Flow script: `composeApp/src/commonMain/kotlin/.../ContactsFlowScript.kt`
- Compose entry: `composeApp/src/commonMain/kotlin/.../App.kt`
- Repository + nodes: same package as the script

Pair this README with `README-NAVFLOWSCRIPT.md` and `docs/NAVFLOW_SCRIPT_COOKBOOK.md`
for deeper context. The contacts flow now uses the `runFlow { step { ... } }` DSL built on
`NavFlowScriptScope`, so it reads like a sequential story instead of manual coroutine wiring.
