package ru.astrakhan.admin.repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.astrakhan.admin.entity.PointOfInterest;
import java.time.LocalDateTime;
import java.util.List;

public interface PoiRepository extends JpaRepository<PointOfInterest, Long> {
    List<PointOfInterest> findByStatus(PointOfInterest.PoiStatus status);
    List<PointOfInterest> findByCategory(String category);
    @Query("SELECT p FROM PointOfInterest p WHERE p.status = 'PUBLISHED' ORDER BY p.viewsCount DESC")
    List<PointOfInterest> findTopByViews();
    @Query("SELECT p FROM PointOfInterest p WHERE p.status = 'PUBLISHED' ORDER BY p.rating DESC")
    List<PointOfInterest> findTopByRating();
    @Query("SELECT p.category, COUNT(p) FROM PointOfInterest p GROUP BY p.category")
    List<Object[]> countByCategory();
    @Query("SELECT COUNT(p) FROM PointOfInterest p WHERE p.status = :status")
    long countByStatus(@Param("status") PointOfInterest.PoiStatus status);

    @Query("SELECT p FROM PointOfInterest p WHERE p.status = 'PUBLISHED' " +
            "AND (p.publishAt IS NULL OR p.publishAt <= :now) " +
            "AND (p.unpublishAt IS NULL OR p.unpublishAt > :now)")
    List<PointOfInterest> findPubliclyVisibleAt(@Param("now") LocalDateTime now);

    @Query("SELECT p FROM PointOfInterest p WHERE p.status IN ('DRAFT', 'PENDING') " +
            "AND p.publishAt IS NOT NULL AND p.publishAt <= :now")
    List<PointOfInterest> findScheduledToPublish(@Param("now") LocalDateTime now);

    @Query("SELECT p FROM PointOfInterest p WHERE p.status = 'PUBLISHED' " +
            "AND p.unpublishAt IS NOT NULL AND p.unpublishAt <= :now")
    List<PointOfInterest> findScheduledToUnpublish(@Param("now") LocalDateTime now);

    @Query("SELECT p FROM PointOfInterest p WHERE p.status = 'PUBLISHED' AND p.id > :afterId " +
            "AND (p.publishAt IS NULL OR p.publishAt <= :now) " +
            "AND (p.unpublishAt IS NULL OR p.unpublishAt > :now) ORDER BY p.id ASC")
    List<PointOfInterest> findPublicVisibleIdAfter(@Param("afterId") long afterId, @Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT COALESCE(MAX(p.id), 0) FROM PointOfInterest p WHERE p.status = 'PUBLISHED' " +
            "AND (p.publishAt IS NULL OR p.publishAt <= :now) " +
            "AND (p.unpublishAt IS NULL OR p.unpublishAt > :now)")
    long maxPublicVisibleId(@Param("now") LocalDateTime now);
}
