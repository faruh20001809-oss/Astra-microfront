package ru.astrakhan.admin.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.astrakhan.admin.entity.Order;
import ru.astrakhan.admin.entity.PoiSuggestion;
import ru.astrakhan.admin.entity.Route;
import ru.astrakhan.admin.repository.OrderRepository;
import ru.astrakhan.admin.repository.PoiSuggestionRepository;
import ru.astrakhan.admin.repository.RouteRepository;

/**
 * Дополнительные фикстуры для Playwright E2E (профиль {@code e2e}).
 */
@Component
@Profile("e2e")
@RequiredArgsConstructor
public class E2eDataInitializer implements CommandLineRunner {

    private final PoiSuggestionRepository poiSuggestionRepository;
    private final OrderRepository orderRepository;
    private final RouteRepository routeRepository;

    @Override
    @Transactional
    public void run(String... args) {
        ensureProcessingOrder();
        ensureSecondPaidRoute();
        ensureFreeRoutesForRewards();
        if (poiSuggestionRepository.count() > 0) {
            return;
        }
        poiSuggestionRepository.save(PoiSuggestion.builder()
                .name("Деревянный дом на ул. Ленина")
                .place("Астрахань, ул. Ленина, 12")
                .description("Резной наличник, типичный для купеческой застройки.")
                .whyAdd("Объект скоро сносится — нужно сохранить в каталоге.")
                .latitude(46.3471)
                .longitude(48.0392)
                .status(PoiSuggestion.SuggestionStatus.NEW)
                .build());
        poiSuggestionRepository.save(PoiSuggestion.builder()
                .name("Бывшая усадьба купца")
                .place("Астрахань, Кировская, 5")
                .description("Фрагмент ограды и флигель.")
                .whyAdd("Редкий пример деревянного декора.")
                .status(PoiSuggestion.SuggestionStatus.APPROVED)
                .build());
    }

    /** Заказ в статусе PROCESSING для скриншота смены статуса (E2E раздел 3, тест 4). */
    private void ensureProcessingOrder() {
        var existing = orderRepository.findByOrderId("ORD-E2E-PROCESSING");
        if (existing.isPresent()) {
            Order o = existing.get();
            if (o.getStatus() != Order.OrderStatus.PROCESSING) {
                o.setStatus(Order.OrderStatus.PROCESSING);
                orderRepository.save(o);
            }
            return;
        }
        orderRepository.save(Order.builder()
                .orderId("ORD-E2E-PROCESSING")
                .userId("e2e-user")
                .customerName("Анна Смирнова")
                .phone("+7 900 555-12-34")
                .email("anna.e2e@example.com")
                .shippingAddress("Астрахань, ул. Максима Горького, 15")
                .shippingMethod("delivery")
                .paymentMethod("card")
                .itemsJson("[{\"productId\":3,\"name\":\"Набор открыток\",\"quantity\":1,\"price\":350}]")
                .total(350.0)
                .status(Order.OrderStatus.PROCESSING)
                .build());
    }

    /** Второй платный маршрут — для E2E списания награды (2 paid → 1 earned). */
    private void ensureSecondPaidRoute() {
        if (routeRepository.findAll().stream().filter(r -> Boolean.TRUE.equals(r.getPaid())).count() >= 2) {
            return;
        }
        routeRepository.save(Route.builder()
                .name("E2E платный маршрут")
                .description("Дополнительный платный маршрут для проверки наград.")
                .category("История")
                .distance(2.5)
                .duration(50)
                .difficulty("easy")
                .published(true)
                .paid(true)
                .price(199.0)
                .rating(4.2)
                .build());
    }

    /** Три бесплатных маршрута — для E2E награды (min(paid/2, free/3)). */
    private void ensureFreeRoutesForRewards() {
        long freePublished = routeRepository.findAll().stream()
                .filter(r -> Boolean.TRUE.equals(r.getPublished()) && !Boolean.TRUE.equals(r.getPaid()))
                .count();
        if (freePublished >= 3) {
            return;
        }
        for (int i = 0; i < 3 - freePublished; i++) {
            routeRepository.save(Route.builder()
                    .name("E2E бесплатный маршрут " + (i + 1))
                    .description("Бесплатный маршрут для проверки начисления наград.")
                    .category("Природа")
                    .distance(1.5 + i)
                    .duration(30 + i * 10)
                    .difficulty("easy")
                    .published(true)
                    .paid(false)
                    .rating(4.0)
                    .build());
        }
    }
}
