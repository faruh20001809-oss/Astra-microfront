package ru.astrakhan.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.astrakhan.admin.entity.PointOfInterest;
import ru.astrakhan.admin.repository.PoiRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PoiService {

    private final PoiRepository poiRepository;
    private final PoiPublicationNotificationService publicationNotificationService;

    public List<PointOfInterest> findAll() {
        return poiRepository.findAll();
    }

    public List<PointOfInterest> findByStatus(PointOfInterest.PoiStatus status) {
        return poiRepository.findByStatus(status);
    }

    public Optional<PointOfInterest> findById(Long id) {
        return poiRepository.findById(id);
    }

    /**
     * Видна ли точка гостю с учётом статуса и окон publishAt / unpublishAt.
     */
    public static boolean isPubliclyVisible(PointOfInterest p, LocalDateTime now) {
        if (p == null || p.getStatus() != PointOfInterest.PoiStatus.PUBLISHED) {
            return false;
        }
        if (p.getPublishAt() != null && p.getPublishAt().isAfter(now)) {
            return false;
        }
        return p.getUnpublishAt() == null || p.getUnpublishAt().isAfter(now);
    }

    public List<PointOfInterest> findPubliclyVisibleNow() {
        return poiRepository.findPubliclyVisibleAt(LocalDateTime.now());
    }

    public List<PointOfInterest> findPublicVisibleAfterId(long afterId, int limit) {
        return poiRepository.findPublicVisibleIdAfter(afterId, LocalDateTime.now(),
                PageRequest.of(0, Math.min(Math.max(limit, 1), 20)));
    }

    public long catalogRevision() {
        return poiRepository.maxPublicVisibleId(LocalDateTime.now());
    }

    @Transactional
    public PointOfInterest save(PointOfInterest poi) {
        PointOfInterest.PoiStatus prevStatus = null;
        Boolean prevNotified = null;
        if (poi.getId() != null) {
            Optional<PointOfInterest> existing = poiRepository.findById(poi.getId());
            if (existing.isPresent()) {
                PointOfInterest db = existing.get();
                if (db != poi) {
                    prevStatus = db.getStatus();
                    prevNotified = db.getNewsletterNotified();
                }
            }
        }
        PointOfInterest saved = poiRepository.save(poi);
        handleFirstPublicationNewsletter(saved, prevStatus, prevNotified);
        return saved;
    }

    @Transactional
    public PointOfInterest publish(Long id) {
        PointOfInterest poi = poiRepository.findById(id).orElseThrow(() -> new RuntimeException("POI not found: " + id));
        PointOfInterest.PoiStatus prev = poi.getStatus();
        Boolean prevNotified = poi.getNewsletterNotified();
        poi.setStatus(PointOfInterest.PoiStatus.PUBLISHED);
        PointOfInterest saved = poiRepository.save(poi);
        handleFirstPublicationNewsletter(saved, prev, prevNotified);
        return saved;
    }

    @Transactional
    public void applyScheduledPublicationWindow() {
        LocalDateTime now = LocalDateTime.now();
        for (PointOfInterest p : poiRepository.findScheduledToPublish(now)) {
            PointOfInterest.PoiStatus prev = p.getStatus();
            Boolean prevNotified = p.getNewsletterNotified();
            p.setStatus(PointOfInterest.PoiStatus.PUBLISHED);
            PointOfInterest saved = poiRepository.save(p);
            handleFirstPublicationNewsletter(saved, prev, prevNotified);
        }
        for (PointOfInterest p : poiRepository.findScheduledToUnpublish(now)) {
            p.setStatus(PointOfInterest.PoiStatus.ARCHIVED);
            poiRepository.save(p);
        }
    }

    private void handleFirstPublicationNewsletter(PointOfInterest saved,
            PointOfInterest.PoiStatus previousStatus, Boolean previousNewsletterNotified) {
        if (saved.getStatus() != PointOfInterest.PoiStatus.PUBLISHED) {
            return;
        }
        if (previousStatus == PointOfInterest.PoiStatus.PUBLISHED) {
            return;
        }
        if (Boolean.TRUE.equals(previousNewsletterNotified)) {
            return;
        }
        if (Boolean.TRUE.equals(saved.getNewsletterNotified())) {
            return;
        }
        saved.setNewsletterNotified(true);
        poiRepository.save(saved);
        publicationNotificationService.sendNewPoiToSubscribersAsync(saved);
    }

    public void deleteById(Long id) {
        poiRepository.deleteById(id);
    }

    /** Все записи со статусом PUBLISHED (админка, маршруты), без фильтра по датам. */
    public List<PointOfInterest> findPublished() {
        return poiRepository.findByStatus(PointOfInterest.PoiStatus.PUBLISHED);
    }

    public List<PointOfInterest> findTopByViews() {
        return poiRepository.findTopByViews();
    }

    public List<PointOfInterest> findTopByRating() {
        return poiRepository.findTopByRating();
    }

    public List<Object[]> countByCategory() {
        return poiRepository.countByCategory();
    }

    public long countByStatus(PointOfInterest.PoiStatus status) {
        return poiRepository.countByStatus(status);
    }

    public PointOfInterest archive(Long id) {
        PointOfInterest poi = poiRepository.findById(id).orElseThrow(() -> new RuntimeException("POI not found: " + id));
        poi.setStatus(PointOfInterest.PoiStatus.ARCHIVED);
        return poiRepository.save(poi);
    }

    public void incrementViews(Long id) {
        poiRepository.findById(id).ifPresent(poi -> {
            poi.setViewsCount(poi.getViewsCount() + 1);
            poiRepository.save(poi);
        });
    }
}
