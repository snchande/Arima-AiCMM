package org.aicmm.site.faa;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry + settings for the FAA assistant. Holds the offline provider plus a
 * {@link CliAssistProvider} per built-in CLI spec, resolves the active provider
 * (with graceful fallback to offline), and persists user preferences.
 */
public class AssistService {

    private static final String PRIMER = """
            You are the AiCMM FAA Assistant, embedded in the AiCMM (Agent Capability Maturity Model)
            web site. AiCMM scores AI agents across 12 Level 0 dimensions (0-5): autonomy, reasoning,
            memory, learning, toolUse, collaboration, embodiment, explainability, safety, interoperability,
            costEfficiency, domainAlignment. A derived position-12 Agency Qualification Layer (-2..+5)
            classifies whether a system is even an agent. There are 7 governance rules and optional
            Level 1 domain scoring. Be concise, accurate, and page-aware. Help the user understand the
            page and act (create/score/validate Agent Cards).""";

    private final ObjectMapper mapper = new ObjectMapper();
    private final Path settingsFile;
    private final Path projectRoot;
    private final List<AssistProvider> providers = new ArrayList<>();
    private final OfflineAssistProvider offline = new OfflineAssistProvider();
    private FaaSettings settings;

    public AssistService(Path projectRoot) {
        this.projectRoot = projectRoot;
        // Offline first (always available), then every built-in CLI (launched in the repo root).
        providers.add(offline);
        for (CliSpec spec : CliSpec.builtins()) {
            // GitHub Copilot is driven via the @github/copilot-sdk bridge (reliable, no argv
            // quoting, no browser-opening sessionStart hook); other CLIs use the generic spec runner.
            if ("copilot".equals(spec.id())) providers.add(new CopilotSdkAssistProvider(spec, projectRoot));
            else providers.add(new CliAssistProvider(spec, projectRoot));
        }
        this.settingsFile = Paths.get(System.getProperty("user.home"), ".aicmm", "faa-settings.json");
        this.settings = load();
    }

    /** Develop &amp; Extend primer — the CLI has full repo access to change code/docs and open PRs. */
    private String developPrimer(String page) {
        String root = projectRoot != null ? projectRoot.toString() : System.getProperty("user.dir");
        return """
            You are the AiCMM FAA Assistant, running via a local agentic CLI with FULL read/write access
            to the AiCMM repository at %s. You help the user DEVELOP, EXTEND and CONTRIBUTE to AiCMM —
            for developers AND non-developers.

            You can: read/edit files; run shell commands; build with
            `mvn -q -pl aicmm-site -am clean package -DskipTests`; and restart the site by running
            `scripts/restart-aicmm.ps1` (or POST /api/admin/shutdown with header X-AiCMM-Token: aicmm-secret-restart,
            then relaunch the jar — the server self-takes-over its port).

            Contribution types you support:
              - Code changes (Java in aicmm-*; site assets in aicmm-site/src/main/resources/static).
              - Documentation (README.md, docs/, CLAUDE.md, GEMINI.md, .github/copilot-instructions.md).
              - Rating agents / creating Agent Cards: write examples/<name>-agent-card.json and validate
                via POST /api/validate. Great for non-developers.
              - Tests under each module's src/test.

            When changes are ready, you MUST protect the integrity of AiCMM before proposing them:
              1. Run the foundational integrity gate: `scripts/run-foundational-tests.ps1`
                 (use `-All` when you touched code outside aicmm-core). This locks the 7 governance
                 rules, the agent threshold, the Agency ladder (-2..+5), and the 12-dimension structure.
              2. If a change adds or alters framework behaviour, ADD or UPDATE tests under the relevant
                 module's src/test FIRST, so the integrity suite still covers the invariants.
              3. Only open a Pull Request when the gate prints `PASS` (exit 0). If it FAILS, do NOT open a
                 PR — fix the regression or revert, and tell the user what broke.

            Then OFFER to open a Pull Request: create a feature branch, commit with the trailer
            'Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>', push, and run
            `gh pr create`. ALWAYS paste the foundational-test summary block (the
            `<!-- AICMM-FOUNDATIONAL-TESTS -->` ... `<!-- /AICMM-FOUNDATIONAL-TESTS -->` section printed
            by the gate) into the PR description so reviewers can see the system stayed intact.
            Summarize the diff and confirm before pushing. Never rewrite published history.

            Be concrete and actually perform the steps with your tools, keeping the user informed.
            AiCMM scores 12 dimensions (autonomy, reasoning, memory, learning, toolUse, collaboration,
            embodiment, explainability, safety, interoperability, costEfficiency, domainAlignment) plus a
            derived Agency Qualification Layer, validated by 7 governance rules. Current page: %s.
            """.formatted(root, page);
    }

