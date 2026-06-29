package org.aicmm.site.faa;

import io.javalin.http.Context;

import java.util.Map;

/**
 * HTTP endpoints for the AiCMM FAA assistant:
 *  - POST /api/assist                 → ask a page-aware question
 *  - GET  /api/assist/providers       → provider catalogue + current settings
 *  - GET  /api/assist/settings        → current settings
 *  - POST /api/assist/settings        → update settings (default CLI, model, temperature)
 */
public class AssistController {

    private final AssistService service = new AssistService();

    @SuppressWarnings("unchecked")
    public void assist(Context ctx) {
        try {
            Map<String, Object> req = ctx.bodyAsClass(Map.class);
            String page = str(req.get("page"), "home");
            String question = str(req.get("question"), "").trim();
            if (question.isEmpty()) { ctx.status(400).json(Map.of("error", "Empty question")); return; }
            String provider = emptyToNull(str(req.get("provider"), ""));
            String model = emptyToNull(str(req.get("model"), ""));
            Double temp = req.get("temperature") instanceof Number n ? n.doubleValue() : null;
            ctx.json(service.answer(page, question, provider, model, temp));
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", "Assistant error: " + e.getMessage()));
        }
    }

    public void providers(Context ctx) { ctx.json(service.catalogue()); }

    public void getSettings(Context ctx) { ctx.json(service.getSettings()); }

    public void saveSettings(Context ctx) {
        try {
            FaaSettings s = ctx.bodyAsClass(FaaSettings.class);
            ctx.json(service.saveSettings(s));
        } catch (Exception e) {
            ctx.status(400).json(Map.of("error", "Invalid settings: " + e.getMessage()));
        }
    }

    private static String str(Object o, String def) { return o == null ? def : String.valueOf(o); }
    private static String emptyToNull(String s) { return (s == null || s.isBlank()) ? null : s; }
}
