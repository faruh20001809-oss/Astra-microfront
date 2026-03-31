package ru.astrakhan.admin.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.astrakhan.admin.entity.Order;
import ru.astrakhan.admin.entity.UserTelegramLink;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class TelegramNotificationService {

    private final TelegramLinkService telegramLinkService;
    private final RestClient restClient;
    private final String serviceToken;
    private final boolean enabled;

    public TelegramNotificationService(
            TelegramLinkService telegramLinkService,
            @Value("${app.telegram.notifications.enabled:false}") boolean enabled,
            @Value("${app.telegram.bot-service-url:http://localhost:8090}") String botServiceUrl,
            @Value("${app.telegram.internal-token:}") String serviceToken) {
        this.telegramLinkService = telegramLinkService;
        this.enabled = enabled;
        this.serviceToken = serviceToken == null ? "" : serviceToken.trim();

        String base = botServiceUrl.endsWith("/") ? botServiceUrl.substring(0, botServiceUrl.length() - 1) : botServiceUrl;
        this.restClient = RestClient.builder()
                .baseUrl(base)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public boolean notifyOrderStatus(Order order) {
        if (!enabled) {
            return false;
        }
        if (serviceToken.isBlank()) {
            log.warn("Telegram notifications are enabled but app.telegram.internal-token is blank.");
            return false;
        }
        Optional<UserTelegramLink> link = telegramLinkService.findLink(order.getEmail(), order.getPhone());
        if (link.isEmpty()) {
            return false;
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("chatId", link.get().getTelegramChatId());
        body.put("orderId", order.getOrderId());
        body.put("status", order.getStatus() != null ? order.getStatus().name() : "");
        body.put("trackingNumber", order.getTrackingNumber());
        body.put("total", order.getTotal());
        body.put("currency", order.getCurrency());

        try {
            restClient.post()
                    .uri("/internal/telegram/send-order-status")
                    .header("X-Service-Token", serviceToken)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            telegramLinkService.touchByOrderContact(order.getEmail(), order.getPhone());
            return true;
        } catch (Exception e) {
            log.warn("Telegram order notification failed for {}: {}", order.getOrderId(), e.getMessage());
            return false;
        }
    }
}