    /**
     * Page Action Protocol — appended to every primer so the assistant can change the page the
     * user is looking at in real time (fill forms, reword visible text, or reload after a
     * persistent source edit). {@code fillableFields} is a JSON description of the current page's
     * form fields supplied by the client, or null/blank when the page has none.
     */
    private String pageActionProtocol(String fillableFields) {
        String fields = (fillableFields != null && !fillableFields.isBlank())
                ? fillableFields
                : "(none detected on this page)";
        return """
            PAGE ACTION PROTOCOL — you can update the page the user is viewing in real time by emitting
            fenced blocks in your reply. The web UI parses and applies them instantly, then hides the raw
            blocks. Keep prose short and always confirm in one line what you changed. In every block emit
            STANDARD JSON (double-quoted keys and string values); the single quotes in the examples below
            are only to keep this prompt readable.

            1) Fill form fields — emit exactly one block:
            ```aicmm-fill
            { 'fields': { '<field-name-or-id>': '<value>' }, 'scores': { 'autonomy': 0, 'reasoning': 0 } }
            ```
            Use ONLY keys listed in FILLABLE FIELDS below. Include 'scores' (each 0-5) only when score-*
            fields are present. For select fields use one of the listed options.

            2) Reword / edit visible text — emit one block of find->replace pairs against text that is
            currently on the page:
            ```aicmm-edit
            [ { 'find': '<exact existing visible text>', 'replace': '<new text>' } ]
            ```
            This updates the live DOM only (a preview the user can see immediately).

            3) Reload — ONLY after you have edited the page's SOURCE files (Develop mode) so the change is
            persistent, emit a block containing just:
            ```aicmm-reload
            ```
            to refresh the page so the user sees the saved change.

            Rules: prefer aicmm-fill / aicmm-edit for anything the user can see right now. NEVER open a
            browser tab or create files merely to fill a form or reword on-screen text. Only touch source
            files (then aicmm-reload) when the user wants the change to persist.

            FILLABLE FIELDS for the current page:
            %s
            """.formatted(fields);
    }

    // ---- settings persistence ----
    private FaaSettings load() {
        try {
            if (Files.exists(settingsFile)) return mapper.readValue(settingsFile.toFile(), FaaSettings.class);
        } catch (Exception ignored) {}
        return new FaaSettings();
    }

    public synchronized FaaSettings getSettings() { return settings; }

    public synchronized FaaSettings saveSettings(FaaSettings s) {
        if (s.provider == null || s.provider.isBlank()) s.provider = "auto";
        this.settings = s;
        try {
            Files.createDirectories(settingsFile.getParent());
            mapper.writerWithDefaultPrettyPrinter().writeValue(settingsFile.toFile(), s);
        } catch (Exception ignored) {}
        return s;
    }

    // ---- provider resolution ----
    private AssistProvider byId(String id) {
        for (AssistProvider p : providers) if (p.id().equals(id)) return p;
        return null;
    }

    /** First available agentic CLI, else offline. */
    private AssistProvider firstAvailableCli() {
        for (AssistProvider p : providers) if (p.agentic() && p.available()) return p;
        return offline;
    }

    /** Resolve the active provider given settings + optional per-request override. */
    public AssistProvider active(String overrideId) {
        String want = (overrideId != null && !overrideId.isBlank()) ? overrideId : settings.provider;
        if (want == null || want.isBlank() || "auto".equals(want)) return firstAvailableCli();
        AssistProvider p = byId(want);
        if (p == null) return firstAvailableCli();
        if (!p.available()) return offline; // graceful rollback
        return p;
    }

    // ---- answer ----
    public Map<String, Object> answer(String page, String question, String mode, String history,
                                      String overrideProvider, String fillableFields) throws Exception {
        AssistProvider p = active(overrideProvider);
        boolean develop = "develop".equalsIgnoreCase(mode);
        String base = develop ? developPrimer(page) : PRIMER;
        String primer = base + "\n\n" + pageActionProtocol(fillableFields);
        AssistProvider.Tuning tuning = new AssistProvider.Tuning(
                emptyToNull(settings.model), settings.temperature, settings.topP, settings.maxTokens);
        String userContent = (history != null && !history.isBlank())
                ? "Conversation so far:\n" + history + "\n\nLatest: " + question
                : question;
        String answer;
        boolean fellBack = false;
        try {
            answer = p.ask(primer, page, userContent, tuning);
        } catch (Exception e) {
            // CLI failed at runtime → fall back to offline so the user always gets help.
            answer = offline.ask(primer, page, question, AssistProvider.Tuning.NONE)
                    + "\n\n_(" + p.label() + " failed: " + e.getMessage() + ")_";
            p = offline; fellBack = true;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("answer", answer);
        out.put("provider", p.id());
        out.put("providerLabel", p.label());
        out.put("agentic", p.agentic());
        out.put("mode", develop ? "develop" : "assist");
        out.put("fellBack", fellBack);
        return out;
    }

    /** Provider catalogue for the settings UI. */
    public Map<String, Object> catalogue() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (AssistProvider p : providers) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.id());
            m.put("label", p.label());
            m.put("available", p.available());
            m.put("agentic", p.agentic());
            m.put("models", p.models());
            m.put("supportsTemperature", p.supportsTemperature());
            m.put("supportsTopP", p.supportsTopP());
            m.put("supportsMaxTokens", p.supportsMaxTokens());
            list.add(m);
        }
        AssistProvider act = active(null);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("providers", list);
        out.put("settings", settings);
        out.put("active", act.id());
        out.put("activeAgentic", act.agentic());
        return out;
    }

    private static String emptyToNull(String s) { return (s == null || s.isBlank()) ? null : s; }
}
