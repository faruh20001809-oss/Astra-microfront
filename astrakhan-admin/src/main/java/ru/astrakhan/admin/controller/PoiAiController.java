package ru.astrakhan.admin.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.astrakhan.admin.service.PoiAiDescriptionService;
import ru.astrakhan.admin.service.PoiImageAnalysisService;
import ru.astrakhan.admin.service.PoiService;

import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "Admin POI AI", description = "Генерация описаний и анализ фото (требуется сессия админки)")
@RestController
@RequestMapping("/admin/api/poi")
@RequiredArgsConstructor
public class PoiAiController {

    private final PoiAiDescriptionService poiAiDescriptionService;
    private final PoiImageAnalysisService poiImageAnalysisService;
    private final PoiService poiService;

    @PostMapping("/ai-description")
    public ResponseEntity<Map<String, Object>> aiDescription(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> b = body != null ? body : Map.of();
        if (!poiAiDescriptionService.isEnabled()) {
            return ResponseEntity.ok(Map.of(
                    "ok", false,
                    "error", "Генерация отключена на сервере (app.ai.enabled=false). Укажите ключ и включите опцию в конфигурации."
            ));
        }
        try {
            String audience = b.get("audience") != null ? String.valueOf(b.get("audience")) : "default";
            String text = poiAiDescriptionService.generateDescription(b, audience);
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("ok", true);
            res.put("text", text);
            return ResponseEntity.ok(res);
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/image-analyze")
    public ResponseEntity<Map<String, Object>> imageAnalyze(@RequestBody Map<String, Object> body) {
        Object idObj = body != null ? body.get("poiId") : null;
        if (idObj == null) {
            return ResponseEntity.ok(Map.of("ok", false, "error", "Укажите poiId"));
        }
        long poiId = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(String.valueOf(idObj));
        if (!poiImageAnalysisService.isEnabled()) {
            return ResponseEntity.ok(Map.of(
                    "ok", false,
                    "error", "Анализ изображений отключён (app.ai.enabled=false)."
            ));
        }
        return poiService.findById(poiId)
                .map(poi -> {
                    try {
                        Map<String, String> r = poiImageAnalysisService.analyzePoiImage(poi);
                        Map<String, Object> res = new LinkedHashMap<>();
                        res.put("ok", true);
                        res.put("alt", r.getOrDefault("alt", ""));
                        res.put("tags", r.getOrDefault("tags", ""));
                        return ResponseEntity.ok(res);
                    } catch (IllegalStateException e) {
                        return ResponseEntity.ok(Map.<String, Object>of("ok", false, "error", e.getMessage()));
                    }
                })
                .orElse(ResponseEntity.ok(Map.of("ok", false, "error", "Точка не найдена")));
    }
}
