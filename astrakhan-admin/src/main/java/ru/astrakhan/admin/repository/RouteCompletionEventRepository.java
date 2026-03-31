package ru.astrakhan.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.astrakhan.admin.entity.RouteCompletionEvent;

public interface RouteCompletionEventRepository extends JpaRepository<RouteCompletionEvent, Long> {
    long countByEmailIgnoreCaseAndPaidTrue(String email);
    long countByEmailIgnoreCaseAndPaidFalse(String email);
    boolean existsByEmailIgnoreCaseAndRouteId(String email, Long routeId);
}
