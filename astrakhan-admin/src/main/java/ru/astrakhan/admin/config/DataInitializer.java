package ru.astrakhan.admin.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.astrakhan.admin.entity.*;
import ru.astrakhan.admin.repository.*;

@Component @RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final PoiRepository poiRepo;
    private final RouteRepository routeRepo;
    private final ProductRepository prodRepo;
    private final FeedbackRepository fbRepo;
    private final OrderRepository orderRepo;
    private final AnalyticsEventRepository analyticsRepo;
    private final ReviewRepository reviewRepo;
    private final AdminUserRepository adminUserRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (adminUserRepo.count() == 0) {
            adminUserRepo.save(AdminUser.builder()
                    .username("admin")
                    .passwordHash(passwordEncoder.encode("admin"))
                    .role("ADMIN")
                    .build());
        }

        orderRepo.setDeletedFalseWhereNull();

        if (poiRepo.count() > 0) return;

        PointOfInterest kreml = poiRepo.save(PointOfInterest.builder()
            .name("Астраханский Кремль").description("Крепость XVI века, памятник военного зодчества.")
            .category("История").latitude(46.3497).longitude(48.0332).address("ул. Тредиаковского, 2")
            .phone("+7 (8512) 51-57-00").website("https://astrakhan-musei.ru")
            .foundedYear(1558).architect("Михаил Вельяминов").material("Кирпич").style("Военная архитектура")
            .tags("крепость,XVI век,архитектура").status(PointOfInterest.PoiStatus.PUBLISHED)
            .rating(4.8).viewsCount(1520L).reviewsCount(156).build());

        PointOfInterest uspensky = poiRepo.save(PointOfInterest.builder()
            .name("Успенский собор").description("Кафедральный собор, построен в 1698-1710 годах.")
            .category("Архитектура").latitude(46.3504).longitude(48.0339).address("Кремль")
            .foundedYear(1710).architect("Дорофей Мякишев").material("Кирпич").style("Барокко")
            .tags("собор,барокко,XVIII век").status(PointOfInterest.PoiStatus.PUBLISHED)
            .rating(4.7).viewsCount(890L).reviewsCount(89).build());

        PointOfInterest museum = poiRepo.save(PointOfInterest.builder()
            .name("Краеведческий музей").description("Один из старейших музеев России, основан в 1837 году.")
            .category("Музеи").latitude(46.3520).longitude(48.0750).address("ул. Советская, 15")
            .phone("+7 (8512) 51-27-81").foundedYear(1837).tags("музей,краеведение,история")
            .status(PointOfInterest.PoiStatus.PUBLISHED).rating(4.5).viewsCount(650L).reviewsCount(45).build());

        poiRepo.save(PointOfInterest.builder().name("Лебединое озеро").description("Парковый водоём в центре города.")
            .category("Парки").latitude(46.3480).longitude(48.0400).address("ул. Адмиралтейская")
            .tags("парк,озеро,природа").status(PointOfInterest.PoiStatus.PUBLISHED)
            .rating(4.3).viewsCount(430L).reviewsCount(30).build());

        poiRepo.save(PointOfInterest.builder().name("Памятник Петру I").description("Монумент на набережной.")
            .category("Памятники").latitude(46.3555).longitude(48.0410).address("Набережная 1 Мая")
            .tags("памятник,Петр I").status(PointOfInterest.PoiStatus.DRAFT).rating(0.0).viewsCount(0L).build());

        routeRepo.save(Route.builder().name("История центра Астрахани").description("Путешествие по историческому центру.")
            .category("История").distance(3.5).duration(60).difficulty("easy").published(true).paid(false).rating(4.6)
            .poiIds(kreml.getId() + "," + uspensky.getId() + "," + museum.getId()).build());
        routeRepo.save(Route.builder().name("Архитектурный тур").description("Архитектурные памятники разных эпох.")
            .category("Архитектура").distance(4.2).duration(90).difficulty("medium").published(true).paid(true).price(299.0).rating(4.4)
            .poiIds(uspensky.getId().toString()).build());
        routeRepo.save(Route.builder().name("Вечерняя прогулка").description("Романтическая прогулка по набережной.")
            .category("Природа").distance(2.0).duration(45).difficulty("easy").published(false).paid(false).rating(0.0).build());

        prodRepo.save(Product.builder().name("Футболка «Кремль»").description("Хлопковая футболка с принтом Кремля")
            .category("Одежда").price(899.0).rating(4.7).reviewsCount(23).inStock(true).quantity(45)
            .sizes("XS,S,M,L,XL,XXL").colors("Черный,Белый").material("100% хлопок").published(true).build());
        prodRepo.save(Product.builder().name("Кружка «Волга»").description("Керамическая кружка с видами Астрахани")
            .category("Сувениры").price(450.0).rating(4.5).reviewsCount(15).inStock(true).quantity(120)
            .material("Керамика").published(true).build());
        prodRepo.save(Product.builder().name("Набор открыток").description("12 открыток с историческими фотографиями")
            .category("Открытки").price(350.0).rating(4.9).reviewsCount(8).inStock(true).quantity(200).published(true).build());

        orderRepo.save(Order.builder().orderId("ORD-2024-001001").userId("user1").customerName("Иван Иванов")
            .phone("+7 900 123-45-67").email("ivan@example.com").shippingAddress("Астрахань, ул. Советская, 25")
            .shippingMethod("delivery").paymentMethod("card")
            .itemsJson("[{\"productId\":1,\"name\":\"Футболка\",\"quantity\":2,\"price\":899}]")
            .total(1798.0).status(Order.OrderStatus.CONFIRMED).build());
        orderRepo.save(Order.builder().orderId("ORD-2024-001002").userId("user2").customerName("Мария Петрова")
            .phone("+7 900 987-65-43").email("maria@example.com").shippingAddress("Астрахань, ул. Ленина, 10")
            .shippingMethod("pickup").paymentMethod("cash")
            .itemsJson("[{\"productId\":2,\"name\":\"Кружка\",\"quantity\":1,\"price\":450}]")
            .total(450.0).status(Order.OrderStatus.SHIPPED).trackingNumber("TRK000001").build());

        fbRepo.save(Feedback.builder().name("Алексей").email("alex@example.com").subject("Предложение")
            .message("Было бы здорово добавить ночной режим карты!").status(Feedback.FeedbackStatus.NEW).build());
        fbRepo.save(Feedback.builder().name("Ольга").email("olga@example.com").subject("Благодарность")
            .message("Отличный проект! Спасибо за сохранение истории города.").status(Feedback.FeedbackStatus.NEW).build());
        fbRepo.save(Feedback.builder().name("Дмитрий").email("dmitry@example.com").phone("+7 927 111-22-33")
            .subject("Ошибка").message("Неправильные координаты у музея.").status(Feedback.FeedbackStatus.READ).build());

        reviewRepo.save(Review.builder().poiId(kreml.getId()).userId("user1").userName("Иван")
            .rating(5).text("Потрясающее место!").visitDate("2024-01-15").approved(true).build());
        reviewRepo.save(Review.builder().poiId(kreml.getId()).userId("user2").userName("Мария")
            .rating(4).text("Красиво, но мало информации.").visitDate("2024-02-10").approved(true).build());
        reviewRepo.save(Review.builder().poiId(museum.getId()).userId("user3").userName("Сергей")
            .rating(5).text("Очень интересная экспозиция!").visitDate("2024-03-05").approved(false).build());

        for (int i = 0; i < 50; i++) analyticsRepo.save(AnalyticsEvent.builder().eventType("poi_view").entityId(kreml.getId()).entityName("Астраханский Кремль").build());
        for (int i = 0; i < 30; i++) analyticsRepo.save(AnalyticsEvent.builder().eventType("poi_view").entityId(uspensky.getId()).entityName("Успенский собор").build());
        for (int i = 0; i < 20; i++) analyticsRepo.save(AnalyticsEvent.builder().eventType("poi_view").entityId(museum.getId()).entityName("Краеведческий музей").build());
        for (int i = 0; i < 15; i++) analyticsRepo.save(AnalyticsEvent.builder().eventType("route_view").entityId(1L).entityName("История центра Астрахани").build());
        for (int i = 0; i < 5; i++) analyticsRepo.save(AnalyticsEvent.builder().eventType("order_created").entityId((long)i).entityName("Order #" + i).build());
    }
}
