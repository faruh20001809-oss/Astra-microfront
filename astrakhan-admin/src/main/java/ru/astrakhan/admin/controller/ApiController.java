package ru.astrakhan.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.astrakhan.admin.entity.*;
import ru.astrakhan.admin.repository.ReviewRepository;
import ru.astrakhan.admin.dto.CreateOrderRequest;
import ru.astrakhan.admin.dto.OrderItemRequest;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import ru.astrakhan.admin.service.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController @RequestMapping("/api/v1") @RequiredArgsConstructor
@Slf4j
public class ApiController {
    private final PoiService poiService;
    private final RouteService routeService;
    private final ProductService productService;
    private final OrderService orderService;
    private final FeedbackService feedbackService;
    private final AnalyticsService analyticsService;
    private final ReviewRepository reviewRepository;
    private final ObjectMapper objectMapper;

    // ===== POIs =====
    @GetMapping("/pois")
    public ResponseEntity<Map<String, Object>> getPois(@RequestParam(required = false) String category,
            @RequestParam(required = false) String search, @RequestParam(required = false) String status) {
        List<PointOfInterest> pois = ("PUBLISHED".equals(status) || status == null) ? poiService.findPublished() :
            poiService.findByStatus(PointOfInterest.PoiStatus.valueOf(status));
        if (category != null && !category.isEmpty()) pois = pois.stream().filter(p -> category.equals(p.getCategory())).collect(Collectors.toList());
        if (search != null && !search.isEmpty()) { String q = search.toLowerCase(); pois = pois.stream().filter(p -> p.getName().toLowerCase().contains(q)).collect(Collectors.toList()); }
        return ok(pois.stream().map(this::poiMap).collect(Collectors.toList()));
    }

