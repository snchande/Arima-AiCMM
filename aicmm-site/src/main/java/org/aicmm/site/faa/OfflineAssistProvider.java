package org.aicmm.site.faa;

import java.util.List;
import java.util.Locale;

/**
 * Always-available fallback that answers from built-in AiCMM knowledge — no LLM CLI
 * required. Deterministic, keyword-driven responses so the FAA is useful even with
 * zero tooling installed. When a CLI is present, {@link CliAssistProvider} takes over
 * for fully agentic answers.
 */
public class OfflineAssistProvider implements AssistProvider {

    @Override public String id() { return "offline"; }
    @Override public String label() { return "Built-in (offline, no CLI)"; }
    @Override public boolean available() { return true; }
    @Override public boolean agentic() { return false; }
    @Override public List<String> models() { return List.of(); }
    @Override public boolean supportsTemperature() { return false; }

    @Override
    public String ask(String primer, String page, String question, String model, Double temperature) {
        String q = question == null ? "" : question.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();

        if (has(q, "dimension", "12 ", "twelve", "score what")) {
            sb.append("AiCMM scores agents on **12 Level 0 dimensions** (0–5):\n")
              .append("Cognitive Core: autonomy, reasoning, memory, learning.\n")
              .append("Action & Integration: toolUse, collaboration, embodiment.\n")
              .append("Trust & Deployment: explainability, safety, interoperability, costEfficiency, domainAlignment.");
        } else if (has(q, "governance", "rule")) {
            sb.append("The **7 governance rules** must all pass, e.g. Autonomy ≤ Reasoning+1; ")
              .append("Autonomy ≥ 4 ⇒ Explainability ≥ 3 and Safety ≥ 3; Tool Use ≥ 4 ⇒ Reasoning ≥ 3 and Cost Efficiency ≥ 2; ")
              .append("Collaboration ≥ 4 ⇒ Interoperability ≥ 3; Embodiment ≥ 3 ⇒ Domain Alignment ≥ 3.");
        } else if (has(q, "agency", "is it an agent", "ladder", "agent?")) {
            sb.append("The **Agency Qualification Layer** (derived position 12) classifies how agentic a system is on a ")
              .append("ladder from -2 to +5. A system is an *agent* (level ≥ 0) only when Autonomy ≥ 2 and Reasoning ≥ 2; ")
              .append("otherwise it is a Reactive Assistant (-1) or Scripted Automation (-2). Levels 1–5 range from Basic to Humanoid Agent.");
        } else if (has(q, "create", "build a card", "brochure", "new card")) {
            sb.append("To create an Agent Card: open **Create Card**, fill the 12 scores (or paste your agent's docs), ")
              .append("and the site renders the radar + Agency footprint. With a CLI installed, ask `@aicmm Create an AiCMM ")
              .append("Agent Card from ./brochure.pdf`. Programmatically, use the `aicmm-core` library.");
        } else if (has(q, "score", "rate", "radar", "fingerprint", "footprint")) {
            sb.append("Scoring rates each of the 12 dimensions 0–5 with evidence, validates the 7 governance rules, ")
              .append("then derives the Agency level + Agency Index (0–100). The **radar fingerprint** visualises the 12 scores; ")
              .append("the **Agency barometer** shows the derived level. See the Catalog for examples.");
        } else if (has(q, "validate", "governance pass", "fail")) {
            sb.append("Validation checks schema conformance and the 7 governance rules, returning any failing rule. ")
              .append("Use the Create Card form, `POST /api/validate`, or a CLI: `@aicmm Validate governance and list failing rules`.");
        } else if (has(q, "level 1", "domain", "healthcare", "finance")) {
            sb.append("**Level 1** adds domain-specific radar charts (Healthcare, Finance, Manufacturing, Transportation, …) ")
              .append("layered on top of the 12 Level 0 dimensions — drill-downs, not replacements.");
        } else if (has(q, "what is aicmm", "about", "overview", "summar")) {
            sb.append("**AiCMM** (Agent Capability Maturity Model) is an open framework that classifies AI agents across ")
              .append("12 dimensions (0–5) plus a derived Agency Qualification Layer, producing a comparable capability ")
              .append("fingerprint and a machine-readable **Agent Card**.");
        } else if (has(q, "cli", "copilot", "install", "how do i use")) {
            sb.append("Install an agentic CLI to unlock live help: `npm install -g @github/copilot`, then run `copilot` in the ")
              .append("repo and type `@aicmm`. Once a CLI is detected, the FAA assistant becomes fully agentic. ")
              .append("Pick your CLI/model under the ⚙ settings.");
        } else {
            sb.append("I'm the built-in AiCMM assistant (offline mode). I can explain the 12 dimensions, the 7 governance ")
              .append("rules, the Agency Qualification ladder, and how to create/score/validate Agent Cards. ")
              .append("Install a Copilot/LLM CLI and pick it under ⚙ for fully agentic, page-aware help.");
        }

        sb.append("\n\n_(Offline answer — install and select an LLM CLI under ⚙ settings for agentic responses.)_");
        return sb.toString();
    }

    private static boolean has(String q, String... keys) {
        for (String k : keys) if (q.contains(k)) return true;
        return false;
    }
}
