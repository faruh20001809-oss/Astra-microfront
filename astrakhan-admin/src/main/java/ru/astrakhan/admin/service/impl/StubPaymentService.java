package ru.astrakhan.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import ru.astrakhan.admin.dto.PaymentLinkResult;
import ru.astrakhan.admin.entity.Order;
import ru.astrakhan.admin.service.OrderService;
import ru.astrakhan.admin.service.PaymentService;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Заглушка платёжного сервиса для разработки и тестов.
 * В проде заменить на реализацию с YooKassa / Robokassa / Tinkoff и т.д.
 */
@Service
@Primary
@Slf4j
@RequiredArgsConstructor
public class StubPaymentService implements PaymentService {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @Value("${app.payment.enabled:true}")
    private boolean paymentEnabled;

    @Override
    public PaymentLinkResult createPaymentLink(Order order, String returnUrl, String cancelUrl) {
        if (!paymentEnabled) {
            log.warn("Payment is disabled (app.payment.enabled=false)");
            return null;
        }
        String paymentId = "stub-" + order.getOrderId();
        order.setPaymentId(paymentId);
        orderService.save(order);

        String redirectUrl = buildStubRedirectUrl(returnUrl, order.getOrderId());
        log.info("Stub payment created: orderId={}, paymentId={}, redirectUrl={}", order.getOrderId(), paymentId, redirectUrl);

        return PaymentLinkResult.builder()
                .paymentId(paymentId)
                .redirectUrl(redirectUrl)
                .orderId(order.getOrderId())
                .amount(order.getTotal())
                .currency(order.getCurrency() != null ? order.getCurrency() : "RUB")
                .build();
    }

    @Override
    public boolean processWebhook(String rawPayload) {
        if (rawPayload == null || rawPayload.isBlank()) return false;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = objectMapper.readValue(rawPayload, Map.class);
            String orderId = (String) body.get("orderId");
            Boolean success = body.get("success") instanceof Boolean ? (Boolean) body.get("success") : "true".equals(String.valueOf(body.get("success")));
            if (orderId == null || orderId.isBlank()) {
                log.warn("Stub webhook: missing orderId");
                return false;
            }
            if (Boolean.TRUE.equals(success)) {
                orderService.findByOrderId(orderId).ifPresent(order -> {
                    order.setPaidAt(LocalDateTime.now());
                    order.setPaymentId(order.getPaymentId() != null ? order.getPaymentId() : "stub-" + orderId);
                    orderService.save(order);
                    log.info("Stub webhook: order {} marked as paid", orderId);
                });
            }
            return true;
        } catch (Exception e) {
            log.error("Stub webhook parse error", e);
            return false;
        }
    }

    @Override
    public boolean isOrderPaid(Order order) {
        return order != null && order.getPaidAt() != null;
    }

    private String buildStubRedirectUrl(String returnUrl, String orderId) {
        if (returnUrl == null || returnUrl.isBlank()) {
            return "https://example.com/payment/success?orderId=" + orderId + "&stub=1";
        }
        return returnUrl + (returnUrl.contains("?") ? "&" : "?") + "orderId=" + orderId + "&stub=1";
    }
}
