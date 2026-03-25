package ru.astrakhan.admin.service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.astrakhan.admin.entity.Feedback;
import ru.astrakhan.admin.repository.FeedbackRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

@Service @RequiredArgsConstructor
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;
    public List<Feedback> findAll() { return feedbackRepository.findAllByOrderByCreatedAtDesc(); }
    public List<Feedback> findByStatus(Feedback.FeedbackStatus status) { return feedbackRepository.findByStatus(status); }
    public Optional<Feedback> findById(Long id) { return feedbackRepository.findById(id); }
    public Feedback save(Feedback feedback) { return feedbackRepository.save(feedback); }
    public Feedback respond(Long id, String response) {
        Feedback fb = feedbackRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        fb.setAdminResponse(response); fb.setStatus(Feedback.FeedbackStatus.RESPONDED); fb.setRespondedAt(LocalDateTime.now());
        return feedbackRepository.save(fb);
    }
    public Feedback markAsRead(Long id) {
        Feedback fb = feedbackRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        fb.setStatus(Feedback.FeedbackStatus.READ); return feedbackRepository.save(fb);
    }
    public long countNew() { return feedbackRepository.countByStatus(Feedback.FeedbackStatus.NEW); }

    public List<Object[]> feedbackStatusCountsInPeriod(LocalDateTime from, LocalDateTime toExclusive) {
        return feedbackRepository.countByStatusInPeriod(from, toExclusive);
    }

    public List<Object[]> topFeedbackSubjectsInPeriod(LocalDateTime from, LocalDateTime toExclusive, int limit) {
        return feedbackRepository.topSubjectsInPeriod(from, toExclusive, PageRequest.of(0, Math.min(limit, 20)));
    }

    /** Среднее время ответа (часы) по обращениям с ответом в периоде. */
    public OptionalDouble averageResponseHoursInPeriod(LocalDateTime from, LocalDateTime toExclusive) {
        List<Feedback> list = feedbackRepository.findByCreatedAtBetweenAndRespondedAtIsNotNull(from, toExclusive);
        if (list.isEmpty()) {
            return OptionalDouble.empty();
        }
        return list.stream()
                .filter(f -> f.getCreatedAt() != null && f.getRespondedAt() != null)
                .mapToLong(f -> Duration.between(f.getCreatedAt(), f.getRespondedAt()).toHours())
                .average();
    }
}
