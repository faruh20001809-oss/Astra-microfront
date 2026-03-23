package ru.astrakhan.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.astrakhan.admin.entity.Order;
import ru.astrakhan.admin.repository.OrderRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Одноразовый код в памяти процесса: запрос на email → письмо → проверка → список заказов по этому email.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GuestOrderLookupService {

    private static final long CODE_TTL_MS = 10 * 60 * 1000L;
    private static final long RESEND_COOLDOWN_MS = 60_000L;
    private static final int MAX_FAILS = 5;

    private final OrderRepository orderRepository;
    private final OrderEmailService orderEmailService;

    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<String, Pending> pendingByEmail = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastRequestAt = new ConcurrentHashMap<>();

    private static final class Pending {
        String code;
        long expiresAtMs;
        int failCount;
    }

    /**
     * Отправляет код на email, если по нему есть неудалённые заказы. Иначе письмо не шлём (ответ для клиента одинаковый).
     *
     * @throws IllegalArgumentException невалидный email
     * @throws LookupRateLimitedException повторный запрос раньше чем через минуту
     */
    public void requestCode(String emailRaw) {
        String email = normalizeEmail(emailRaw);
        if (email.isEmpty()) {
            throw new IllegalArgumentException("Укажите email");
        }
        if (!email.contains("@") || email.length() < 5) {
            throw new IllegalArgumentException("Некорректный email");
        }

        long now = System.currentTimeMillis();
        Long last = lastRequestAt.get(email);
        if (last != null && now - last < RESEND_COOLDOWN_MS) {
            throw new LookupRateLimitedException("Новый код можно запросить не чаще одного раза в минуту");
        }

        List<Order> orders = orderRepository.findByEmailIgnoreCaseAndDeletedFalseOrderByCreatedAtDesc(email);
        lastRequestAt.put(email, now);

        if (orders.isEmpty()) {
            log.debug("Order lookup: no orders for email, skip mail");
            return;
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        Pending p = new Pending();
        p.code = code;
        p.expiresAtMs = now + CODE_TTL_MS;
        p.failCount = 0;
        pendingByEmail.put(email, p);

        orderEmailService.sendGuestOrderLookupCode(email, code);
    }

    /**
     * Проверяет код и возвращает заказы с этим email.
     *
     * @throws IllegalArgumentException пустые поля
     * @throws LookupAuthException неверный / просроченный код
     */
    public List<Order> verifyAndListOrders(String emailRaw, String codeRaw) {
        String email = normalizeEmail(emailRaw);
        if (email.isEmpty()) {
            throw new IllegalArgumentException("Укажите email");
        }
        String code = normalizeCodeDigits(codeRaw);
        if (code.length() != 6) {
            throw new IllegalArgumentException("Введите 6-значный код из письма");
        }

        Pending p = pendingByEmail.get(email);
        if (p == null) {
            throw new LookupAuthException("Сначала запросите код на этот адрес");
        }
        long now = System.currentTimeMillis();
        if (now > p.expiresAtMs) {
            pendingByEmail.remove(email);
            throw new LookupAuthException("Код истёк — запросите новый");
        }

        if (!constantTimeEqual(p.code, code)) {
            p.failCount++;
            if (p.failCount >= MAX_FAILS) {
                pendingByEmail.remove(email);
            }
            throw new LookupAuthException("Неверный код");
        }

        pendingByEmail.remove(email);
        return orderRepository.findByEmailIgnoreCaseAndDeletedFalseOrderByCreatedAtDesc(email);
    }

    private static String normalizeEmail(String raw) {
        if (raw == null) return "";
        return raw.trim().toLowerCase();
    }

    private static String normalizeCodeDigits(String raw) {
        if (raw == null) return "";
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() > 6) {
            digits = digits.substring(0, 6);
        }
        while (digits.length() < 6) {
            digits = "0" + digits;
        }
        return digits;
    }

    private static boolean constantTimeEqual(String a, String b) {
        byte[] ba = a.getBytes(StandardCharsets.UTF_8);
        byte[] bb = b.getBytes(StandardCharsets.UTF_8);
        if (ba.length != bb.length) {
            return false;
        }
        return MessageDigest.isEqual(ba, bb);
    }

    public static final class LookupRateLimitedException extends RuntimeException {
        public LookupRateLimitedException(String message) {
            super(message);
        }
    }

    public static final class LookupAuthException extends RuntimeException {
        public LookupAuthException(String message) {
            super(message);
        }
    }
}
