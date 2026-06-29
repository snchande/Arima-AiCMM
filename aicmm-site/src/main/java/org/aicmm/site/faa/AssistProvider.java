package org.aicmm.site.faa;

import java.util.List;

/**
 * Pluggable backend for the AiCMM FAA (Floating Agentic Assistance) assistant.
 *
 * Two kinds of providers exist:
 *  - {@link OfflineAssistProvider} — always-available, deterministic answers from
 *    built-in AiCMM knowledge (the "rolled back" experience when no CLI is present).
 *  - {@link CliAssistProvider} — bridges to a local agentic LLM CLI (GitHub Copilot
 *    today; Claude Code / Gemini optionally if installed). Truly agentic.
 */
public interface AssistProvider {

    /** Stable id, e.g. "copilot", "claude", "gemini", "offline". */
    String id();

    /** Human label, e.g. "GitHub Copilot CLI". */
    String label();

    /** Whether usable on this machine right now (binary present / always for offline). */
    boolean available();

    /** Whether this provider runs a real LLM CLI (vs. the offline knowledge base). */
    boolean agentic();

    /** Selectable models for this provider (may be empty). */
    List<String> models();

    /** Whether the CLI honours a temperature setting. */
    boolean supportsTemperature();

    /**
     * Answer a page-contextual question.
     *
     * @param primer      system primer describing AiCMM + the current page
     * @param page        current page key
     * @param question    user question
     * @param model       selected model (nullable → provider default)
     * @param temperature selected temperature (nullable → provider default)
     */
    String ask(String primer, String page, String question, String model, Double temperature) throws Exception;
}
