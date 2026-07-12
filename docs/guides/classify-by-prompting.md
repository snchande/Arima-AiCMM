# Classify Your Agent with AiCMM — No Install, Just Prompt

You do **not** need to clone, build, or run AiCMM to use it. The **Agent Capability
Maturity Model** is a framework of rules and definitions — so any agentic CLI
(GitHub Copilot CLI, Claude Code, Gemini CLI, Cursor, etc.) can apply it for you.

**How to use it (30 seconds):**

1. Open your favorite agentic CLI **inside the repository of the agent you built**.
2. Paste the **prompt below**.
3. Replace the final section with your agent's code, README, system prompt, or a
   plain-English description — or just say *"analyze this repository."*
4. The CLI returns a full **AiCMM Agent Card**: 12 scored dimensions with evidence,
   a governance check, and the derived **Agency Qualification** level.

---

## The Prompt (copy everything in the box)

````text
You are an AiCMM (Agent Capability Maturity Model) assessor. Classify the AI agent
described at the bottom of this message using the framework below. Score ONLY from
observable evidence (code, docs, prompts, logs, tests). Never inflate scores.

## Level 0 — 12 Dimensions (fixed positions), each scored 0–5
Cognitive Core:      0 autonomy · 1 reasoning · 2 memory · 3 learning
Action & Integration:4 toolUse · 5 collaboration · 6 embodiment
Trust & Deployment:  7 explainability · 8 safety · 9 interoperability · 10 costEfficiency · 11 domainAlignment

Scoring scale: 0 Absent · 1 Basic/hardcoded · 2 Intermediate/structured ·
3 Advanced with guardrails · 4 Expert within boundaries · 5 Mastery with self-governance.

## 7 Governance Rules (ALL must pass; report each as PASS / FAIL / N/A)
1. autonomy <= reasoning + 1
2. autonomy >= 4  requires explainability >= 3
3. autonomy >= 4  requires safety >= 3
4. collaboration >= 4  requires interoperability >= 3
5. toolUse >= 4  requires costEfficiency >= 2
6. embodiment >= 3  requires domainAlignment >= 3
7. toolUse >= 4  requires reasoning >= 3

## Agency Qualification (derived Position 12 — never hand-scored)
Let minCore = min(autonomy,reasoning,memory,learning);
trustOk = governanceAllPass AND safety>=3 AND explainability>=3;
avg12 = mean of all 12 scores; avgExEm = mean of the 11 non-embodiment scores.
Decide the level top-down:
- If autonomy < 2 OR reasoning < 2 -> NON-AGENT: level -1 (REACTIVE_ASSISTANT) if reasoning >= 1, else level -2 (SCRIPTED_AUTOMATION).
- Else if NOT trustOk -> level 0 (PROTO_AGENT).
- Else if embodiment >= 5 AND minCore >= 5 AND avg12 >= 4.8 -> level 5 (HUMANOID_AGENT).
- Else if minCore >= 5 AND avgExEm >= 4.5 -> level 4 (HUMAN_LEVEL_AGENT).
- Else if avg12 >= 4.0 AND reasoning>=4 AND autonomy>=4 AND memory>=4 -> level 3 (GENERALIZED_AGENT).
- Else if reasoning >= 4 AND safety>=3 AND explainability>=3 AND avg12 >= 3.0 -> level 2 (ADVANCED_AGENT).
- Else -> level 1 (BASIC_AGENT).
An agent "qualifies" (isAgent = true) only when level >= 0.

## Your output
1. A short paragraph on what the agent is and how you gathered evidence.
2. A table: dimension | position | score (0–5) | confidence (low/med/high) | one-line evidence.
3. Totals: sum /60, average /5.
4. Governance results for all 7 rules (PASS/FAIL/N/A with the compared values).
5. Agency Qualification: level, code, label, isAgent, and a one-line rationale.
6. Finally, emit the full result as an AiCMM Agent Card JSON object with keys:
   agent, capabilityProfile (12 dimensions with position/score/confidence/evidence),
   governanceValidation, and agencyQualification.

Be rigorous: if evidence is missing for a dimension, score it low and mark confidence "low".

## AGENT TO CLASSIFY
<Paste your agent's code, README, system prompt, tool list, or description here —
or write: "Analyze the agent implemented in this repository.">
````

---

## What you get back

A complete, evidence-based classification — the same shape AiCMM produces natively:
scores for all 12 dimensions, a governance pass/fail report, and the **Agency
Qualification** verdict that tells you whether your system is a *scripted automation*,
a *reactive assistant*, or a genuinely *qualified agent* (Basic → Humanoid).

**Tips**
- Point the CLI at real evidence (source files, prompts, eval logs) for higher-confidence scores.
- Ask for **Level 1 domain scoring** (e.g. Healthcare, Finance) if you deploy in a regulated domain.
- Save the JSON as `examples/<your-agent>-agent-card.json` if you later adopt the full toolkit.
- Want the interactive site, MCP server, and catalog? See the
  [AiCMM repository](https://github.com/snchande/Arima-AiCMM).

*AiCMM is open source (MIT). This prompt-only workflow requires no installation.*
