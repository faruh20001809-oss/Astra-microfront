package ru.astrakhan.admin.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Тематический контент остановок маршрута (JSON в {@code routes.waypoints}).
 * Формат: [{ "poiId": 1, "thematicDescription": "...", "videoUrls": "url\\nurl", "audioUrls": "..." }]
 */
@Slf4j
public final class RouteStopsHelper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RouteStopsHelper() {}

    public static class StopContent {
        public Long poiId;
        public String thematicDescription;
        public String videoUrls;
        public String audioUrls;
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
                .filter(s -> s.poiId != null)
                .collect(Collectors.toMap(s -> s.poiId, s -> s, (a, b) -> b, LinkedHashMap::new));
    }

    /** Оставляет только точки из poiIds, сохраняя порядок списка id. */
    public static String syncWithPoiIds(String waypointsJson, String poiIdsCsv) {
        Map<Long, StopContent> byId = indexByPoiId(waypointsJson);
        List<Long> ordered = parsePoiIdsCsv(poiIdsCsv);
        List<StopContent> out = new ArrayList<>();
        for (Long id : ordered) {
            StopContent c = byId.getOrDefault(id, new StopContent());
            c.poiId = id;
            if (c.thematicDescription == null) c.thematicDescription = "";
            out.add(c);
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
}
