package ru.astrakhan.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import ru.astrakhan.admin.entity.*;
import ru.astrakhan.admin.repository.ReviewRepository;
import ru.astrakhan.admin.dto.CreateOrderRequest;
import ru.astrakhan.admin.dto.CreatePreorderRequest;
import ru.astrakhan.admin.dto.OrderItemRequest;
import java.util.UUID;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import ru.astrakhan.admin.service.*;
import ru.astrakhan.admin.util.RouteStopsHelper;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "Public API v1", description = "Публичный REST API для module2 (маршруты, POI, заказы, прогресс)")
@RestController @RequestMapping("/api/v1") @RequiredArgsConstructor
@Slf4j
public class ApiController {
    @Value("${app.telegram.internal-token:}")
    private String telegramInternalToken;

    private final PoiService poiService;
    private final NewsletterService newsletterService;
    private final RouteService routeService;
    private final ProductService productService;
    private final OrderService orderService;
    private final FeedbackService feedbackService;
    private final AnalyticsService analyticsService;
    private final ReviewRepository reviewRepository;
    private final ObjectMapper objectMapper;
    private final OrderEmailService orderEmailService;
    private final PoiSuggestionService poiSuggestionService;
    private final GuestOrderLookupService guestOrderLookupService;
    private final TelegramLinkService telegramLinkService;
    private final UserProgressService userProgressService;
    private final RouteProgressService routeProgressService;
    private final SharedUserSyncService sharedUserSyncService;
    private final PickupPointsService pickupPointsService;
    private final PreorderService preorderService;

    /** Тот же справочник пунктов самовывоза, что в админке (модалка заказа). */
    @GetMapping("/pickup-points")
    public ResponseEntity<Map<String, Object>> getPickupPoints() {
        return ok(pickupPointsService.listAll());
    }

    // ===== POIs =====
    @GetMapping("/pois")
    public ResponseEntity<Map<String, Object>> getPois(@RequestParam(required = false) String category,
            @RequestParam(required = false) String search, @RequestParam(required = false) String status) {
        List<PointOfInterest> pois;
        if ("PUBLISHED".equals(status) || status == null) {
            pois = poiService.findPubliclyVisibleNow();
        } else {
            pois = poiService.findByStatus(PointOfInterest.PoiStatus.valueOf(status));
        }
        if (category != null && !category.isEmpty()) pois = pois.stream().filter(p -> category.equals(p.getCategory())).collect(Collectors.toList());
        if (search != null && !search.isEmpty()) { String q = search.toLowerCase(); pois = pois.stream().filter(p -> p.getName().toLowerCase().contains(q)).collect(Collectors.toList()); }
        return ok(pois.stream().map(this::poiMap).collect(Collectors.toList()));
    }

    /** Версия каталога для клиента: max(id) среди сейчас видимых гостю ТОИ. */
    @GetMapping("/pois/catalog-revision")
    public ResponseEntity<Map<String, Object>> getPoiCatalogRevision() {
        return okSingle(Map.of("revision", poiService.catalogRevision()));
    }

    /** Новые видимые ТОИ с id строго больше afterId (для модалки на витрине). */
    @GetMapping("/pois/since")
    public ResponseEntity<Map<String, Object>> getPoisSince(
            @RequestParam("afterId") long afterId,
            @RequestParam(defaultValue = "10") int limit) {
        List<PointOfInterest> list = poiService.findPublicVisibleAfterId(afterId, limit);
        return ok(list.stream().map(this::poiSinceMap).collect(Collectors.toList()));
    }

    @GetMapping("/pois/{id}")
    public ResponseEntity<Map<String, Object>> getPoi(@PathVariable Long id) {
        Optional<PointOfInterest> opt = poiService.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        PointOfInterest poi = opt.get();
        LocalDateTime now = LocalDateTime.now();
        if (!PoiService.isPubliclyVisible(poi, now)) {
            return ResponseEntity.notFound().build();
        }
        analyticsService.trackEvent("poi_view", poi.getId(), poi.getName());
        poiService.incrementViews(id);
        return okSingle(poiDetailMap(poi));
    }

