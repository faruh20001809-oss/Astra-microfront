package ru.astrakhan.admin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.astrakhan.admin.entity.UserProgressProfile;
import ru.astrakhan.admin.repository.UserProgressProfileRepository;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserProgressService {
    private final UserProgressProfileRepository repository;
    private final ObjectMapper objectMapper;

    public Map<String, Object> getSnapshotByEmail(String email) {
        String normalized = normalizeEmail(email);
        Optional<UserProgressProfile> opt = repository.findByEmailIgnoreCase(normalized);
        if (opt.isEmpty()) {
            Map<String, Object> emptyResult = new LinkedHashMap<>();
            emptyResult.put("email", normalized);
            emptyResult.put("snapshot", new LinkedHashMap<>());
            emptyResult.put("updatedAt", null);
            return emptyResult;
        }
        UserProgressProfile profile = opt.get();
        Map<String, Object> snapshot = parseSnapshot(profile.getSnapshotJson());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("email", profile.getEmail());
        result.put("snapshot", snapshot);
        result.put("updatedAt", profile.getUpdatedAt() != null ? profile.getUpdatedAt().toString() : null);
        return result;
    }

    public Map<String, Object> upsertSnapshot(String email, Map<String, Object> snapshot) {
        String normalized = normalizeEmail(email);
        UserProgressProfile profile = repository.findByEmailIgnoreCase(normalized).orElseGet(() -> UserProgressProfile.builder()
                .email(normalized)
                .build());
        profile.setSnapshotJson(writeSnapshot(snapshot));
        profile.setUpdatedAt(LocalDateTime.now());
        UserProgressProfile saved = repository.save(profile);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("email", saved.getEmail());
        result.put("snapshot", parseSnapshot(saved.getSnapshotJson()));
        result.put("updatedAt", saved.getUpdatedAt() != null ? saved.getUpdatedAt().toString() : null);
        return result;
    }

    private static String normalizeEmail(String email) {
        String e = email != null ? email.trim().toLowerCase() : "";
        if (e.isBlank()) throw new IllegalArgumentException("Email обязателен.");
        if (!e.contains("@")) throw new IllegalArgumentException("Некорректный email.");
        return e;
    }

    private Map<String, Object> parseSnapshot(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private String writeSnapshot(Map<String, Object> snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot != null ? snapshot : new LinkedHashMap<>());
        } catch (Exception e) {
            throw new IllegalArgumentException("Snapshot должен быть валидным JSON.");
        }
    }
}
