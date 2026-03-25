package ru.astrakhan.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.astrakhan.admin.entity.PointOfInterest;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class PoiImageAnalysisService {

    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Value("${app.ai.enabled:false}")
    private boolean enabled;

    @Value("${app.ai.vision-model:gpt-4o-mini}")
    private String visionModel;

    public PoiImageAnalysisService(ObjectMapper objectMapper,
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

    /**
     * Анализ фото ТОИ: краткий alt-текст и предложение тегов (через запятую).
     */
    public Map<String, String> analyzePoiImage(PointOfInterest poi) {
        if (!enabled) {
            throw new IllegalStateException("Анализ изображений отключён (app.ai.enabled=false).");
        }
        byte[] data = poi.getImageData();
        if (data == null || data.length == 0) {
            throw new IllegalStateException("У точки нет загруженного изображения в базе.");
        }
        String mime = guessMime(poi.getImageFilename());
        String b64 = Base64.getEncoder().encodeToString(data);
        String dataUrl = "data:" + mime + ";base64," + b64;

        String instruction = """
                Ты видишь фото объекта (точка интереса). Кратко опиши, что на снимке (alt-текст для сайта, до 200 символов).
                Предложи 5–12 коротких тегов через запятую на русском (существительные/прилагательные).
                Ответ строго в одном JSON-объекте без markdown: {"alt":"...","tags":"тег1, тег2"}
                Контекст объекта: %s, категория: %s
                """.formatted(
                poi.getName() != null ? poi.getName() : "",
                poi.getCategory() != null ? poi.getCategory() : ""
        );

        List<Map<String, Object>> content = List.of(
                Map.of("type", "text", "text", instruction),
                Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))
        );
        Map<String, Object> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", content);

        Map<String, Object> req = new LinkedHashMap<>();
        req.put("model", visionModel);
        req.put("max_tokens", 400);
        req.put("messages", List.of(userMsg));

        try {
            String raw = restClient.post()
                    .uri("/chat/completions")
                    .body(objectMapper.writeValueAsString(req))
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(raw);
            if (root.has("error")) {
                throw new IllegalStateException(root.path("error").path("message").asText("Ошибка API"));
            }
            String reply = root.path("choices").path(0).path("message").path("content").asText("").trim();
            String jsonBlob = extractJsonObject(reply);
            JsonNode parsed = objectMapper.readTree(jsonBlob);
            String alt = parsed.path("alt").asText("").trim();
            String tags = parsed.path("tags").asText("").trim();
            Map<String, String> out = new LinkedHashMap<>();
            out.put("alt", alt);
            out.put("tags", tags);
            return out;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Vision analyze failed: {}", e.getMessage());
            throw new IllegalStateException("Анализ изображения не удался: " + e.getMessage());
        }
    }

    private static String extractJsonObject(String reply) {
        int i = reply.indexOf('{');
        int j = reply.lastIndexOf('}');
        if (i >= 0 && j > i) {
            return reply.substring(i, j + 1);
        }
        return reply;
    }

    private static String guessMime(String filename) {
        if (filename == null) return "image/jpeg";
        String f = filename.toLowerCase();
        if (f.endsWith(".png")) return "image/png";
        if (f.endsWith(".gif")) return "image/gif";
        if (f.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }
}
