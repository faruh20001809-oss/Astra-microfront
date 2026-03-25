package ru.astrakhan.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
public class PoiAiDescriptionService {

    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Value("${app.ai.enabled:false}")
    private boolean enabled;

    @Value("${app.ai.model:gpt-4o-mini}")
    private String model;

    public PoiAiDescriptionService(ObjectMapper objectMapper,
            @Value("${app.ai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${app.ai.api-key:}") String apiKey) {
        this.objectMapper = objectMapper;
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        RestClient.Builder b = RestClient.builder().baseUrl(base);
        if (apiKey != null && !apiKey.isBlank()) {
            b.defaultHeader("Authorization", "Bearer " + apiKey.trim());
        }
        b.defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        this.restClient = b.build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String generateDescription(Map<String, Object> body, String audienceRaw) {
        if (!enabled) {
            throw new IllegalStateException("Генерация описаний отключена (app.ai.enabled=false). Включите в настройках сервера.");
        }
        String audience = normalizeAudience(audienceRaw);
        String name = str(body.get("name"));
        String category = str(body.get("category"));
        String existing = str(body.get("description"));
        String architect = str(body.get("architect"));
        String material = str(body.get("material"));
        String style = str(body.get("style"));
        String tags = str(body.get("tags"));
        Integer year = body.get("foundedYear") instanceof Number ? ((Number) body.get("foundedYear")).intValue() : null;

        String styleHint = switch (audience) {
            case "children" -> "Пиши для детей 8–12 лет: короткие предложения, простые слова, один интересный факт.";
            case "academic" -> "Академический стиль: историко-культурная ценность, термины по делу, нейтральный тон.";
            default -> "Стиль экскурсовода: живо, понятно взрослому туристу, 2–4 абзаца.";
        };

        String userPrompt = """
                Объект (ТОИ) в Астрахани.
                Название: %s
                Категория: %s
                Год основания: %s
                Архитектор: %s
                Материал: %s
                Стиль: %s
                Теги: %s
                Текущее описание (можно переписать): %s

                %s
                Только текст описания на русском, без заголовков и кавычек в начале.
                """.formatted(
                name.isEmpty() ? "—" : name,
                category.isEmpty() ? "—" : category,
                year != null ? year.toString() : "не указан",
                architect.isEmpty() ? "—" : architect,
                material.isEmpty() ? "—" : material,
                style.isEmpty() ? "—" : style,
                tags.isEmpty() ? "—" : tags,
                existing.isEmpty() ? "нет" : existing,
                styleHint
        );

        Map<String, Object> req = new LinkedHashMap<>();
        req.put("model", model);
        req.put("temperature", 0.35);
        req.put("max_tokens", 900);
        req.put("messages", java.util.List.of(
                Map.of("role", "system", "content", "Ты помощник сотрудника музея/туризма в Астрахани. Пишешь точные, доброжелательные описания для каталога точек интереса."),
                Map.of("role", "user", "content", userPrompt)
        ));

        try {
            String raw = restClient.post()
                    .uri("/chat/completions")
                    .body(objectMapper.writeValueAsString(req))
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(raw);
            if (root.has("error")) {
                String msg = root.path("error").path("message").asText("Ошибка API");
                throw new IllegalStateException(msg);
            }
            String text = root.path("choices").path(0).path("message").path("content").asText("").trim();
            if (text.isEmpty()) {
                throw new IllegalStateException("Пустой ответ модели");
            }
            return text;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("AI description failed: {}", e.getMessage());
            throw new IllegalStateException("Не удалось обратиться к LLM: " + e.getMessage());
        }
    }

    private static String normalizeAudience(String a) {
        if (a == null) return "default";
        return switch (a.trim().toLowerCase()) {
            case "children", "детям" -> "children";
            case "academic", "академично" -> "academic";
            default -> "default";
        };
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }
}
