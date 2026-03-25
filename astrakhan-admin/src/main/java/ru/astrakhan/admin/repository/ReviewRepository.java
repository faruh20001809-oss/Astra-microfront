package ru.astrakhan.admin.repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.astrakhan.admin.entity.Review;
import java.time.LocalDateTime;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByPoiId(Long poiId);
    List<Review> findByApprovedFalse();
    List<Review> findByPoiIdAndApprovedTrue(Long poiId);
    long countByApprovedFalse();

    List<Review> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime toExclusive);

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.createdAt >= :from AND r.createdAt < :to GROUP BY r.rating ORDER BY r.rating")
    List<Object[]> countByRatingInPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime toExclusive);

    @Query("SELECT r.poiId, COUNT(r) FROM Review r WHERE r.approved = false GROUP BY r.poiId ORDER BY COUNT(r) DESC")
    List<Object[]> topPoisByPendingReviews(Pageable pageable);
}
