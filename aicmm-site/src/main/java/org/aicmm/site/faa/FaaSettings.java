package org.aicmm.site.faa;

/**
 * Persisted FAA preferences. {@code provider} is the default provider id; {@code model}
 * and {@code temperature} are optional advanced LLM controls applied when supported.
 */
public class FaaSettings {
    public String provider = "auto";   // "auto" → first available CLI, else offline
    public String model = "";          // empty → provider default
    public Double temperature = null;  // null → provider default
    public Double topP = null;         // null → provider default (nucleus sampling)
    public Integer maxTokens = null;   // null → provider default (max output tokens)
    public boolean powerUser = false;  // reveals Develop & Extend mode + developer tools

    public FaaSettings() {}
}
