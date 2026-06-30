package org.aicmm.site.faa;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Bridges the AiCMM FAA to a local agentic LLM CLI described by a {@link CliSpec}.
 * Runs the CLI non-interactively (single prompt) and returns its output. The CLI is
 * launched in the repository root so Develop &amp; Extend mode can read/edit project code.
 */
public class CliAssistProvider implements AssistProvider {

    private final CliSpec spec;
    private final Path workingDir;
    private Boolean availableCache;
    private long availableCheckedAt;

    public CliAssistProvider(CliSpec spec, Path workingDir) { this.spec = spec; this.workingDir = workingDir; }

    @Override public String id() { return spec.id(); }
    @Override public String label() { return spec.label(); }
    @Override public boolean agentic() { return true; }
    @Override public List<String> models() { return spec.models(); }
    @Override public boolean supportsTemperature() { return spec.supportsTemperature(); }
    @Override public boolean supportsTopP() { return spec.supportsTopP(); }
    @Override public boolean supportsMaxTokens() { return spec.supportsMaxTokens(); }

    @Override
    public boolean available() {
        long now = System.currentTimeMillis();
        if (availableCache != null && now - availableCheckedAt < 30_000) return availableCache;
        boolean ok = false;
        try {
            Process p = new ProcessBuilder(spec.binary(), "--version")
                    .redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            ok = p.waitFor(8, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception ignored) { ok = false; }
        availableCache = ok; availableCheckedAt = now;
        return ok;
    }

    @Override
    public String ask(String primer, String page, String question, Tuning tuning) throws Exception {
        Tuning t = tuning != null ? tuning : Tuning.NONE;
        String prompt = primer + "\n\nCurrent page: " + page + "\nUser question: " + question;
        // Windows ProcessBuilder mangles a single argument that contains straight double quotes
        // (a long-standing JDK quoting limitation), which makes the CLI see split tokens. Newlines
        // and backticks are safe; only " is problematic. Normalise it to a typographic quote so the
        // whole prompt survives as one argv element. The model still emits valid JSON in its reply.
        String safePrompt = prompt.replace('"', '\u2019');
        List<String> cmd = new ArrayList<>();
        cmd.add(spec.binary());
        cmd.addAll(spec.baseArgs());
        if (t.model() != null && !t.model().isBlank() && spec.modelFlag() != null) {
            cmd.add(spec.modelFlag()); cmd.add(t.model());
        }
        if (t.temperature() != null && spec.supportsTemperature()) {
            cmd.add(spec.temperatureFlag()); cmd.add(String.valueOf(t.temperature()));
        }
        if (t.topP() != null && spec.supportsTopP()) {
            cmd.add(spec.topPFlag()); cmd.add(String.valueOf(t.topP()));
        }
        if (t.maxTokens() != null && spec.supportsMaxTokens()) {
            cmd.add(spec.maxTokensFlag()); cmd.add(String.valueOf(t.maxTokens()));
        }
        cmd.add(spec.promptFlag()); cmd.add(safePrompt);

        Process p = new ProcessBuilder(cmd)
                .directory(workingDir != null ? workingDir.toFile() : null)
                .redirectErrorStream(true).start();
        p.getOutputStream().close();
        StringBuilder out = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) { out.append(line).append('\n'); }
        }
        if (!p.waitFor(120, TimeUnit.SECONDS)) { p.destroyForcibly(); throw new RuntimeException(spec.label() + " timed out"); }
        String text = out.toString().trim();
        if (text.isEmpty()) throw new RuntimeException(spec.label() + " returned no output");
        return text;
    }
}
