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
    private final List<AssistProvider> providers = new ArrayList<>();
    private final OfflineAssistProvider offline = new OfflineAssistProvider();
    private FaaSettings settings;

    public AssistService() {
        // Offline first (always available), then every built-in CLI.
        providers.add(offline);
        for (CliSpec spec : CliSpec.builtins()) providers.add(new CliAssistProvider(spec));
        this.settingsFile = Paths.get(System.getProperty("user.home"), ".aicmm", "faa-settings.json");
        this.settings = load();
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
    public Map<String, Object> answer(String page, String question, String overrideProvider,
                                      String overrideModel, Double overrideTemp) throws Exception {
        AssistProvider p = active(overrideProvider);
        String model = (overrideModel != null && !overrideModel.isBlank()) ? overrideModel : emptyToNull(settings.model);
        Double temp = overrideTemp != null ? overrideTemp : settings.temperature;
        String answer;
        boolean fellBack = false;
        try {
            answer = p.ask(PRIMER, page, question, model, temp);
        } catch (Exception e) {
            // CLI failed at runtime → fall back to offline so the user always gets help.
            answer = offline.ask(PRIMER, page, question, null, null)
                    + "\n\n_(" + p.label() + " failed: " + e.getMessage() + ")_";
            p = offline; fellBack = true;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("answer", answer);
        out.put("provider", p.id());
        out.put("providerLabel", p.label());
        out.put("agentic", p.agentic());
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
