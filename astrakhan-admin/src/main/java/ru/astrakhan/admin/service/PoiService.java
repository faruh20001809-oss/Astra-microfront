package ru.astrakhan.admin.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.astrakhan.admin.entity.PointOfInterest;
import ru.astrakhan.admin.repository.PoiRepository;
import java.util.List;
import java.util.Optional;

@Service @RequiredArgsConstructor
public class PoiService {
    private final PoiRepository poiRepository;
    public List<PointOfInterest> findAll() { return poiRepository.findAll(); }
    public List<PointOfInterest> findByStatus(PointOfInterest.PoiStatus status) { return poiRepository.findByStatus(status); }
    public Optional<PointOfInterest> findById(Long id) { return poiRepository.findById(id); }
    public PointOfInterest save(PointOfInterest poi) { return poiRepository.save(poi); }
    public void deleteById(Long id) { poiRepository.deleteById(id); }
    public List<PointOfInterest> findPublished() { return poiRepository.findByStatus(PointOfInterest.PoiStatus.PUBLISHED); }
    public List<PointOfInterest> findTopByViews() { return poiRepository.findTopByViews(); }
    public List<PointOfInterest> findTopByRating() { return poiRepository.findTopByRating(); }
    public List<Object[]> countByCategory() { return poiRepository.countByCategory(); }
    public long countByStatus(PointOfInterest.PoiStatus status) { return poiRepository.countByStatus(status); }
    public PointOfInterest publish(Long id) {
        PointOfInterest poi = poiRepository.findById(id).orElseThrow(() -> new RuntimeException("POI not found: " + id));
        poi.setStatus(PointOfInterest.PoiStatus.PUBLISHED);
        return poiRepository.save(poi);
    }
    public PointOfInterest archive(Long id) {
        PointOfInterest poi = poiRepository.findById(id).orElseThrow(() -> new RuntimeException("POI not found: " + id));
        poi.setStatus(PointOfInterest.PoiStatus.ARCHIVED);
        return poiRepository.save(poi);
    }
    public void incrementViews(Long id) {
        poiRepository.findById(id).ifPresent(poi -> { poi.setViewsCount(poi.getViewsCount() + 1); poiRepository.save(poi); });
    }
}
