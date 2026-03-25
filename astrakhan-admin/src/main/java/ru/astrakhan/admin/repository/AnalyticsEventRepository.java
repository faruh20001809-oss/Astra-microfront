package ru.astrakhan.admin.repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.astrakhan.admin.entity.AnalyticsEvent;
import java.util.List;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, Long> {
    @Query("SELECT a.entityName, COUNT(a) as cnt FROM AnalyticsEvent a WHERE a.eventType = 'poi_view' GROUP BY a.entityId, a.entityName ORDER BY cnt DESC")
    List<Object[]> topViewedPois();
    @Query("SELECT a.entityName, COUNT(a) as cnt FROM AnalyticsEvent a WHERE a.eventType = 'poi_view' GROUP BY a.entityId, a.entityName ORDER BY cnt ASC")
    List<Object[]> leastViewedPois(Pageable pageable);
    @Query("SELECT a.entityName, COUNT(a) as cnt FROM AnalyticsEvent a WHERE a.eventType = 'route_view' GROUP BY a.entityId, a.entityName ORDER BY cnt DESC")
    List<Object[]> topViewedRoutes();
    @Query("SELECT a.entityName, COUNT(a) as cnt FROM AnalyticsEvent a WHERE a.eventType = 'route_view' GROUP BY a.entityId, a.entityName ORDER BY cnt ASC")
    List<Object[]> leastViewedRoutes(Pageable pageable);
    @Query("SELECT a.eventType, COUNT(a) FROM AnalyticsEvent a GROUP BY a.eventType")
    List<Object[]> countByEventType();
    long countByEventType(String eventType);
}
