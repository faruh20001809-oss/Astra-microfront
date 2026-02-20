package ru.astrakhan.admin.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.astrakhan.admin.entity.Feedback;
import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByStatus(Feedback.FeedbackStatus status);
    List<Feedback> findAllByOrderByCreatedAtDesc();
    long countByStatus(Feedback.FeedbackStatus status);
}
