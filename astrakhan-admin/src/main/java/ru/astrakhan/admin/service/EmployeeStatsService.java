package ru.astrakhan.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.astrakhan.admin.dto.EmployeeAchievementDto;
import ru.astrakhan.admin.dto.EmployeeStatsDto;
import ru.astrakhan.admin.entity.PointOfInterest;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeStatsService {

    private final PoiService poiService;
    private final RouteService routeService;
    private final ProductService productService;
    private final OrderService orderService;
    private final FeedbackService feedbackService;
    private final PoiSuggestionService poiSuggestionService;

    public EmployeeStatsDto aggregate() {
        long publishedPois = poiService.countByStatus(PointOfInterest.PoiStatus.PUBLISHED);
        long publishedRoutes = routeService.findPublished().size();
        long products = productService.findAll().size();
        long orders = orderService.findAll().size();
        long feedbackCount = feedbackService.findAll().size();
        long poiSuggestionsCount = poiSuggestionService.findAll().size();
        return EmployeeStatsDto.builder()
                .publishedPois(publishedPois)
                .publishedRoutes(publishedRoutes)
                .products(products)
                .orders(orders)
                .feedbackCount(feedbackCount)
                .poiSuggestionsCount(poiSuggestionsCount)
                .build();
    }

    public List<EmployeeAchievementDto> achievements(EmployeeStatsDto s) {
        long community = s.getFeedbackCount() + s.getPoiSuggestionsCount();
        List<EmployeeAchievementDto> list = new ArrayList<>();
        list.add(achievement(
                "content_rich",
                "Богатый контент",
                "Опубликовано не менее 50 точек интереса",
                s.getPublishedPois(),
                50));
        list.add(achievement(
                "routes_portfolio",
                "Портфель маршрутов",
                "Опубликовано не менее 10 маршрутов",
                s.getPublishedRoutes(),
                10));
        list.add(achievement(
                "shop_ready",
                "Магазин готов",
                "Не менее 20 товаров в каталоге",
                s.getProducts(),
                20));
        list.add(achievement(
                "community_active",
                "Активное сообщество",
                "Не менее 30 обращений и предложений точек (всего)",
                community,
                30));
        return list;
    }

    private static EmployeeAchievementDto achievement(String id, String title, String description, long current, long threshold) {
        int progress = threshold <= 0 ? 100 : (int) Math.min(100, (current * 100) / threshold);
        String status;
        if (current >= threshold) {
            status = "UNLOCKED";
        } else if (current > 0) {
            status = "IN_PROGRESS";
        } else {
            status = "LOCKED";
        }
        return EmployeeAchievementDto.builder()
                .id(id)
                .title(title)
                .description(description)
                .status(status)
                .progress(progress)
                .build();
    }
}
