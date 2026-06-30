package org.aicmm.site.faa;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * GitHub Copilot CLI provider that drives the Copilot runtime through the official
 * {@code @github/copilot-sdk} (JSON-RPC) via a small Node bridge, instead of shelling out to
 * {@code copilot -p "<prompt>"}.
 *
 * <p>Why: passing a long prompt as a Windows argv element mangles embedded double quotes (a
 * JDK limitation) and the interactive CLI also fires the repo's {@code sessionStart} hook which
 * opens a browser on every invocation. The SDK bridge sidesteps both: the prompt travels as a
 * JSON-RPC message over stdin, and it spawns the Copilot <em>runtime</em> (no session hooks).
 */
public class CopilotSdkAssistProvider implements AssistProvider {

    private final CliSpec spec;       // reuses copilot id/label/models from the spec
    private final Path projectRoot;
    private final Path bridgeDir;
    private final Path bridgeScript;
    private final ObjectMapper mapper = new ObjectMapper();

    private Boolean availableCache;
    private long availableCheckedAt;
    private volatile Boolean depsReady;

    public CopilotSdkAssistProvider(CliSpec spec, Path projectRoot) {
        this.spec = spec;
        this.projectRoot = projectRoot;
        Path root = projectRoot != null ? projectRoot : Path.of(System.getProperty("user.dir"));
        this.bridgeDir = root.resolve("aicmm-site").resolve("faa-bridge");
        this.bridgeScript = bridgeDir.resolve("copilot-bridge.mjs");
    }

    @Override public String id() { return spec.id(); }
    @Override public String label() { return spec.label(); }
    @Override public boolean agentic() { return true; }
    @Override public List<String> models() { return spec.models(); }
    // The SDK tunes Copilot via model selection only (no temperature/top-p/max-tokens knobs).
    @Override public boolean supportsTemperature() { return false; }
    @Override public boolean supportsTopP() { return false; }
    @Override public boolean supportsMaxTokens() { return false; }

    private static String nodeBin() {
        String v = System.getenv("AICMM_NODE_BIN");
        return (v == null || v.isBlank()) ? "node" : v;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    @Override
    public boolean available() {
        long now = System.currentTimeMillis();
        if (availableCache != null && now - availableCheckedAt < 30_000) return availableCache;
        boolean ok = false;
        try {
            if (Files.exists(bridgeScript)) {
                Process p = new ProcessBuilder(nodeBin(), "--version").redirectErrorStream(true).start();
                p.getInputStream().readAllBytes();
                ok = p.waitFor(8, TimeUnit.SECONDS) && p.exitValue() == 0;
            }
        } catch (Exception ignored) { ok = false; }
        availableCache = ok; availableCheckedAt = now;
        return ok;
    }

    /** Lazily install the bridge's node_modules on first use (one-time on a fresh clone). */
    private synchronized void ensureDeps() throws Exception {
        if (depsReady != null && depsReady) return;
        if (Files.isDirectory(bridgeDir.resolve("node_modules").resolve("@github").resolve("copilot-sdk"))) {
            depsReady = true; return;
        }
        List<String> cmd = isWindows()
                ? List.of("cmd", "/c", "npm", "install", "--silent", "--no-audit", "--no-fund")
                : List.of("npm", "install", "--silent", "--no-audit", "--no-fund");
        Process p = new ProcessBuilder(cmd)
                .directory(bridgeDir.toFile())
                .redirectErrorStream(true)
                .start();
        p.getInputStream().readAllBytes();
        boolean done = p.waitFor(300, TimeUnit.SECONDS);
        if (!done) { p.destroyForcibly(); throw new RuntimeException("npm install for the Copilot SDK bridge timed out"); }
        depsReady = Files.isDirectory(bridgeDir.resolve("node_modules").resolve("@github").resolve("copilot-sdk"));
        if (!depsReady) throw new RuntimeException("Copilot SDK bridge dependencies failed to install");
    }

    @Override
    public String ask(String primer, String page, String question, Tuning tuning) throws Exception {
        ensureDeps();
        Tuning t = tuning != null ? tuning : Tuning.NONE;
        String prompt = primer + "\n\nCurrent page: " + page + "\nUser question: " + question;

        Map<String, Object> req = new LinkedHashMap<>();
        req.put("prompt", prompt);
        if (t.model() != null && !t.model().isBlank()) req.put("model", t.model());
        req.put("timeoutMs", 120_000);
        byte[] body = mapper.writeValueAsBytes(req);

        ProcessBuilder pb = new ProcessBuilder(nodeBin(), bridgeScript.toString())
                .directory((projectRoot != null ? projectRoot : bridgeDir).toFile());
        // Mark the child so any descendant script (sessionStart hooks, launch scripts) does NOT
        // open a browser — that should happen only for a genuine command-line launch.
        pb.environment().put("AICMM_FAA", "1");
        Process p = pb.start();

        try (OutputStream os = p.getOutputStream()) { os.write(body); }

        StringBuilder out = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) out.append(line).append('\n');
        }
        StringBuilder err = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) err.append(line).append('\n');
        }
        if (!p.waitFor(150, TimeUnit.SECONDS)) { p.destroyForcibly(); throw new RuntimeException(spec.label() + " (SDK) timed out"); }
        String text = out.toString().trim();
        if (p.exitValue() != 0 || text.isEmpty()) {
            String e = err.toString().trim();
            throw new RuntimeException(spec.label() + " (SDK) failed: "
                    + (e.isEmpty() ? "no output" : e.substring(0, Math.min(400, e.length()))));
        }
        return text;
    }
}
