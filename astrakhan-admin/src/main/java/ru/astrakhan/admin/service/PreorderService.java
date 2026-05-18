package ru.astrakhan.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.astrakhan.admin.dto.CreatePreorderRequest;
import ru.astrakhan.admin.dto.OrderItemRequest;
import ru.astrakhan.admin.entity.Preorder;
import ru.astrakhan.admin.repository.PreorderRepository;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Слой бизнес-логики предзаказов корзины модуля 2.
 *
 * Ответственность:
 *  - валидация контактных полей telegramUsername/maxUsername;
 *  - сериализация позиций корзины в `itemsJson` (без суммы доставки, ТЗ 5.4);
 *  - сохранение и поиск.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PreorderService {

    /** Длина 3-64, латиница/цифры/`_.-`, без пробелов (ТЗ 5.3). */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_.\\-]{3,64}$");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");

    private final PreorderRepository preorderRepository;
    private final ObjectMapper objectMapper;

    public Preorder create(CreatePreorderRequest request) {
        Map<String, String> errors = validate(request);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        String preorderId = "PRE-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        double total = 0.0;
        if (request.getItems() != null) {
            for (OrderItemRequest item : request.getItems()) {
                double price = item.getPrice() != null ? item.getPrice() : 0.0;
                int qty = item.getQty() != null ? item.getQty() : 1;
                total += price * qty;
            }
        }

        String itemsJson;
        try {
            itemsJson = objectMapper.writeValueAsString(
                    request.getItems() != null ? request.getItems() : List.of());
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось сериализовать позиции предзаказа", e);
        }

        Preorder preorder = Preorder.builder()
                .preorderId(preorderId)
                .customerName(trim(request.getCustomerName()))
                .phone(trim(request.getPhone()))
                .email(trim(request.getEmail()))
                .telegramUsername(normalizeUsername(request.getTelegramUsername()))
                .maxUsername(normalizeUsername(request.getMaxUsername()))
                .comment(trim(request.getComment()))
                .itemsJson(itemsJson)
                .total(total)
                .status(Preorder.PreorderStatus.NEW_PREORDER)
                .build();

        Preorder saved = preorderRepository.save(preorder);
        log.info("Preorder created: {} (contacts: tg={}, max={})",
                saved.getPreorderId(), saved.getTelegramUsername(), saved.getMaxUsername());
        return saved;
    }

    public List<Preorder> findAll() {
        return preorderRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Preorder> findByStatus(Preorder.PreorderStatus status) {
        return preorderRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public Optional<Preorder> findByPreorderId(String preorderId) {
        if (preorderId == null) return Optional.empty();
        return preorderRepository.findByPreorderId(preorderId.trim());
    }

    public Preorder updateStatus(String preorderId, Preorder.PreorderStatus status) {
        Preorder p = preorderRepository.findByPreorderId(preorderId)
                .orElseThrow(() -> new IllegalArgumentException("Предзаказ не найден"));
        p.setStatus(status);
        return preorderRepository.save(p);
    }

    /** Валидация контактных полей и состава корзины (ТЗ 5.3). */
    private Map<String, String> validate(CreatePreorderRequest r) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (r.getItems() == null || r.getItems().isEmpty()) {
            errors.put("items", "Корзина пуста — добавьте хотя бы один товар");
        }

        String tg = normalizeUsername(r.getTelegramUsername());
        String mx = normalizeUsername(r.getMaxUsername());

        if ((tg == null || tg.isEmpty()) && (mx == null || mx.isEmpty())) {
            errors.put("contact",
                    "Заполните логин в Telegram или MAX — сотрудник свяжется по нему");
        }
        if (tg != null && !tg.isEmpty() && !USERNAME_PATTERN.matcher(tg).matches()) {
            errors.put("telegramUsername",
                    "3-64 символа: латиница, цифры, `_`, `.`, `-`");
        }
        if (mx != null && !mx.isEmpty() && !USERNAME_PATTERN.matcher(mx).matches()) {
            errors.put("maxUsername",
                    "3-64 символа: латиница, цифры, `_`, `.`, `-`");
        }

        String email = trim(r.getEmail());
        if (email != null && !email.isEmpty() && !EMAIL_PATTERN.matcher(email).matches()) {
            errors.put("email", "Некорректный формат email");
        }

        return errors;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    /** Снимает ведущий `@`, тримит пробелы. */
    private static String normalizeUsername(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.startsWith("@")) v = v.substring(1).trim();
        return v;
    }

    /** Кастомное исключение валидации — несет карту ошибок для красивого ответа API. */
    public static class ValidationException extends RuntimeException {
        private final Map<String, String> errors;
        public ValidationException(Map<String, String> errors) {
            super("Ошибка валидации предзаказа");
            this.errors = errors;
        }
        public Map<String, String> getErrors() { return errors; }
    }
}
