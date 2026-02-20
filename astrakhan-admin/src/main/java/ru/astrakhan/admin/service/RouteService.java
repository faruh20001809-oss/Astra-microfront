package ru.astrakhan.admin.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.astrakhan.admin.entity.Route;
import ru.astrakhan.admin.repository.RouteRepository;
import java.util.List;
import java.util.Optional;

@Service @RequiredArgsConstructor
public class RouteService {
    private final RouteRepository routeRepository;
    public List<Route> findAll() { return routeRepository.findAll(); }
    public List<Route> findPublished() { return routeRepository.findByPublishedTrue(); }
    public Optional<Route> findById(Long id) { return routeRepository.findById(id); }
    public Route save(Route route) { return routeRepository.save(route); }
    public void deleteById(Long id) { routeRepository.deleteById(id); }
    public Route publish(Long id) {
        Route r = routeRepository.findById(id).orElseThrow(() -> new RuntimeException("Route not found"));
        r.setPublished(true); return routeRepository.save(r);
    }
    public Route unpublish(Long id) {
        Route r = routeRepository.findById(id).orElseThrow(() -> new RuntimeException("Route not found"));
        r.setPublished(false); return routeRepository.save(r);
    }
    public List<Route> findTopByRating() { return routeRepository.findTopByRating(); }
}