    /** Обложка ТОИ из загруженного файла (imageData), как у товаров и маршрутов. */
    @GetMapping(value = "/pois/{id}/image", produces = "image/*")
    public ResponseEntity<byte[]> getPoiImage(@PathVariable Long id) {
        return poiService.findById(id)
                .filter(p -> p.getImageData() != null && p.getImageData().length > 0)
                .map(p -> {
                    String contentType = "image/jpeg";
                    if (p.getImageFilename() != null) {
                        String fn = p.getImageFilename().toLowerCase();
                        if (fn.endsWith(".png")) contentType = "image/png";
                        else if (fn.endsWith(".gif")) contentType = "image/gif";
                        else if (fn.endsWith(".webp")) contentType = "image/webp";
                    }
                    String etag = p.getUpdatedAt() != null
                            ? "\"poi" + id + "-" + p.getUpdatedAt().toEpochSecond(java.time.ZoneOffset.UTC) + "\""
                            : "\"poi" + id + "\"";
                    return ResponseEntity.ok()
                            .header("Cache-Control", "private, max-age=3600, must-revalidate")
                            .header("ETag", etag)
                            .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                            .body(p.getImageData());
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> poiSinceMap(PointOfInterest p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("category", p.getCategory());
        String desc = p.getDescription();
        if (desc != null && desc.length() > 280) {
            desc = desc.substring(0, 277) + "…";
        }
        m.put("description", desc);
        m.put("latitude", p.getLatitude());
        m.put("longitude", p.getLongitude());
        return m;
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
        try {
            List<Route> routes = "true".equals(published) ? routeService.findPublished() : routeService.findAll();
            if (category != null && !category.isEmpty()) {
                routes = routes.stream().filter(r -> category.equals(r.getCategory())).collect(Collectors.toList());
            }
            if ("true".equals(published)) {
                routes = routes.stream()
                        .filter(r -> r.getStatus() == null || r.getStatus() != Route.RouteStatus.OUTDATED)
                        .collect(Collectors.toList());
            }
            routes = routes.stream()
                    .sorted((a, b) -> {
                        int pa = a.getPriority() != null ? a.getPriority() : 0;
                        int pb = b.getPriority() != null ? b.getPriority() : 0;
                        int cmp = Integer.compare(pb, pa);
                        if (cmp != 0) return cmp;
                        LocalDateTime ua = a.getUpdatedAt();
                        LocalDateTime ub = b.getUpdatedAt();
                        if (ua == null && ub == null) return 0;
                        if (ua == null) return 1;
                        if (ub == null) return -1;
                        return ub.compareTo(ua);
                    })
                    .collect(Collectors.toList());
            List<Map<String, Object>> items = new ArrayList<>();
            for (Route r : routes) {
                try {
                    items.add(routeMapListItem(r));
                } catch (Exception one) {
                    log.warn("Skip route id={} in public list: {}", r.getId(), one.getMessage());
                }
            }
            return ok(items);
        } catch (Exception e) {
            log.error("getRoutes failed", e);
            return apiError(HttpStatus.INTERNAL_SERVER_ERROR, "Не удалось загрузить маршруты: " + e.getMessage());
        }
    }

    @GetMapping("/routes/{id}")
    public ResponseEntity<Map<String, Object>> getRoute(@PathVariable Long id) {
        return routeService.findById(id).map(r -> { analyticsService.trackEvent("route_view", r.getId(), r.getName()); return okSingle(routeMapDetail(r)); })
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

    /**
     * Ручная смена статуса маршрута оператором (например, перевод из OUTDATED в ACTIVE после починки точек).
     * Тело: { "status": "ACTIVE" | "DRAFT" | "OUTDATED", "outdatedReason": "..." }
     */
    @PatchMapping("/routes/{id}/status")
    public ResponseEntity<Map<String, Object>> updateRouteStatus(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> b = body != null ? body : Collections.emptyMap();
        String statusStr = b.get("status") instanceof String ? (String) b.get("status") : "";
        String reason = b.get("outdatedReason") instanceof String ? (String) b.get("outdatedReason") : null;
        return routeService.findById(id).map(r -> {
            try {
                Route.RouteStatus newStatus = Route.RouteStatus.valueOf(statusStr);
                r.setStatus(newStatus);
                if (newStatus == Route.RouteStatus.OUTDATED) {
                    r.setOutdatedReason(reason != null ? reason : "Установлено вручную оператором");
                } else {
                    r.setOutdatedReason(null);
                }
                routeService.save(r);
                log.info("Route {} status changed to {} (reason: {})", id, newStatus, reason);
                return okSingle(routeMapDetail(r));
            } catch (IllegalArgumentException e) {
                return apiError(HttpStatus.BAD_REQUEST,
                        "Недопустимый статус: " + statusStr + ". Допустимы: DRAFT, ACTIVE, OUTDATED.");
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/routes/{id}/complete")
    public ResponseEntity<Map<String, Object>> completeRoute(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> b = body != null ? body : Collections.emptyMap();
        String email = b.get("email") instanceof String ? (String) b.get("email") : "";
        try {
            analyticsService.trackEvent("route_completed", id, "route_" + id);
            return okSingle(routeProgressService.markCompleted(email, id));
        } catch (IllegalArgumentException e) {
            return apiError(HttpStatus.BAD_REQUEST, e.getMessage());
        }
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

            // 🔹 3. Обрабатываем адрес доставки (для самовывоза — pickupPointId/pickupAddress в JSON)
            if (request.getShippingAddress() != null) {
                java.util.Map<String, Object> addrMap = new java.util.LinkedHashMap<>(request.getShippingAddress());
                if (request.getPickupPointId() != null && !request.getPickupPointId().isBlank()) {
                    addrMap.putIfAbsent("pickupPointId", request.getPickupPointId().trim());
                }
                if (request.getPickupAddress() != null && !request.getPickupAddress().isBlank()) {
                    addrMap.putIfAbsent("pickupAddress", request.getPickupAddress().trim());
                }
                String addressJson = objectMapper.writeValueAsString(addrMap);
                order.setShippingAddress(addressJson);

                // Извлекаем имя и телефон для удобного отображения
                String firstName = (String) addrMap.getOrDefault("firstName", "");
                String lastName = (String) addrMap.getOrDefault("lastName", "");
                String phone = (String) addrMap.get("phone");
                String email = (String) addrMap.get("email");

                order.setCustomerName((firstName + " " + lastName).trim());
                order.setPhone(phone);
                order.setEmail(email);
                sharedUserSyncService.ensureCustomerByEmail(email, order.getCustomerName());
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
                if (Boolean.TRUE.equals(request.getNewsletterSubscribe()) && savedOrder.getEmail() != null) {
                    try {
                        newsletterService.subscribe(savedOrder.getEmail());
                    } catch (IllegalArgumentException ignored) {
                        // некорректный email — заказ уже создан
                    }
                }
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
            errors.put("shippingAddress", "Укажите контактные данные");
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
            if (city == null || city.isBlank()) {
                errors.put("shippingAddress.city", "Укажите город");
            }
            String email = (String) addr.get("email");
            if (email != null && !email.isBlank() && !EMAIL_PATTERN.matcher(email).matches()) {
                errors.put("shippingAddress.email", "Некорректный формат email");
            }

            String method = request.getShippingMethod() != null ? request.getShippingMethod().trim().toLowerCase() : "";
            boolean isPickup = "pickup".equals(method);
            String pickupPointId = request.getPickupPointId();
            if (pickupPointId == null || pickupPointId.isBlank()) {
                Object pObj = addr.get("pickupPointId");
                pickupPointId = pObj != null ? String.valueOf(pObj).trim() : "";
            }
            if (isPickup) {
                if (pickupPointId.isEmpty()) {
                    errors.put("pickupPointId", "Выберите пункт самовывоза");
                }
            } else {
                if (address == null || address.isBlank()) {
                    errors.put("shippingAddress.address", "Укажите адрес доставки");
                }
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
        return orderService.findByOrderId(orderId)
                .map(o -> okSingle(orderDetailMap(o)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Гостевой кабинет: запросить код на email (если есть заказы с этим email — уходит письмо). */
    @PostMapping("/orders/lookup/request-code")
    public ResponseEntity<Map<String, Object>> requestOrderLookupCode(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> b = body != null ? body : Collections.emptyMap();
        String email = b.get("email") instanceof String ? (String) b.get("email") : "";
        try {
            guestOrderLookupService.requestCode(email);
        } catch (IllegalArgumentException e) {
            return apiError(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (GuestOrderLookupService.LookupRateLimitedException e) {
            return apiError(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", "Если этот адрес связан с заказами, мы отправили на него код. Проверьте почту (включая папку «Спам»).");
        return ok(data);
    }

    /** Гостевой кабинет: проверить код и получить список заказов по email. */
    @PostMapping("/orders/lookup/verify")
    public ResponseEntity<Map<String, Object>> verifyOrderLookup(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> b = body != null ? body : Collections.emptyMap();
        String email = b.get("email") instanceof String ? (String) b.get("email") : "";
        String code = b.get("code") != null ? String.valueOf(b.get("code")) : "";
        Map<String, Object> tgStatus = telegramLinkService.status(email, "");
        if (!Boolean.TRUE.equals(tgStatus.get("linked"))) {
            return apiError(HttpStatus.FORBIDDEN,
                    "Требуется подтвержденная привязка Telegram. Подтвердите аккаунт через бота и повторите вход.");
        }
        try {
            List<Order> orders = guestOrderLookupService.verifyAndListOrders(email, code);
            sharedUserSyncService.ensureCustomerByEmail(email, "");
            return ok(orders.stream().map(this::orderDetailMap).collect(Collectors.toList()));
        } catch (IllegalArgumentException e) {
            return apiError(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (GuestOrderLookupService.LookupAuthException e) {
            return apiError(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    // ===== Preorders (корзина -> заявка сотруднику) =====
    /**
     * Создание предзаказа из корзины модуля 2.
     *
     * Контракт ТЗ (раздел 5):
     *  - сумма доставки не учитывается;
     *  - обязателен хотя бы один из telegramUsername/maxUsername;
     *  - статусная модель: NEW_PREORDER -> IN_PROCESS -> CONFIRMED/REJECTED.
     */
    @PostMapping("/preorders")
    public ResponseEntity<Map<String, Object>> createPreorder(@RequestBody CreatePreorderRequest request) {
        try {
            Preorder saved = preorderService.create(request);
            analyticsService.trackEvent("preorder_created", saved.getId(), saved.getPreorderId());
            return okSingle(preorderMap(saved));
        } catch (PreorderService.ValidationException ve) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", "error");
            body.put("message", "Ошибка валидации");
            body.put("errors", ve.getErrors());
            return ResponseEntity.badRequest().body(body);
        } catch (Exception e) {
            log.error("Failed to create preorder", e);
            return apiError(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Не удалось создать предзаказ. Попробуйте позже.");
        }
    }

    @GetMapping("/preorders")
    public ResponseEntity<Map<String, Object>> listPreorders(@RequestParam(required = false) String status) {
        List<Preorder> list;
        if (status != null && !status.isBlank()) {
            try {
                list = preorderService.findByStatus(Preorder.PreorderStatus.valueOf(status));
            } catch (IllegalArgumentException ex) {
                return apiError(HttpStatus.BAD_REQUEST, "Недопустимый статус: " + status);
            }
        } else {
            list = preorderService.findAll();
        }
        return ok(list.stream().map(this::preorderMap).collect(Collectors.toList()));
    }

    @GetMapping("/preorders/{preorderId}")
    public ResponseEntity<Map<String, Object>> getPreorder(@PathVariable String preorderId) {
        return preorderService.findByPreorderId(preorderId)
                .map(p -> okSingle(preorderMap(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Переключение статуса предзаказа (для админки). */
    @PatchMapping("/preorders/{preorderId}/status")
    public ResponseEntity<Map<String, Object>> updatePreorderStatus(
            @PathVariable String preorderId,
            @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> b = body != null ? body : Collections.emptyMap();
        String statusStr = b.get("status") instanceof String ? (String) b.get("status") : "";
        try {
            Preorder.PreorderStatus next = Preorder.PreorderStatus.valueOf(statusStr);
            Preorder saved = preorderService.updateStatus(preorderId, next);
            return okSingle(preorderMap(saved));
        } catch (IllegalArgumentException e) {
            return apiError(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    // ===== Telegram link (deep-link account confirmation / order notifications) =====
    @PostMapping("/telegram/link/request")
    public ResponseEntity<Map<String, Object>> requestTelegramLink(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> b = body != null ? body : Collections.emptyMap();
        String email = b.get("email") instanceof String ? (String) b.get("email") : "";
        String phone = b.get("phone") instanceof String ? (String) b.get("phone") : "";
        try {
            return okSingle(telegramLinkService.requestLink(email, phone));
        } catch (IllegalArgumentException e) {
            return apiError(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            return apiError(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        }
    }

    // ===== User progress sync (этап 2: серверный профиль прогресса) =====
    @GetMapping("/user-progress")
    public ResponseEntity<Map<String, Object>> getUserProgress(@RequestParam String email) {
        try {
            return okSingle(userProgressService.getSnapshotByEmail(email));
        } catch (IllegalArgumentException e) {
            return apiError(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/user-progress/sync")
    public ResponseEntity<Map<String, Object>> syncUserProgress(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> b = body != null ? body : Collections.emptyMap();
        String email = b.get("email") instanceof String ? (String) b.get("email") : "";
        @SuppressWarnings("unchecked")
        Map<String, Object> snapshot = b.get("snapshot") instanceof Map ? (Map<String, Object>) b.get("snapshot") : new LinkedHashMap<>();
        try {
            return okSingle(userProgressService.upsertSnapshot(email, snapshot));
        } catch (IllegalArgumentException e) {
            return apiError(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    // ===== Client profile auth =====
    @PostMapping("/profile/register")
    public ResponseEntity<Map<String, Object>> registerClientProfile(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> b = body != null ? body : Collections.emptyMap();
        String login = b.get("login") instanceof String ? (String) b.get("login") : "";
        String email = b.get("email") instanceof String ? (String) b.get("email") : "";
        String password = b.get("password") instanceof String ? (String) b.get("password") : "";
        try {
            return okSingle(sharedUserSyncService.registerClient(login, email, password));
        } catch (IllegalArgumentException e) {
            return apiError(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/profile/login")
    public ResponseEntity<Map<String, Object>> loginClientProfile(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> b = body != null ? body : Collections.emptyMap();
        String loginOrEmail = b.get("loginOrEmail") instanceof String ? (String) b.get("loginOrEmail") : "";
        String password = b.get("password") instanceof String ? (String) b.get("password") : "";
        try {
            return okSingle(sharedUserSyncService.loginClient(loginOrEmail, password));
        } catch (IllegalArgumentException e) {
            return apiError(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/user-progress/confirmed")
    public ResponseEntity<Map<String, Object>> getConfirmedProgress(@RequestParam String email) {
        try {
            return okSingle(routeProgressService.getConfirmedStats(email));
        } catch (IllegalArgumentException e) {
            return apiError(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/rewards/redeem")
    public ResponseEntity<Map<String, Object>> redeemReward(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> b = body != null ? body : Collections.emptyMap();
        String email = b.get("email") instanceof String ? (String) b.get("email") : "";
        Long routeId = null;
        if (b.get("routeId") instanceof Number n) routeId = n.longValue();
        try {
            if (routeId == null) throw new IllegalArgumentException("routeId обязателен.");
            analyticsService.trackEvent("reward_redeemed", routeId, "route_" + routeId);
            return okSingle(routeProgressService.redeemReward(email, routeId));
        } catch (IllegalArgumentException e) {
            return apiError(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/telegram/link/status")
    public ResponseEntity<Map<String, Object>> telegramLinkStatus(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone) {
        return okSingle(telegramLinkService.status(email, phone));
    }

    @PostMapping("/internal/telegram/link/confirm")
    public ResponseEntity<Map<String, Object>> confirmTelegramLink(
            @RequestHeader(value = "X-Service-Token", required = false) String serviceToken,
            @RequestBody(required = false) Map<String, Object> body) {
        String expectedToken = System.getenv("TELEGRAM_INTERNAL_TOKEN");
        if (expectedToken == null || expectedToken.isBlank()) {
            expectedToken = telegramInternalToken;
        }
        if (expectedToken == null || expectedToken.isBlank()) {
            return apiError(HttpStatus.SERVICE_UNAVAILABLE, "Internal Telegram token is not configured.");
        }
        if (serviceToken == null || !expectedToken.equals(serviceToken)) {
            return apiError(HttpStatus.UNAUTHORIZED, "Invalid service token.");
        }
        Map<String, Object> b = body != null ? body : Collections.emptyMap();
        String token = b.get("token") instanceof String ? (String) b.get("token") : "";
        Long chatId = null;
        if (b.get("chatId") instanceof Number n) {
            chatId = n.longValue();
        } else if (b.get("chatId") != null) {
            try {
                chatId = Long.parseLong(String.valueOf(b.get("chatId")));
            } catch (NumberFormatException ignored) {
            }
        }
        String username = b.get("username") instanceof String ? (String) b.get("username") : "";
        try {
            return okSingle(telegramLinkService.confirmLink(token, chatId, username));
        } catch (IllegalArgumentException e) {
            return apiError(HttpStatus.BAD_REQUEST, e.getMessage());
        }
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
        Double lat = null;
        Double lng = null;
        if (body.get("latitude") instanceof Number n) {
            lat = n.doubleValue();
        } else if (body.get("latitude") != null) {
            try {
                lat = Double.parseDouble(String.valueOf(body.get("latitude")));
            } catch (NumberFormatException ignored) {
            }
        }
        if (body.get("longitude") instanceof Number n) {
            lng = n.doubleValue();
        } else if (body.get("longitude") != null) {
            try {
                lng = Double.parseDouble(String.valueOf(body.get("longitude")));
            } catch (NumberFormatException ignored) {
            }
        }
        PoiSuggestion s = PoiSuggestion.builder()
                .name(name.trim())
                .place(place.trim())
                .description(description != null ? description.trim() : null)
                .whyAdd(whyAdd.trim())
                .latitude(lat)
                .longitude(lng)
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

    // ===== Newsletter (новые точки на карте) =====
    @PostMapping("/newsletter/subscribe")
    public ResponseEntity<Map<String, Object>> newsletterSubscribe(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> b = body != null ? body : Collections.emptyMap();
        String email = b.get("email") instanceof String ? (String) b.get("email") : "";
        try {
            newsletterService.subscribe(email);
        } catch (IllegalArgumentException e) {
            return apiError(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", "Вы подписались на уведомления о новых точках на карте.");
        return ok(data);
    }

    @GetMapping(value = "/newsletter/unsubscribe", produces = "text/html;charset=UTF-8")
    public ResponseEntity<String> newsletterUnsubscribe(@RequestParam String token) {
        boolean ok = newsletterService.unsubscribeByToken(token);
        String body = ok
                ? "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Отписка</title></head><body style=\"font-family:sans-serif;padding:24px;\"><p>Вы отписаны от рассылки.</p><p><a href=\"/\">На главную</a></p></body></html>"
                : "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Отписка</title></head><body style=\"font-family:sans-serif;padding:24px;\"><p>Ссылка недействительна или подписка уже отключена.</p></body></html>";
        return ResponseEntity.ok(body);
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

    private static ResponseEntity<Map<String, Object>> apiError(HttpStatus status, String message) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("status", "error");
        err.put("message", message);
        return ResponseEntity.status(status).body(err);
    }

    private Map<String, Object> poiMap(PointOfInterest p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId()); m.put("name", p.getName()); m.put("description", p.getDescription());
        m.put("shortDescription", p.getShortDescription() != null ? p.getShortDescription() : p.getDescription());
        m.put("category", p.getCategory()); m.put("latitude", p.getLatitude()); m.put("longitude", p.getLongitude());
        m.put("address", p.getAddress());
        m.put("style", p.getStyle() != null ? p.getStyle() : "");
        m.put("image", poiCoverImageUrl(p));
        m.put("phone", p.getPhone()); m.put("website", p.getWebsite());
        m.put("tags", p.getTags() != null ? Arrays.asList(p.getTags().split(",")) : List.of());
        return m;
    }
    private Map<String, Object> poiDetailMap(PointOfInterest p) {
        Map<String, Object> m = poiMap(p);
        m.put("email", p.getEmail());
        m.put("detailText", p.getDetailText());
        m.put("style", p.getStyle() != null ? p.getStyle() : "");
        m.put("maxAudioUrl", p.getMaxAudioUrl());
        m.put("maxVideoUrl", p.getMaxVideoUrl());
        m.put("maxPlaylistUrl", p.getMaxPlaylistUrl());
        Map<String, Object> extended = new LinkedHashMap<>();
        extended.put("foundedYear", p.getFoundedYear() != null ? p.getFoundedYear() : 0);
        extended.put("architect", p.getArchitect() != null ? p.getArchitect() : "");
        extended.put("material", p.getMaterial() != null ? p.getMaterial() : "");
        extended.put("style", p.getStyle() != null ? p.getStyle() : "");
        extended.put("shortDescription", p.getShortDescription() != null ? p.getShortDescription() : "");
        extended.put("detailText", p.getDetailText() != null ? p.getDetailText() : "");
        m.put("extendedInfo", extended);
        m.put("coordinates", Map.of(
            "latitude", p.getLatitude() != null ? p.getLatitude() : 0,
            "longitude", p.getLongitude() != null ? p.getLongitude() : 0));
        return m;
    }

    /** Есть загруженный файл в БД (проверка по filename, без чтения bytea). */
    private static boolean hasUploadedImage(String imageFilename) {
        return imageFilename != null && !imageFilename.isBlank();
    }

    /** Внешний URL или путь к API для обложки: при загрузке файла в админке — только imageData, без imageUrl. */
    private static String poiCoverImageUrl(PointOfInterest p) {
        if (p.getImageUrl() != null && !p.getImageUrl().isBlank()) {
            return p.getImageUrl().trim();
        }
        if (hasUploadedImage(p.getImageFilename()) && p.getId() != null) {
            return "/java-api/api/v1/pois/" + p.getId() + "/image";
        }
        return null;
    }

    private static String routeCoverImageUrl(Route r) {
        if (r.getImageUrl() != null && !r.getImageUrl().isBlank()) {
            return r.getImageUrl().trim();
        }
        if (r.getId() != null && hasUploadedImage(r.getImageFilename())) {
            return "/java-api/api/v1/routes/" + r.getId() + "/image";
        }
        return null;
    }

    private static String productCoverImageUrl(Product p) {
        if (p.getImageUrl() != null && !p.getImageUrl().isEmpty()) {
            return p.getImageUrl();
        }
        if (hasUploadedImage(p.getImageFilename()) && p.getId() != null) {
            return "/java-api/api/v1/products/" + p.getId() + "/image";
        }
        return null;
    }

    /** Публичный список маршрутов — без N+1 загрузки ТОИ (снижает риск обрыва chunked-ответа). */
    private Map<String, Object> routeMapListItem(Route r) {
        return routeMapCore(r, false);
    }

    /** Детальная карточка маршрута — с остановками по связанным ТОИ. */
    private Map<String, Object> routeMapDetail(Route r) {
        return routeMapCore(r, true);
    }

    private static List<Long> parseRoutePoiIds(String poiIds) {
        if (poiIds == null || poiIds.isBlank()) {
            return List.of();
        }
        List<Long> out = new ArrayList<>();
        for (String part : poiIds.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                out.add(Long.parseLong(trimmed));
            } catch (NumberFormatException ignored) {
                /* пропускаем битые id в строке poi_ids */
            }
        }
        return out;
    }

    private Map<String, Object> routeMapCore(Route r, boolean withStops) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("name", r.getName());
        m.put("title", r.getName());
        m.put("description", r.getDescription());
        m.put("thematicDescription", r.getThematicDescription());
        m.put("videoUrls", parseUrlList(r.getVideoUrls()));
        m.put("audioUrls", parseUrlList(r.getAudioUrls()));
        m.put("category", r.getCategory());
        m.put("distance", formatDistance(r.getDistance()));
        m.put("duration", formatDuration(r.getDuration()));
        m.put("difficulty", r.getDifficulty());
        m.put("rating", r.getRating() != null ? r.getRating() : 0.0);
        m.put("published", r.getPublished());
        m.put("paid", r.getPaid() != null && r.getPaid());
        m.put("isPaid", r.getPaid() != null && r.getPaid());
        m.put("price", r.getPrice() != null ? r.getPrice() : 0.0);
        m.put("priority", r.getPriority() != null ? r.getPriority() : 0);
        m.put("status", r.getStatus() != null ? r.getStatus().name() : Route.RouteStatus.DRAFT.name());
        m.put("outdatedReason", r.getOutdatedReason());
        m.put("isActual", r.getStatus() == null || r.getStatus() != Route.RouteStatus.OUTDATED);
        List<Long> poiIdList = parseRoutePoiIds(r.getPoiIds());
        m.put("pois", poiIdList);
        if (withStops && !poiIdList.isEmpty()) {
            m.put("stops", buildRouteStops(r));
        } else {
            m.put("stops", List.of());
        }
        String coverImage = routeCoverImageUrl(r);
        m.put("image", coverImage);
        m.put("coverImage", coverImage);
        return m;
    }

    private List<Map<String, Object>> buildRouteStops(Route r) {
        List<Long> poiIdList = parseRoutePoiIds(r.getPoiIds());
        Map<Long, RouteStopsHelper.StopContent> contentByPoi = RouteStopsHelper.indexByPoiId(r.getWaypoints());
        List<Map<String, Object>> stops = new ArrayList<>();
        for (Long poiId : poiIdList) {
            Map<String, Object> stop = new LinkedHashMap<>();
            stop.put("poiId", poiId);
            poiService.findById(poiId).ifPresentOrElse(poi -> {
                stop.put("name", poi.getName());
                stop.put("description", poi.getDescription() != null ? poi.getDescription() : "");
            }, () -> {
                stop.put("name", "Точка #" + poiId);
                stop.put("description", "");
            });
            RouteStopsHelper.StopContent c = contentByPoi.get(poiId);
            String thematic = c != null && c.thematicDescription != null ? c.thematicDescription : "";
            stop.put("thematicDescription", thematic);
            stop.put("videoUrls", c != null ? parseUrlList(c.videoUrls) : List.of());
            stop.put("audioUrls", c != null ? parseUrlList(c.audioUrls) : List.of());
            stops.add(stop);
        }
        return stops;
    }

    /** Ссылки из админки / JSON bulk: по одной в строке или через запятую. */
    private static List<String> parseUrlList(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String part : raw.split("[\\r\\n,]+")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty() && !out.contains(trimmed)) {
                out.add(trimmed);
            }
        }
        return out;
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
        m.put("image", productCoverImageUrl(p)); m.put("rating", p.getRating()); m.put("inStock", p.getInStock());
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
        String pricingType = resolveOrderPricingType(o);
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
        // Явный серверный контракт для фильтрации в ЛК.
        m.put("orderType", pricingType);
        m.put("routeType", pricingType);
        return m;
    }

    /** Полная карточка заказа для гостя (кабинет / оплата): позиции из items_json */
    private Map<String, Object> orderDetailMap(Order o) {
        Map<String, Object> m = orderMap(o);
        m.put("items", o.getItemsList());
        return m;
    }

    /** Сериализация предзаказа для публичного API. Безопасна для null-полей. */
    private Map<String, Object> preorderMap(Preorder p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("preorderId", p.getPreorderId());
        m.put("customerName", p.getCustomerName());
        m.put("phone", p.getPhone());
        m.put("email", p.getEmail());
        m.put("telegramUsername", p.getTelegramUsername());
        m.put("maxUsername", p.getMaxUsername());
        m.put("comment", p.getComment());
        m.put("total", p.getTotal() != null ? p.getTotal() : 0.0);
        m.put("status", p.getStatus() != null ? p.getStatus().name() : Preorder.PreorderStatus.NEW_PREORDER.name());
        m.put("createdAt", p.getCreatedAt() != null ? p.getCreatedAt().toString() : "");
        m.put("updatedAt", p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : "");
        try {
            List<Map<String, Object>> items = objectMapper.readValue(
                    p.getItemsJson() != null ? p.getItemsJson() : "[]",
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            m.put("items", items);
        } catch (Exception e) {
            m.put("items", List.of());
        }
        return m;
    }

    private String resolveOrderPricingType(Order o) {
        try {
            List<Map<String, Object>> items = o.getItemsList();
            if (items != null && !items.isEmpty()) {
                double total = 0;
                for (Map<String, Object> item : items) {
                    Object p = item.get("price");
                    if (p instanceof Number n) total += n.doubleValue();
                    else if (p != null) {
                        try { total += Double.parseDouble(String.valueOf(p)); } catch (Exception ignored) {}
                    }
                }
                return total > 0 ? "paid" : "free";
            }
        } catch (Exception ignored) {
        }
        return (o.getTotal() != null && o.getTotal() > 0) ? "paid" : "free";
    }
}
