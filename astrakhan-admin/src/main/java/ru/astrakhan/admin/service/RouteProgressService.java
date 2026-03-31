package ru.astrakhan.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.astrakhan.admin.entity.RewardRedemptionEvent;
import ru.astrakhan.admin.entity.Route;
import ru.astrakhan.admin.entity.RouteCompletionEvent;
import ru.astrakhan.admin.repository.RewardRedemptionEventRepository;
import ru.astrakhan.admin.repository.RouteCompletionEventRepository;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RouteProgressService {
    private final RouteService routeService;
    private final RouteCompletionEventRepository completionRepository;
    private final RewardRedemptionEventRepository redemptionRepository;

    public Map<String, Object> markCompleted(String email, Long routeId) {
        String e = normalizeEmail(email);
        Route route = routeService.findById(routeId).orElseThrow(() -> new IllegalArgumentException("Маршрут не найден."));
        if (!completionRepository.existsByEmailIgnoreCaseAndRouteId(e, routeId)) {
            completionRepository.save(RouteCompletionEvent.builder()
                    .email(e)
                    .routeId(routeId)
                    .paid(Boolean.TRUE.equals(route.getPaid()))
                    .build());
        }
        return getConfirmedStats(e);
    }

    public Map<String, Object> getConfirmedStats(String email) {
        String e = normalizeEmail(email);
        long paid = completionRepository.countByEmailIgnoreCaseAndPaidTrue(e);
        long free = completionRepository.countByEmailIgnoreCaseAndPaidFalse(e);
        long total = paid + free;
        long earnedRewards = Math.min(paid / 2, free / 3);
        long usedRewards = redemptionRepository.countByEmailIgnoreCase(e);
        long availableRewards = Math.max(earnedRewards - usedRewards, 0);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("email", e);
        m.put("completedPaidRoutes", paid);
        m.put("completedFreeRoutes", free);
        m.put("completedRoutes", total);
        m.put("earnedRewards", earnedRewards);
        m.put("usedRewards", usedRewards);
        m.put("availableRewards", availableRewards);
        return m;
    }

    public Map<String, Object> redeemReward(String email, Long routeId) {
        String e = normalizeEmail(email);
        Route route = routeService.findById(routeId).orElseThrow(() -> new IllegalArgumentException("Маршрут не найден."));
        if (!Boolean.TRUE.equals(route.getPaid())) {
            throw new IllegalArgumentException("Награду можно применить только к платному маршруту.");
        }
        Map<String, Object> stats = getConfirmedStats(e);
        long available = ((Number) stats.getOrDefault("availableRewards", 0)).longValue();
        if (available < 1) {
            throw new IllegalArgumentException("Недостаточно доступных наград.");
        }
        redemptionRepository.save(RewardRedemptionEvent.builder()
                .email(e)
                .routeId(routeId)
                .build());
        return getConfirmedStats(e);
    }

    private static String normalizeEmail(String email) {
        String e = email != null ? email.trim().toLowerCase() : "";
        if (e.isBlank() || !e.contains("@")) {
            throw new IllegalArgumentException("Некорректный email.");
        }
        return e;
    }
}
