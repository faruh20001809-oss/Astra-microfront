package ru.astrakhan.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Тело запроса POST /api/v1/preorders.
 *
 * Бизнес-инвариант (ТЗ, раздел 5.3): должен быть заполнен хотя бы один из
 * `telegramUsername` или `maxUsername` — сотрудник по нему свяжется с клиентом.
 * Сумма доставки в предзаказе не учитывается (см. ТЗ 5.4).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePreorderRequest {
    private String customerName;
    private String phone;
    private String email;
    /** Логин в Telegram (без `@`). */
    private String telegramUsername;
    /** Логин в MAX (без `@`). */
    private String maxUsername;
    private String comment;
    private List<OrderItemRequest> items;
}
