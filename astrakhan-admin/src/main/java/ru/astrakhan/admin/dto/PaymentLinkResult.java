package ru.astrakhan.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Результат создания платежа: ссылка для редиректа пользователя на оплату.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentLinkResult {
    /** Внешний ID платежа у провайдера */
    private String paymentId;
    /** URL для перенаправления пользователя (страница оплаты) */
    private String redirectUrl;
    /** Номер заказа */
    private String orderId;
    /** Сумма к оплате (для отображения) */
    private Double amount;
    /** Валюта */
    private String currency;
}