    @GetMapping("/pois/{id}")
    public ResponseEntity<Map<String, Object>> getPoi(@PathVariable Long id) {
        return poiService.findById(id).map(poi -> {
            analyticsService.trackEvent("poi_view", poi.getId(), poi.getName());
            poiService.incrementViews(id);
            return okSingle(poiDetailMap(poi));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/pois/{id}/reviews")
    public ResponseEntity<Map<String, Object>> getPoiReviews(@PathVariable Long id) {
        return ok(reviewRepository.findByPoiIdAndApprovedTrue(id).stream().map(this::reviewMap).collect(Collectors.toList()));
    }

    @PostMapping("/pois/{id}/reviews")
    public ResponseEntity<Map<String, Object>> addReview(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Review review = Review.builder().poiId(id).userId((String)body.get("userId"))
            .userName((String)body.getOrDefault("userName","Аноним")).rating((Integer)body.get("rating"))
            .text((String)body.get("text")).visitDate((String)body.get("visitDate")).approved(false).build();
        return okSingle(reviewMap(reviewRepository.save(review)));
    }

    // ===== Routes =====
    @GetMapping("/routes")
    public ResponseEntity<Map<String, Object>> getRoutes(@RequestParam(required = false) String published, @RequestParam(required = false) String category) {
        List<Route> routes = "true".equals(published) ? routeService.findPublished() : routeService.findAll();
        if (category != null && !category.isEmpty()) routes = routes.stream().filter(r -> category.equals(r.getCategory())).collect(Collectors.toList());
        return ok(routes.stream().map(this::routeMap).collect(Collectors.toList()));
    }

    @GetMapping("/routes/{id}")
    public ResponseEntity<Map<String, Object>> getRoute(@PathVariable Long id) {
        return routeService.findById(id).map(r -> { analyticsService.trackEvent("route_view", r.getId(), r.getName()); return okSingle(routeMap(r)); })
            .orElse(ResponseEntity.notFound().build());
    }

    // ===== Products =====
    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> getProducts(@RequestParam(required = false) String category) {
        List<Product> products = productService.findPublished();
        if (category != null && !category.isEmpty()) products = products.stream().filter(p -> category.equals(p.getCategory())).collect(Collectors.toList());
        return ok(products.stream().map(this::productMap).collect(Collectors.toList()));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<Map<String, Object>> getProduct(@PathVariable Long id) {
        return productService.findById(id).map(p -> okSingle(productMap(p))).orElse(ResponseEntity.notFound().build());
    }

    // ===== Orders =====
    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody CreateOrderRequest request) {
        try {
            // 🔹 1. Валидация
            if (request.getItems() == null || request.getItems().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("status", "error", "message", "Заказ должен содержать хотя бы один товар"));
            }

            // 🔹 2. Создаём заказ
            String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
            System.out.println("🔹🔹🔹 DEBUG ORDER ID GENERATED: " + orderId);
            System.out.println("🔹🔹🔹 UUID CHECK: " + UUID.randomUUID());
            log.info("🔹🔹🔹 CREATING ORDER WITH ID: {}", orderId);

            Order order = Order.builder()
                    .orderId(orderId)
                    .userId(request.getUserId())
                    .status(Order.OrderStatus.PROCESSING)
                    .shippingMethod(request.getShippingMethod())
                    .paymentMethod(request.getPaymentMethod())
                    .currency("RUB")
                    .build();

            // 🔹 3. Обрабатываем адрес доставки
            if (request.getShippingAddress() != null) {
                // Сохраняем весь адрес как JSON
                String addressJson = objectMapper.writeValueAsString(request.getShippingAddress());
                order.setShippingAddress(addressJson);

                // Извлекаем имя и телефон для удобного отображения
                String firstName = (String) request.getShippingAddress().getOrDefault("firstName", "");
                String lastName = (String) request.getShippingAddress().getOrDefault("lastName", "");
                String phone = (String) request.getShippingAddress().get("phone");

                order.setCustomerName((firstName + " " + lastName).trim());
                order.setPhone(phone);
            }

            // 🔹 4. Сериализуем товары в JSON
            String itemsJson = objectMapper.writeValueAsString(request.getItems());
            order.setItemsJson(itemsJson);

            // 🔹 5. Рассчитываем общую сумму
            double total = request.getItems().stream()
                    .mapToDouble(item -> {
                        double price = item.getPrice() != null ? item.getPrice() : 0.0;
                        int qty = item.getQty() != null ? item.getQty() : 1;
                        return price * qty;
                    })
                    .sum();
            order.setTotal(total);

            // 🔹 6. Сохраняем заказ
            Order savedOrder = orderService.create(order);

            // 🔹 7. Трекаем событие аналитики
            analyticsService.trackEvent("order_created", savedOrder.getId(), savedOrder.getOrderId());

            // 🔹 8. Формируем ответ для фронта
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("data", Map.of(
                    "orderId", savedOrder.getOrderId(),
                    "total", savedOrder.getTotal(),
                    "status", savedOrder.getStatus().name().toLowerCase(),
                    "createdDate", savedOrder.getCreatedAt() != null
                            ? savedOrder.getCreatedAt().toString()
                            : null
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating order", e);
            return ResponseEntity.status(500)
                    .body(Map.of("status", "error", "message", "Ошибка сервера: " + e.getMessage()));
        }
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable String orderId) {
        return orderService.findByOrderId(orderId).map(o -> okSingle(Map.of("orderId", o.getOrderId(), "status", o.getStatus().name().toLowerCase())))
            .orElse(ResponseEntity.notFound().build());
    }

    // ===== Feedback =====
    @PostMapping("/feedback")
    public ResponseEntity<Map<String, Object>> submitFeedback(@RequestBody Map<String, Object> body) {
        Feedback fb = Feedback.builder().name((String)body.get("name")).email((String)body.get("email"))
            .phone((String)body.get("phone")).subject((String)body.get("subject")).message((String)body.get("message")).build();
        Feedback saved = feedbackService.save(fb);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("status", "success"); res.put("message", "Сообщение отправлено. Спасибо!");
        res.put("data", Map.of("id", saved.getId(), "createdDate", saved.getCreatedAt().toString()));
        return ResponseEntity.ok(res);
    }

    // ===== Analytics =====
    @PostMapping("/analytics/events")
    public ResponseEntity<Map<String, Object>> trackEvent(@RequestBody Map<String, Object> body) {
        String eventType = (String) body.get("event");
        @SuppressWarnings("unchecked") Map<String, Object> data = (Map<String, Object>) body.get("data");
        Long entityId = data != null && data.get("poiId") != null ? Long.valueOf(data.get("poiId").toString()) : null;
        String entityName = data != null ? (String) data.get("poiName") : null;
        analyticsService.trackEvent(eventType, entityId, entityName);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    // ===== Helpers =====
    private ResponseEntity<Map<String, Object>> ok(Object data) {
        Map<String, Object> r = new LinkedHashMap<>(); r.put("status", "success"); r.put("data", data); return ResponseEntity.ok(r);
    }
    private ResponseEntity<Map<String, Object>> okSingle(Object data) { return ok(data); }

    private Map<String, Object> poiMap(PointOfInterest p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId()); m.put("name", p.getName()); m.put("description", p.getDescription());
        m.put("category", p.getCategory()); m.put("latitude", p.getLatitude()); m.put("longitude", p.getLongitude());
        m.put("address", p.getAddress()); m.put("image", p.getImageUrl());
        m.put("rating", p.getRating()); m.put("reviewsCount", p.getReviewsCount());
        m.put("phone", p.getPhone()); m.put("website", p.getWebsite());
        m.put("tags", p.getTags() != null ? Arrays.asList(p.getTags().split(",")) : List.of());
        return m;
    }
    private Map<String, Object> poiDetailMap(PointOfInterest p) {
        Map<String, Object> m = poiMap(p);
        m.put("email", p.getEmail());
        m.put("extendedInfo", Map.of("foundedYear", p.getFoundedYear() != null ? p.getFoundedYear() : 0,
            "architect", p.getArchitect() != null ? p.getArchitect() : "", "material", p.getMaterial() != null ? p.getMaterial() : "",
            "style", p.getStyle() != null ? p.getStyle() : ""));
        m.put("coordinates", Map.of("latitude", p.getLatitude() != null ? p.getLatitude() : 0, "longitude", p.getLongitude() != null ? p.getLongitude() : 0));
        return m;
    }
    private Map<String, Object> routeMap(Route r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId()); m.put("name", r.getName()); m.put("description", r.getDescription());
        m.put("category", r.getCategory()); m.put("distance", r.getDistance()); m.put("duration", r.getDuration());
        m.put("difficulty", r.getDifficulty()); m.put("rating", r.getRating()); m.put("published", r.getPublished());
        m.put("paid", r.getPaid()); m.put("price", r.getPrice());
        if (r.getPoiIds() != null && !r.getPoiIds().isEmpty())
            m.put("pois", Arrays.stream(r.getPoiIds().split(",")).map(s -> Long.parseLong(s.trim())).collect(Collectors.toList()));
        else m.put("pois", List.of());
        return m;
    }
    private Map<String, Object> productMap(Product p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId()); m.put("name", p.getName()); m.put("description", p.getDescription());
        m.put("category", p.getCategory()); m.put("price", p.getPrice()); m.put("currency", p.getCurrency());
        m.put("image", p.getImageUrl()); m.put("rating", p.getRating()); m.put("inStock", p.getInStock());
        m.put("quantity", p.getQuantity()); m.put("material", p.getMaterial());
        if (p.getSizes() != null) m.put("sizes", Arrays.asList(p.getSizes().split(",")));
        if (p.getColors() != null) m.put("colors", Arrays.asList(p.getColors().split(",")));
        return m;
    }
    private Map<String, Object> reviewMap(Review r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId()); m.put("userId", r.getUserId()); m.put("userName", r.getUserName());
        m.put("rating", r.getRating()); m.put("text", r.getText()); m.put("visitDate", r.getVisitDate());
        m.put("createdDate", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
        m.put("helpfulCount", r.getHelpfulCount());
        return m;
    }
}
