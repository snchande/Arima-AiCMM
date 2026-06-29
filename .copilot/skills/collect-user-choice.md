---
name: collect-user-choice
description: "Serve a question (image cards, multi-select, or free text) as a local web page, let the user select and submit, and read the choice back as JSON. Generic human-in-the-loop input — use for icon/design picks, approvals, or any visual selection."
allowed-tools:
  - create
  - powershell
  - view
  - read_powershell
---

# Collect User Choice

## Purpose
Get a real selection from the user via a browser page and receive it back as data. Powered by `tools/interaction-agent/server.js`.

## Steps
1. Create `tools/interaction-agent/prompt.json`:
   ```json
   { "title":"Pick one", "subtitle":"Click then Submit", "type":"single",
     "submitLabel":"Submit", "assetsDir":"../../docs/branding",
     "options":[{"id":"A","label":"Option A","desc":"...","image":"variant-A.png"}] }
   ```
2. Clear old result: `Remove-Item tools/interaction-agent/response.json -EA SilentlyContinue`.
3. Run server (async) and open browser:
   ```bash
   cd tools/interaction-agent && node server.js   # http://localhost:8099
   ```
4. Wait for the exit notification; then read `tools/interaction-agent/response.json`.
5. `{choice}` / `{choices:[]}` / `{text}` → proceed with the user's decision.

## Types
- `single` — radio cards → `{choice}`
- `multi` — toggle cards → `{choices:[]}`
- `text` — textarea → `{text}`

No dependencies. Override port via `PORT`. See `tools/interaction-agent/README.md`.
