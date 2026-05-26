package ru.astrakhan.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.astrakhan.admin.entity.RewardRedemptionEvent;

public interface RewardRedemptionEventRepository extends JpaRepository<RewardRedemptionEvent, Long> {
    long countByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndRouteId(String email, Long routeId);
}
