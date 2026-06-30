// copilot-bridge.mjs — AiCMM FAA ⇄ GitHub Copilot CLI bridge.
//
// Drives the Copilot runtime through the official @github/copilot-sdk over JSON-RPC
// instead of shelling out to `copilot -p "<prompt>"`. Benefits:
//   * The prompt travels as a JSON-RPC message (via stdin), so there is no Windows
//     argv double-quote mangling and no need to sanitise the prompt.
//   * It spawns the Copilot *runtime*, not the interactive CLI, so the repo's
//     sessionStart hook (which opens a browser) never fires for FAA requests.
//
// Protocol: read one JSON object from stdin: { "prompt": "...", "model": "auto" }
// Write the assistant's text to stdout. Non-zero exit + message on stderr on error.

import { CopilotClient, approveAll } from "@github/copilot-sdk";

async function readStdin() {
  const chunks = [];
  for await (const c of process.stdin) chunks.push(c);
  return Buffer.concat(chunks).toString("utf8");
}

async function main() {
  const raw = await readStdin();
  const req = JSON.parse(raw || "{}");
  const prompt = typeof req.prompt === "string" ? req.prompt : "";
  if (!prompt.trim()) throw new Error("empty prompt");
  const model = req.model && req.model !== "auto" ? req.model : undefined;
  const timeoutMs = Number.isFinite(req.timeoutMs) ? req.timeoutMs : 120000;

  const client = new CopilotClient({ logLevel: "none" });
  await client.start();

  let out = "";
  let session;
  try {
    session = await client.createSession({
      ...(model ? { model } : {}),
      onPermissionRequest: approveAll,
    });

    const done = new Promise((resolve, reject) => {
      const timer = setTimeout(() => reject(new Error("bridge timed out")), timeoutMs);
      session.on("assistant.message", (e) => {
        if (e?.data?.content) out += e.data.content;
      });
      session.on("session.idle", () => { clearTimeout(timer); resolve(); });
      session.on("session.error", (e) => {
        clearTimeout(timer);
        reject(new Error(e?.data?.message || "session error"));
      });
    });

    await session.send({ prompt });
    await done;
  } finally {
    try { if (session) await session.disconnect(); } catch {}
    try { await client.stop(); } catch {}
  }

  process.stdout.write(out);
}

main().catch((err) => {
  process.stderr.write(String(err?.stack || err));
  process.exit(1);
});
