package ru.astrakhan.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ru.astrakhan.admin.entity.PointOfInterest;
import ru.astrakhan.admin.service.*;

@Controller
@RequiredArgsConstructor
public class DashboardController {
    private final PoiService poiService;
    private final RouteService routeService;
    private final ProductService productService;
    private final OrderService orderService;
    private final FeedbackService feedbackService;
    private final AnalyticsService analyticsService;

    @GetMapping("/")
    public String root() {
        return "redirect:/admin";
    }

    @GetMapping("/admin")
    public String dashboard(Model model, HttpServletRequest request) {
        // 🔹 Явно добавляем request в модель для использования в шаблонах
        model.addAttribute("request", request);

        model.addAttribute("totalPois", poiService.findAll().size());
        model.addAttribute("publishedPois", poiService.countByStatus(PointOfInterest.PoiStatus.PUBLISHED));
        model.addAttribute("totalRoutes", routeService.findAll().size());
        model.addAttribute("totalProducts", productService.findAll().size());
        model.addAttribute("totalOrders", orderService.findAll().size());
        model.addAttribute("newFeedback", feedbackService.countNew());
        model.addAttribute("revenue", orderService.totalRevenue());
        model.addAttribute("topPois", analyticsService.topViewedPois());
        model.addAttribute("topRoutes", analyticsService.topViewedRoutes());

        return "dashboard";
    }
}