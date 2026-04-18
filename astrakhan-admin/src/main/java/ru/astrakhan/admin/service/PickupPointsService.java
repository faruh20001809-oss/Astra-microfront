package ru.astrakhan.admin.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Единый справочник пунктов самовывоза: админка (модалка заказа) и публичный API для фронта (корзина).
 */
@Service
public class PickupPointsService {

    private static final List<Map<String, String>> POINTS = List.of(
            Map.of(
                    "id", "pickup-main",
                    "label", "Магазин «Живая история»",
                    "address", "г. Астрахань, ул. Кремлёвская, 2 (вход с набережной)",
                    "hours", "Ежедневно: 10:00–20:00"
            ),
            Map.of(
                    "id", "pickup-center",
                    "label", "Пункт выдачи «Центральный»",
                    "address", "г. Астрахань, ул. Кирова, 14",
                    "hours", "Пн–Сб: 09:00–19:00"
            )
    );

    public List<Map<String, String>> listAll() {
        return POINTS;
    }

    public Optional<Map<String, String>> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return POINTS.stream().filter(p -> id.equals(p.get("id"))).findFirst();
    }
}
