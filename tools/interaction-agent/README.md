# Interaction Agent — local human-in-the-loop UI

A tiny, dependency-free local web server that turns any agent question into an
interactive web page, captures the user's submission, and returns it as JSON.
Generic — icon picking is just one use case.

## How it works
1. Write `prompt.json` describing the question.
2. Start the server: `node server.js` (serves http://localhost:8099).
3. User opens the page, selects, and clicks Submit.
4. Server writes `response.json` and exits. The agent reads `response.json`.

## prompt.json schema
```json
{
  "title": "string",
  "subtitle": "string",
  "type": "single | multi | text",
  "submitLabel": "Submit",
  "assetsDir": "path/to/images",      // optional, served under /assets
  "options": [{ "id": "A", "label": "...", "desc": "...", "image": "a.png" }]
}
```

## response.json
- single → `{ "choice": "A" }`
- multi  → `{ "choices": ["A","B"] }`
- text   → `{ "text": "..." }`

## Notes
- Port override: `PORT=8100 node server.js`.
- Server self-terminates ~0.4s after submit and logs `RESPONSE:{...}`.
- No external packages required.
