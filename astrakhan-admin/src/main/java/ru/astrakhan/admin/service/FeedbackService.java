package ru.astrakhan.admin.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.astrakhan.admin.entity.Feedback;
import ru.astrakhan.admin.repository.FeedbackRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
}
