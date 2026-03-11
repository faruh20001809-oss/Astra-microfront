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
    private final OrderEmailService orderEmailService;
    private final PoiSuggestionService poiSuggestionService;

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

    /** Картинка маршрута (из imageData). ETag по updatedAt — при обновлении файла на бэкенде кэш инвалидируется. */
    @GetMapping(value = "/routes/{id}/image", produces = "image/*")
    public ResponseEntity<byte[]> getRouteImage(@PathVariable Long id) {
        return routeService.findById(id)
                .filter(r -> r.getImageData() != null && r.getImageData().length > 0)
                .map(r -> {
                    String contentType = "image/jpeg";
                    if (r.getImageFilename() != null) {
                        String fn = r.getImageFilename().toLowerCase();
                        if (fn.endsWith(".png")) contentType = "image/png";
                        else if (fn.endsWith(".gif")) contentType = "image/gif";
                        else if (fn.endsWith(".webp")) contentType = "image/webp";
                    }
                    String etag = r.getUpdatedAt() != null ? "\"r" + id + "-" + r.getUpdatedAt().toEpochSecond(java.time.ZoneOffset.UTC) + "\"" : "\"r" + id + "\"";
                    return ResponseEntity.ok()
                            .header("Cache-Control", "private, max-age=3600, must-revalidate")
                            .header("ETag", etag)
                            .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                            .body(r.getImageData());
                })
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

    /** Картинка товара (из imageData). ETag — при изменении на бэкенде кэш инвалидируется. */
    @GetMapping(value = "/products/{id}/image", produces = "image/*")
    public ResponseEntity<byte[]> getProductImage(@PathVariable Long id) {
        return productService.findById(id)
                .filter(p -> p.getImageData() != null && p.getImageData().length > 0)
                .map(p -> {
                    String contentType = "image/jpeg";
                    if (p.getImageFilename() != null) {
                        String fn = p.getImageFilename().toLowerCase();
                        if (fn.endsWith(".png")) contentType = "image/png";
                        else if (fn.endsWith(".gif")) contentType = "image/gif";
                        else if (fn.endsWith(".webp")) contentType = "image/webp";
                    }
                    String etag = p.getUpdatedAt() != null ? "\"p" + id + "-" + p.getUpdatedAt().toEpochSecond(java.time.ZoneOffset.UTC) + "\"" : "\"p" + id + "\"";
                    return ResponseEntity.ok()
                            .header("Cache-Control", "private, max-age=3600, must-revalidate")
                            .header("ETag", etag)
                            .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                            .body(p.getImageData());
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ===== Orders =====
    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody CreateOrderRequest request) {
        try {
            // 🔹 1. Валидация
            Map<String, String> errors = validateOrderRequest(request);
            if (!errors.isEmpty()) {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("status", "error");
                body.put("message", "Ошибка валидации");
                body.put("errors", errors);
                return ResponseEntity.badRequest().body(body);
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
                String email = (String) request.getShippingAddress().get("email");

                order.setCustomerName((firstName + " " + lastName).trim());
                order.setPhone(phone);
                order.setEmail(email);
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

            // 🔹 6. Сохраняем заказ (до любой пост-обработки, чтобы заказ всегда создавался)
            Order savedOrder = orderService.create(order);

            // 🔹 9. Формируем ответ для фронта сразу (без null в Map — Map.of не допускает null)
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("orderId", savedOrder.getOrderId());
            data.put("total", savedOrder.getTotal() != null ? savedOrder.getTotal() : 0.0);
            data.put("status", savedOrder.getStatus() != null ? savedOrder.getStatus().name().toLowerCase() : "processing");
            data.put("createdDate", savedOrder.getCreatedAt() != null ? savedOrder.getCreatedAt().toString() : "");
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("data", data);

            // 🔹 7–8. Пост-обработка не должна ломать ответ — заказ уже создан
            try {
                orderEmailService.sendOrderAcceptedForProcessing(savedOrder);
                analyticsService.trackEvent("order_created", savedOrder.getId(), savedOrder.getOrderId());
            } catch (Exception ex) {
                log.warn("Post-order processing failed (order {} created): {}", savedOrder.getOrderId(), ex.getMessage());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating order", e);
            Map<String, Object> errBody = new LinkedHashMap<>();
            errBody.put("status", "error");
            errBody.put("message", "Сервер не смог обработать заказ. Попробуйте позже или оформите заказ по телефону.");
            return ResponseEntity.status(500).body(errBody);
        }
    }

    private static final java.util.regex.Pattern EMAIL_PATTERN = java.util.regex.Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private Map<String, String> validateOrderRequest(CreateOrderRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (request.getItems() == null || request.getItems().isEmpty()) {
            errors.put("items", "Заказ должен содержать хотя бы один товар");
        } else {
            for (int i = 0; i < request.getItems().size(); i++) {
                OrderItemRequest item = request.getItems().get(i);
                if (item.getName() == null || item.getName().isBlank()) {
                    errors.put("items[" + i + "].name", "Укажите название товара");
                }
                if (item.getPrice() == null || item.getPrice() < 0) {
                    errors.put("items[" + i + "].price", "Укажите корректную цену");
                }
                if (item.getQty() != null && item.getQty() < 1) {
                    errors.put("items[" + i + "].qty", "Количество должно быть не менее 1");
                }
            }
        }
        if (request.getShippingAddress() == null || request.getShippingAddress().isEmpty()) {
            errors.put("shippingAddress", "Укажите адрес доставки");
        } else {
            Map<String, Object> addr = request.getShippingAddress();
            String firstName = (String) addr.get("firstName");
            String lastName = (String) addr.get("lastName");
            String phone = (String) addr.get("phone");
            String city = (String) addr.get("city");
            String address = (String) addr.get("address");
            String customerName = ((firstName != null ? firstName : "").trim() + " " + (lastName != null ? lastName : "").trim()).trim();
            if (customerName.isBlank()) {
                errors.put("shippingAddress.firstName", "Укажите имя");
            }
            if (phone == null || phone.isBlank()) {
                errors.put("shippingAddress.phone", "Укажите телефон");
            }
            if ((address == null || address.isBlank()) && (city == null || city.isBlank())) {
                errors.put("shippingAddress.address", "Укажите адрес доставки");
            }
            String email = (String) addr.get("email");
            if (email != null && !email.isBlank() && !EMAIL_PATTERN.matcher(email).matches()) {
                errors.put("shippingAddress.email", "Некорректный формат email");
            }
        }
        if (request.getShippingMethod() == null || request.getShippingMethod().isBlank()) {
            errors.put("shippingMethod", "Укажите способ доставки");
        }
        return errors;
    }

    /** Список заказов (GET для просмотра в браузере или API). POST — только для создания. */
    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> listOrders(@RequestParam(required = false) String status) {
        List<Order> orders = (status != null && !status.isEmpty())
                ? orderService.findByStatus(Order.OrderStatus.valueOf(status))
                : orderService.findAll();
        return ok(orders.stream().map(this::orderMap).collect(Collectors.toList()));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable String orderId) {
        return orderService.findByOrderId(orderId).map(o -> okSingle(Map.of("orderId", o.getOrderId(), "status", o.getStatus().name().toLowerCase())))
            .orElse(ResponseEntity.notFound().build());
    }

    // ===== POI Suggestions (предложения точек) =====
    @PostMapping("/poi-suggestions")
    public ResponseEntity<Map<String, Object>> submitPoiSuggestion(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String place = (String) body.get("place");
        String description = (String) body.get("description");
        String whyAdd = (String) body.get("whyAdd");
        if (name == null || name.isBlank() || place == null || place.isBlank() || whyAdd == null || whyAdd.isBlank()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("status", "error");
            err.put("message", "Заполните обязательные поля: название, место, почему добавить");
            return ResponseEntity.badRequest().body(err);
        }
        PoiSuggestion s = PoiSuggestion.builder()
                .name(name.trim())
                .place(place.trim())
                .description(description != null ? description.trim() : null)
                .whyAdd(whyAdd.trim())
                .build();
        PoiSuggestion saved = poiSuggestionService.save(s);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("status", "success");
        res.put("message", "Предложение отправлено. Спасибо!");
        res.put("data", Map.of("id", saved.getId(), "createdDate", saved.getCreatedAt() != null ? saved.getCreatedAt().toString() : null));
        return ResponseEntity.ok(res);
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
        m.put("id", r.getId());
        m.put("name", r.getName());
        m.put("title", r.getName());
        m.put("description", r.getDescription());
        m.put("category", r.getCategory());
        m.put("distance", formatDistance(r.getDistance()));
        m.put("duration", formatDuration(r.getDuration()));
        m.put("difficulty", r.getDifficulty());
        m.put("rating", r.getRating() != null ? r.getRating() : 0.0);
        m.put("published", r.getPublished());
        m.put("paid", r.getPaid() != null && r.getPaid());
        m.put("isPaid", r.getPaid() != null && r.getPaid());
        m.put("price", r.getPrice() != null ? r.getPrice() : 0.0);
        if (r.getPoiIds() != null && !r.getPoiIds().isEmpty()) {
            List<Long> poiIdList = Arrays.stream(r.getPoiIds().split(",")).map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
            m.put("pois", poiIdList);
            List<Map<String, Object>> stops = new ArrayList<>();
            for (Long poiId : poiIdList) {
                poiService.findById(poiId).ifPresent(poi -> stops.add(Map.of("name", poi.getName(), "description", poi.getDescription() != null ? poi.getDescription() : "")));
            }
            m.put("stops", stops);
        } else {
            m.put("pois", List.of());
            m.put("stops", List.of());
        }
        String coverImage = (r.getImageUrl() != null && !r.getImageUrl().isEmpty())
                ? r.getImageUrl()
                : (r.getImageData() != null && r.getImageData().length > 0 ? "/java-api/api/v1/routes/" + r.getId() + "/image" : null);
        m.put("image", coverImage);
        m.put("coverImage", coverImage);
        return m;
    }

    private static String formatDistance(Double km) {
        if (km == null) return "—";
        return km + " км";
    }

    private static String formatDuration(Integer minutes) {
        if (minutes == null) return "—";
        if (minutes < 60) return minutes + " мин";
        int h = minutes / 60;
        int m = minutes % 60;
        if (m == 0) return h + " ч";
        return h + " ч " + m + " мин";
    }
    private Map<String, Object> productMap(Product p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId()); m.put("name", p.getName()); m.put("description", p.getDescription());
        m.put("category", p.getCategory()); m.put("price", p.getPrice()); m.put("currency", p.getCurrency());
        m.put("image", (p.getImageUrl() != null && !p.getImageUrl().isEmpty()) ? p.getImageUrl() : "/java-api/api/v1/products/" + p.getId() + "/image"); m.put("rating", p.getRating()); m.put("inStock", p.getInStock());
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
    private Map<String, Object> orderMap(Order o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", o.getId());
        m.put("orderId", o.getOrderId());
        m.put("customerName", o.getCustomerName());
        m.put("phone", o.getPhone());
        m.put("email", o.getEmail());
        m.put("total", o.getTotal() != null ? o.getTotal() : 0);
        m.put("currency", o.getCurrency() != null ? o.getCurrency() : "RUB");
        m.put("status", o.getStatus() != null ? o.getStatus().name().toLowerCase() : "processing");
        m.put("shippingMethod", o.getShippingMethod());
        m.put("paymentMethod", o.getPaymentMethod());
        m.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : "");
        m.put("paidAt", o.getPaidAt() != null ? o.getPaidAt().toString() : null);
        return m;
    }
}
