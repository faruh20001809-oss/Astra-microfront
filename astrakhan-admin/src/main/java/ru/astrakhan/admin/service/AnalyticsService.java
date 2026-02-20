package ru.astrakhan.admin.service;

import org.springframework.stereotype.Service;
import ru.astrakhan.admin.entity.AnalyticsEvent;
import ru.astrakhan.admin.repository.AnalyticsEventRepository;

import java.util.List;

/**
 * Сервис аналитики для отслеживания событий и статистики
 */
@Service
public class AnalyticsService {

    private final AnalyticsEventRepository repo;

    // 🔹 Явный конструктор вместо @RequiredArgsConstructor
    public AnalyticsService(AnalyticsEventRepository repo) {
        this.repo = repo;
    }

    /**
     * Отслеживает событие аналитики
     */
    public void trackEvent(String type, Long entityId, String entityName) {
        repo.save(AnalyticsEvent.builder()
                .eventType(type)
                .entityId(entityId)
                .entityName(entityName)
                .build());
    }

    /**
     * Топ популярных точек интереса по просмотрам
     */
    public List<Object[]> topViewedPois() {
        return repo.topViewedPois();
    }

    /**
     * Топ популярных маршрутов по просмотрам
     */
    public List<Object[]> topViewedRoutes() {
        return repo.topViewedRoutes();
    }

    /**
     * Количество событий по типам
     */
    public List<Object[]> countByEventType() {
        return repo.countByEventType();
    }

    /**
     * Количество событий конкретного типа
     */
    public long countEvents(String eventType) {
        return repo.countByEventType(eventType);
    }
}
