# App Modeler v0.4 — Softr Dashboard Start

Clean Java 21 + Vue 3 interpreter prototype.

## Changes from v0.3
- `Apply & Interpret` now returns to/stays on Administration after the generated UI reloads.
- The external `dashboard` layout now has a richer Softr-inspired composition: hero, KPI cards, entity list, and interpreter-status panel.
- The Softr visual treatment remains in external `config/ui/styles/softr.css`; the dashboard structure remains in external `config/ui/layouts/dashboard/layout.html`.
- DomainInterpreter, MenuInterpreter, LayoutInterpreter, StyleInterpreter, and HtmlAssembler remain separate.
- Default port remains 8090.

## Run
```bash
mvn compile exec:java
```
Open `http://localhost:8090`.

To try the new starting point, open Administration, choose `dashboard` for Content layout and `softr` for Style, then click **Apply & Interpret**. The app remains in Administration after reload so you can immediately try another combination.

## v0.5 light-sidebar update
- Preserves the v0.4 interpreter architecture.
- Softr style now uses semantic sidebar variables and renders a white sidebar in LIGHT appearance.
- DARK appearance retains a dark sidebar through variable overrides.
- Added `crm-domain.json` to `config/specialties` using the compatible CRM domain supplied for this build.

## v0.7 disk-runtime UI

- Generated browser UI is materialized to `runtime/index.html` on disk.
- Startup creates/replaces `runtime/index.html` from the selected/default external UI configuration.
- `Apply & Interpret` reinterprets the selected configuration, atomically replaces `runtime/index.html`, then reloads `/index.html` with a cache-busting query parameter.
- The generated page is served with no-cache headers for development reliability.
- The default layout includes one-level-at-a-time hierarchical menu drill-down.
- UML Diagram and Admin are placed in the persistent sidebar footer.
- External configuration remains the source of truth; `runtime/index.html` is generated output and should not be hand-edited.


## v0.7
- Added generic `GET /api/data?entity=...` endpoint backed by top-level domain `seedData`.
- Default layout renders selected leaf entities as dynamic record tables.
- No entity names are hard-coded in Java or Vue.
- Uses the user-provided current `config` folder as the configuration baseline.


## v0.8 — Composable JavaScript Runtime Prototype
Reusable browser behavior is represented by Java classes derived from `BaseJsRuntime`.
`DefaultJsRuntimePreset` composes navigation, entity-table and module-card capabilities.
`JsRuntimeComposer` materializes `runtime/appmodeler-runtime.js` under the collision-resistant
`window.__APPMODELER_RUNTIME_7F3A__` namespace. The external Vue layout delegates reusable
methods to that runtime while HTML and CSS remain external.


## v0.9 - Layout-owned JS/CSS composition

UI composition is now:
- HTML = selected `config/ui/layouts/<layout>/layout.html`
- JS = `config/ui/js/default.js` + selected `layout.js`
- CSS = `config/ui/css/default.css` + selected `layout.css`

`BaseJsRuntime` and `BaseStyleRuntime` only load and compose external files.
Style, Appearance, and Font selectors were removed from Admin. Theme/font
fine-tuning can be layered later through the layout CSS model.

## v0.10 - Generic UI library dependencies

Layouts can declare reusable UI dependencies in `layout.json`.

Example:
```json
{
  "id": "adminlte",
  "requires": ["bootstrap"]
}
```

Libraries live under `config/ui/libraries/<id>/` with a `library.json`.
The engine is library-agnostic.

Composition order:
- CSS: default.css -> declared libraries -> layout.css
- JS: default.js -> declared libraries -> layout.js

Bootstrap 5.3.6 is included as the first reusable library.
