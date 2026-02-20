package ru.astrakhan.admin.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.astrakhan.admin.entity.Route;
import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {
    List<Route> findByPublishedTrue();
    List<Route> findByCategory(String category);
    @Query("SELECT r FROM Route r ORDER BY r.rating DESC")
    List<Route> findTopByRating();
}
