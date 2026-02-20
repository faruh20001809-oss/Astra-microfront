package ru.astrakhan.admin.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.astrakhan.admin.entity.Review;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByPoiId(Long poiId);
    List<Review> findByApprovedFalse();
    List<Review> findByPoiIdAndApprovedTrue(Long poiId);
    long countByApprovedFalse();
}
