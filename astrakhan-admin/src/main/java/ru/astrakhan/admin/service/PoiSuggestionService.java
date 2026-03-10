package ru.astrakhan.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.astrakhan.admin.entity.PoiSuggestion;
import ru.astrakhan.admin.repository.PoiSuggestionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PoiSuggestionService {

    private final PoiSuggestionRepository repository;

    public List<PoiSuggestion> findAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public List<PoiSuggestion> findByStatus(PoiSuggestion.SuggestionStatus status) {
        return repository.findByStatus(status);
    }

    public Optional<PoiSuggestion> findById(Long id) {
        return repository.findById(id);
    }

    public PoiSuggestion save(PoiSuggestion suggestion) {
        return repository.save(suggestion);
    }

    @Transactional
    public void approve(Long id, String adminNotes) {
        PoiSuggestion s = repository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        s.setStatus(PoiSuggestion.SuggestionStatus.APPROVED);
        s.setAdminNotes(adminNotes);
        s.setReviewedAt(LocalDateTime.now());
        repository.save(s);
    }

    @Transactional
    public void reject(Long id, String adminNotes) {
        PoiSuggestion s = repository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        s.setStatus(PoiSuggestion.SuggestionStatus.REJECTED);
        s.setAdminNotes(adminNotes);
        s.setReviewedAt(LocalDateTime.now());
        repository.save(s);
    }

    public long countNew() {
        return repository.countByStatus(PoiSuggestion.SuggestionStatus.NEW);
    }
}
