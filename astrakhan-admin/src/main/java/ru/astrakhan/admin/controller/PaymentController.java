package ru.astrakhan.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.astrakhan.admin.dto.PaymentLinkResult;
import ru.astrakhan.admin.entity.Order;
import ru.astrakhan.admin.service.OrderService;
import ru.astrakhan.admin.service.PaymentService;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * API для онлайн-оплаты заказов (заготовка под интеграцию с платёжным провайдером).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    /**
     * Создать платёж и получить ссылку на страницу оплаты.
     * Тело: { "returnUrl": "https://...", "cancelUrl": "https://..." }
     */
    @PostMapping("/orders/{orderId}/payment")
    public ResponseEntity<Map<String, Object>> createPayment(
            @PathVariable String orderId,
            @RequestBody Map<String, Object> body) {
        Order order = orderService.findByOrderId(orderId).orElse(null);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        if (paymentService.isOrderPaid(order)) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("status", "error");
            err.put("message", "Заказ уже оплачен");
            return ResponseEntity.badRequest().body(err);
        }
        String returnUrl = body != null ? (String) body.get("returnUrl") : null;
        String cancelUrl = body != null ? (String) body.get("cancelUrl") : null;

        PaymentLinkResult result = paymentService.createPaymentLink(order, returnUrl, cancelUrl);
        if (result == null) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("status", "error");
            err.put("message", "Не удалось создать платёж");
            return ResponseEntity.unprocessableEntity().body(err);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("paymentId", result.getPaymentId());
        data.put("redirectUrl", result.getRedirectUrl());
        data.put("orderId", result.getOrderId());
        data.put("amount", result.getAmount());
        data.put("currency", result.getCurrency());
        return ResponseEntity.ok(Map.of("status", "success", "data", data));
    }

    /**
     * Статус оплаты заказа.
     */
    @GetMapping("/orders/{orderId}/payment-status")
    public ResponseEntity<Map<String, Object>> paymentStatus(@PathVariable String orderId) {
        Order order = orderService.findByOrderId(orderId).orElse(null);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        boolean paid = paymentService.isOrderPaid(order);
        String statusLower = order.getStatus() != null ? order.getStatus().name().toLowerCase() : "processing";
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderId", orderId);
        data.put("paid", paid);
        data.put("paidAt", order.getPaidAt() != null ? order.getPaidAt().toString() : null);
        data.put("status", statusLower);
        return ResponseEntity.ok(Map.of("status", "success", "data", data));
    }

    /**
     * Webhook от платёжного провайдера (успех/отмена оплаты).
     * В заглушке можно отправить JSON: { "orderId": "ORD-XXX", "success": true }
     */
    @PostMapping(value = "/payment/webhook", consumes = "application/json")
    public ResponseEntity<Map<String, Object>> webhook(@RequestBody String rawPayload) {
        boolean ok = paymentService.processWebhook(rawPayload);
        return ResponseEntity.ok(Map.of("status", ok ? "ok" : "error"));
    }
}
