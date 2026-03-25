package ru.astrakhan.admin.repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.astrakhan.admin.entity.Feedback;
import java.time.LocalDateTime;
import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByStatus(Feedback.FeedbackStatus status);
    List<Feedback> findAllByOrderByCreatedAtDesc();
    long countByStatus(Feedback.FeedbackStatus status);

    List<Feedback> findByCreatedAtBetweenAndRespondedAtIsNotNull(LocalDateTime from, LocalDateTime toExclusive);

    @Query("SELECT f.status, COUNT(f) FROM Feedback f WHERE f.createdAt >= :from AND f.createdAt < :to GROUP BY f.status")
    List<Object[]> countByStatusInPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime toExclusive);

    @Query("SELECT f.subject, COUNT(f) FROM Feedback f WHERE f.createdAt >= :from AND f.createdAt < :to GROUP BY f.subject ORDER BY COUNT(f) DESC")
    List<Object[]> topSubjectsInPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime toExclusive, Pageable pageable);
}
