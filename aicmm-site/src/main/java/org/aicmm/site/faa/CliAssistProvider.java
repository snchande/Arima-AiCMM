package org.aicmm.site.faa;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Bridges the AiCMM FAA to a local agentic LLM CLI described by a {@link CliSpec}.
 * Runs the CLI non-interactively (single prompt) and returns its output.
 */
public class CliAssistProvider implements AssistProvider {

    private final CliSpec spec;
    private Boolean availableCache;
    private long availableCheckedAt;

    public CliAssistProvider(CliSpec spec) { this.spec = spec; }

    @Override public String id() { return spec.id(); }
    @Override public String label() { return spec.label(); }
    @Override public boolean agentic() { return true; }
    @Override public List<String> models() { return spec.models(); }
    @Override public boolean supportsTemperature() { return spec.supportsTemperature(); }

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
    public String ask(String primer, String page, String question, String model, Double temperature) throws Exception {
        String prompt = primer + "\n\nCurrent page: " + page + "\nUser question: " + question;
        List<String> cmd = new ArrayList<>();
        cmd.add(spec.binary());
        cmd.addAll(spec.baseArgs());
        if (model != null && !model.isBlank() && spec.modelFlag() != null) {
            cmd.add(spec.modelFlag()); cmd.add(model);
        }
        if (temperature != null && spec.supportsTemperature()) {
            cmd.add(spec.temperatureFlag()); cmd.add(String.valueOf(temperature));
        }
        cmd.add(spec.promptFlag()); cmd.add(prompt);

        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
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
