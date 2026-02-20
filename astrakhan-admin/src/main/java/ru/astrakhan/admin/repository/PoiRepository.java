package ru.astrakhan.admin.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.astrakhan.admin.entity.PointOfInterest;
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
}
