package ru.astrakhan.admin.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import ru.astrakhan.admin.entity.Order;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEmailService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@astrakhan-history.local}")
    private String fromAddress;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    /**
     * Sends "Order accepted for processing" email right after order is placed (e.g. after payment).
     */
    public void sendOrderAcceptedForProcessing(Order order) {
        if (!mailEnabled) {
            log.debug("Mail disabled, skipping order accepted email for {}", order.getOrderId());
            return;
        }
        String email = order.getEmail();
        if (email == null || email.isBlank()) {
            log.debug("Order {} has no email, skipping accepted email", order.getOrderId());
            return;
        }
        try {
            String subject = "Заказ " + order.getOrderId() + " принят в обработку — Астрахань. Живая История";
            String html = buildOrderAcceptedHtml(order);
            sendHtml(email, subject, html);
            log.info("Order accepted email sent for {} to {}", order.getOrderId(), email);
        } catch (Exception e) {
            log.error("Failed to send order accepted email for " + order.getOrderId(), e);
        }
    }

    /**
     * Sends order confirmation email with full order details to the customer.
     */
    public void sendOrderConfirmation(Order order) {
        if (!mailEnabled) {
            log.debug("Mail disabled, skipping confirmation for {}", order.getOrderId());
            return;
        }
        String email = order.getEmail();
        if (email == null || email.isBlank()) {
            log.debug("Order {} has no email, skipping confirmation", order.getOrderId());
            return;
        }
        try {
            String subject = "Заказ " + order.getOrderId() + " — Астрахань. Живая История";
            String html = buildOrderConfirmationHtml(order);
            sendHtml(email, subject, html);
            log.info("Order confirmation email sent for {} to {}", order.getOrderId(), email);
        } catch (Exception e) {
            log.error("Failed to send order confirmation for " + order.getOrderId(), e);
        }
    }

    /**
     * Sends tracking number email when order is shipped.
     */
    public void sendTrackingUpdate(Order order) {
        if (!mailEnabled) {
            log.debug("Mail disabled, skipping tracking email for {}", order.getOrderId());
            return;
        }
        String email = order.getEmail();
        if (email == null || email.isBlank()) {
            log.debug("Order {} has no email, skipping tracking email", order.getOrderId());
            return;
        }
        String tracking = order.getTrackingNumber();
        if (tracking == null || tracking.isBlank()) {
            log.debug("Order {} has no tracking number", order.getOrderId());
            return;
        }
        try {
            String subject = "Отправка заказа " + order.getOrderId() + " — трек-номер";
            String html = buildTrackingHtml(order);
            sendHtml(email, subject, html);
            log.info("Tracking email sent for {} to {}", order.getOrderId(), email);
        } catch (Exception e) {
            log.error("Failed to send tracking email for " + order.getOrderId(), e);
        }
    }

    private void sendHtml(String to, String subject, String html) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(message);
    }

    private String buildOrderConfirmationHtml(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head><body style=\"font-family:sans-serif;max-width:600px;margin:0 auto;padding:20px;\">");
        sb.append("<h2>Подтверждение заказа</h2>");
        sb.append("<p>Здравствуйте").append(order.getCustomerName() != null && !order.getCustomerName().isBlank() ? ", " + escape(order.getCustomerName()) : "").append("!</p>");
        sb.append("<p>Ваш заказ <strong>").append(escape(order.getOrderId())).append("</strong> принят.</p>");
        sb.append("<table border=\"0\" cellpadding=\"8\" style=\"border-collapse:collapse;width:100%;\">");
        sb.append("<tr><td style=\"background:#f5f5f5;\">Номер заказа</td><td>").append(escape(order.getOrderId())).append("</td></tr>");
        sb.append("<tr><td style=\"background:#f5f5f5;\">Статус</td><td>").append(escape(order.getStatus().name())).append("</td></tr>");
        sb.append("<tr><td style=\"background:#f5f5f5;\">Сумма</td><td>").append(order.getTotal() != null ? order.getTotal() : 0).append(" ").append(escape(order.getCurrency() != null ? order.getCurrency() : "RUB")).append("</td></tr>");
        sb.append("<tr><td style=\"background:#f5f5f5;\">Способ доставки</td><td>").append(escape(order.getShippingMethod())).append("</td></tr>");
        sb.append("<tr><td style=\"background:#f5f5f5;\">Адрес</td><td>").append(escape(order.getFormattedAddress())).append("</td></tr>");
        if (order.getCreatedAt() != null) {
            sb.append("<tr><td style=\"background:#f5f5f5;\">Дата</td><td>").append(order.getCreatedAt().format(DATE_FMT)).append("</td></tr>");
        }
        sb.append("</table>");
        List<Map<String, Object>> items = order.getItemsList();
        if (items != null && !items.isEmpty()) {
            sb.append("<h3>Состав заказа</h3><table border=\"0\" cellpadding=\"6\" style=\"border-collapse:collapse;width:100%;\">");
            sb.append("<tr style=\"background:#f5f5f5;\"><th style=\"text-align:left;\">Товар</th><th>Цена</th><th>Кол-во</th><th>Сумма</th></tr>");
            for (Map<String, Object> item : items) {
                String name = String.valueOf(item.getOrDefault("name", ""));
                Object priceObj = item.getOrDefault("price", 0);
                Object qtyObj = item.getOrDefault("qty", 1);
                double price = priceObj instanceof Number ? ((Number) priceObj).doubleValue() : 0;
                int qty = qtyObj instanceof Number ? ((Number) qtyObj).intValue() : 1;
                double sum = price * qty;
                sb.append("<tr><td>").append(escape(name)).append("</td><td>").append(price).append(" ₽</td><td>").append(qty).append("</td><td>").append(sum).append(" ₽</td></tr>");
            }
            sb.append("<tr><td colspan=\"3\" style=\"text-align:right;\"><strong>Итого:</strong></td><td><strong>").append(order.getTotal() != null ? order.getTotal() : 0).append(" ₽</strong></td></tr>");
            sb.append("</table>");
        }
        sb.append("<p style=\"color:#666;margin-top:24px;\">— Астрахань. Живая История</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private String buildOrderAcceptedHtml(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head><body style=\"font-family:sans-serif;max-width:600px;margin:0 auto;padding:20px;\">");
        sb.append("<h2>Ваш заказ принят в обработку</h2>");
        sb.append("<p>Здравствуйте").append(order.getCustomerName() != null && !order.getCustomerName().isBlank() ? ", " + escape(order.getCustomerName()) : "").append("!</p>");
        sb.append("<p>Спасибо за заказ. Мы получили ваш заказ <strong>").append(escape(order.getOrderId())).append("</strong> и приняли его в обработку.</p>");
        sb.append("<p>Сумма заказа: <strong>").append(order.getTotal() != null ? order.getTotal() : 0).append(" ₽</strong>.</p>");
        sb.append("<p>Когда заказ будет отправлен, мы пришлём вам письмо с трек-номером для отслеживания.</p>");
        sb.append("<p style=\"color:#666;margin-top:24px;\">— Астрахань. Живая История</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private String buildTrackingHtml(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head><body style=\"font-family:sans-serif;max-width:600px;margin:0 auto;padding:20px;\">");
        sb.append("<h2>Ваш заказ отправлен</h2>");
        sb.append("<p>Здравствуйте").append(order.getCustomerName() != null && !order.getCustomerName().isBlank() ? ", " + escape(order.getCustomerName()) : "").append("!</p>");
        sb.append("<p>Заказ <strong>").append(escape(order.getOrderId())).append("</strong> передан в доставку.</p>");
        sb.append("<p><strong>Трек-номер для отслеживания:</strong></p>");
        sb.append("<p style=\"font-size:18px;background:#f5f5f5;padding:12px;border-radius:6px;\">").append(escape(order.getTrackingNumber())).append("</p>");
        sb.append("<p>Используйте его на сайте службы доставки для отслеживания посылки.</p>");
        sb.append("<p style=\"color:#666;margin-top:24px;\">— Астрахань. Живая История</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
