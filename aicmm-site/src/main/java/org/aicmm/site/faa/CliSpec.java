package org.aicmm.site.faa;

import java.util.List;

/**
 * Declarative description of an agentic LLM CLI so a single {@link CliAssistProvider}
 * can drive any of them. Add a spec here (or via config) to support a new CLI —
 * the "call for contributions" extension point.
 *
 * @param id                  stable id (copilot/claude/gemini)
 * @param label               human label
 * @param binary              executable name (overridable via env)
 * @param baseArgs            always-passed args (e.g. --allow-all-tools)
 * @param promptFlag          flag that precedes the prompt (e.g. -p)
 * @param modelFlag           flag to select a model (nullable if unsupported)
 * @param models              suggested model ids
 * @param temperatureFlag     flag to set temperature (nullable if unsupported)
 * @param topPFlag            flag to set top-p / nucleus sampling (nullable if unsupported)
 * @param maxTokensFlag       flag to set max output tokens (nullable if unsupported)
 */
public record CliSpec(
        String id,
        String label,
        String binary,
        List<String> baseArgs,
        String promptFlag,
        String modelFlag,
        List<String> models,
        String temperatureFlag,
        String topPFlag,
        String maxTokensFlag) {

    public boolean supportsTemperature() { return temperatureFlag != null && !temperatureFlag.isBlank(); }
    public boolean supportsTopP() { return topPFlag != null && !topPFlag.isBlank(); }
    public boolean supportsMaxTokens() { return maxTokensFlag != null && !maxTokensFlag.isBlank(); }

    /** Built-in specs. GitHub Copilot is the supported default; others are optional. */
    public static List<CliSpec> builtins() {
        return List.of(
            new CliSpec("copilot", "GitHub Copilot CLI", env("AICMM_COPILOT_BIN", "copilot"),
                    List.of("--allow-all-tools"), "-p", "--model",
                    List.of("auto", "gpt-5.4", "gpt-5", "gpt-5-mini", "claude-sonnet-4.5", "claude-opus-4.1", "o4-mini"),
                    null, null, null),
            new CliSpec("claude", "Claude Code CLI", env("AICMM_CLAUDE_BIN", "claude"),
                    List.of(), "-p", "--model",
                    List.of("claude-sonnet-4-5", "claude-opus-4-1", "claude-haiku-4-5"),
                    null, null, null),
            new CliSpec("gemini", "Gemini CLI", env("AICMM_GEMINI_BIN", "gemini"),
                    List.of(), "-p", "-m",
                    List.of("gemini-2.5-pro", "gemini-2.5-flash"),
                    "--temperature", null, null)
        );
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }
}
