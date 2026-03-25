package ru.astrakhan.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.astrakhan.admin.service.*;

/**
 * Контроллер панели аналитики администратора
 */
@Controller
@RequestMapping("/admin/analytics")
@RequiredArgsConstructor
public class AnalyticsAdminController {

    private final AnalyticsService analyticsService;
    private final PoiService poiService;
    private final RouteService routeService;
    private final OrderService orderService;

    @GetMapping
    public String analytics(Model model, HttpServletRequest request) {
        // 🔹 Идентификатор активного пункта меню для sidebar
        model.addAttribute("activeMenu", "analytics");

        // 🔹 Явно добавляем request в модель, чтобы использовать в шаблонах
        model.addAttribute("request", request); // 👈 Главная фикс-строка

        // 🔹 Статистика просмотров
        model.addAttribute("topPois", analyticsService.topViewedPois());
        model.addAttribute("topRoutes", analyticsService.topViewedRoutes());
        model.addAttribute("leastPois", analyticsService.leastViewedPois(10));
        model.addAttribute("leastRoutes", analyticsService.leastViewedRoutes(10));
        model.addAttribute("eventCounts", analyticsService.countByEventType());

        // 🔹 Счётчики событий
        model.addAttribute("poiViews", analyticsService.countEvents("poi_view"));
        model.addAttribute("routeViews", analyticsService.countEvents("route_view"));
        model.addAttribute("orderCount", analyticsService.countEvents("order_created"));

        // 🔹 Рейтинги
        model.addAttribute("topRatedPois", poiService.findTopByRating());
        model.addAttribute("topRatedRoutes", routeService.findTopByRating());

        // 🔹 Финансы и категории
        model.addAttribute("revenue", orderService.totalRevenue());
        model.addAttribute("categoryStats", poiService.countByCategory());

        return "analytics/dashboard";
    }
}