package ru.astrakhan.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.astrakhan.admin.entity.PoiSuggestion;

import java.util.List;

public interface PoiSuggestionRepository extends JpaRepository<PoiSuggestion, Long> {

    List<PoiSuggestion> findByStatus(PoiSuggestion.SuggestionStatus status);

    List<PoiSuggestion> findAllByOrderByCreatedAtDesc();

    long countByStatus(PoiSuggestion.SuggestionStatus status);
}
