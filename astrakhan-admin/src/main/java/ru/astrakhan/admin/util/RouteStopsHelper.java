package ru.astrakhan.admin.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Тематический контент остановок маршрута (JSON в {@code routes.waypoints}).
 *
 * Поддерживает два типа точек:
 * <ul>
 *   <li>POI-точки — привязаны к существующему POI через {@code poiId}</li>
 *   <li>Кастомные точки — {@code isCustom=true}, не связаны с POI,
 *       имеют собственные координаты и название</li>
 * </ul>
 */
@Slf4j
public final class RouteStopsHelper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RouteStopsHelper() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StopContent {
        public Long poiId;
        public String thematicDescription;
        public String description;
        public String videoUrls;
        public String audioUrls;
        public String imageUrl;
        public Integer durationMinutes;

        public Boolean isCustom;
        public String customName;
        public Double customLatitude;
        public Double customLongitude;
    }

    public static List<StopContent> parse(String waypointsJson) {
        if (waypointsJson == null || waypointsJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<StopContent> list = MAPPER.readValue(waypointsJson, new TypeReference<>() {});
            return list != null ? new ArrayList<>(list) : new ArrayList<>();
        } catch (Exception e) {
            log.warn("Could not parse route waypoints JSON: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public static String serialize(List<StopContent> contents) {
        if (contents == null || contents.isEmpty()) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(contents);
        } catch (Exception e) {
            log.warn("Could not serialize route waypoints: {}", e.getMessage());
            return "[]";
        }
    }

    public static Map<Long, StopContent> indexByPoiId(String waypointsJson) {
        return parse(waypointsJson).stream()
                .filter(s -> s.poiId != null && !Boolean.TRUE.equals(s.isCustom))
                .collect(Collectors.toMap(s -> s.poiId, s -> s, (a, b) -> b, LinkedHashMap::new));
    }

    /** Извлекает кастомные точки из JSON (те, у которых isCustom=true). */
    public static List<StopContent> extractCustomStops(String waypointsJson) {
        return parse(waypointsJson).stream()
                .filter(s -> Boolean.TRUE.equals(s.isCustom))
                .collect(Collectors.toList());
    }

    /**
     * Синхронизирует waypoints с poiIds, сохраняя порядок из входного JSON.
     * POI-точки привязываются к poiIds, кастомные точки сохраняются как есть.
     * Порядок определяется входным JSON (а не csv), что позволяет перемешивать
     * POI и кастомные точки в любой последовательности.
     */
    public static String syncWithPoiIds(String waypointsJson, String poiIdsCsv) {
        List<StopContent> incoming = parse(waypointsJson);
        Set<Long> allowedPoiIds = new HashSet<>(parsePoiIdsCsv(poiIdsCsv));

        List<StopContent> out = new ArrayList<>();
        for (StopContent c : incoming) {
            if (Boolean.TRUE.equals(c.isCustom)) {
                if (c.thematicDescription == null) c.thematicDescription = "";
                out.add(c);
            } else if (c.poiId != null && allowedPoiIds.contains(c.poiId)) {
                if (c.thematicDescription == null) c.thematicDescription = "";
                out.add(c);
                allowedPoiIds.remove(c.poiId);
            }
        }
        for (Long id : parsePoiIdsCsv(poiIdsCsv)) {
            if (allowedPoiIds.contains(id)) {
                StopContent c = new StopContent();
                c.poiId = id;
                c.thematicDescription = "";
                out.add(c);
                allowedPoiIds.remove(id);
            }
        }
        return serialize(out);
    }

    public static List<Long> parsePoiIdsCsv(String poiIds) {
        if (poiIds == null || poiIds.isBlank()) {
            return List.of();
        }
        List<Long> out = new ArrayList<>();
        for (String part : poiIds.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;
            try {
                out.add(Long.parseLong(trimmed));
            } catch (NumberFormatException ignored) {
                /* skip */
            }
        }
        return out;
    }

    public static String normalizeIncomingJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return "[]";
        }
        return serialize(parse(raw));
    }

    // ═══════ Media URL validation ═══════

    private static final Pattern URL_BASIC = Pattern.compile("^https?://\\S+$", Pattern.CASE_INSENSITIVE);

    private static final Pattern[] AUDIO_PATTERNS = {
            Pattern.compile("\\.(mp3|ogg|wav|aac|m4a|flac|wma|opus)(\\?.*)?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^https?://music\\.yandex\\.(ru|com)/", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^https?://(www\\.)?soundcloud\\.com/", Pattern.CASE_INSENSITIVE),
            Pattern.compile("/audio/", Pattern.CASE_INSENSITIVE),
    };

    public static boolean isValidVideoUrl(String url) {
        if (url == null || url.isBlank()) return true;
        String trimmed = url.trim();
        return URL_BASIC.matcher(trimmed).matches();
    }

    public static boolean isValidAudioUrl(String url) {
        if (url == null || url.isBlank()) return true;
        String trimmed = url.trim();
        if (!URL_BASIC.matcher(trimmed).matches()) return false;
        for (Pattern p : AUDIO_PATTERNS) {
            if (p.matcher(trimmed).find()) return true;
        }
        return false;
    }

    /** Валидирует все медиа-ссылки в тексте (по одной в строке). Возвращает список ошибок. */
    public static List<String> validateMediaUrls(String urlsText, String mediaType) {
        if (urlsText == null || urlsText.isBlank()) return List.of();
        List<String> errors = new ArrayList<>();
        String[] lines = urlsText.split("[\\r\\n,]+");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            boolean valid = "video".equals(mediaType) ? isValidVideoUrl(line) : isValidAudioUrl(line);
            if (!valid) {
                errors.add("Строка " + (i + 1) + ": недопустимый формат " + mediaType + " — " + line);
            }
        }
        return errors;
    }
}
