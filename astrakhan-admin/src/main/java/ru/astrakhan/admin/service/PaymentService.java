package ru.astrakhan.admin.service;

import ru.astrakhan.admin.dto.PaymentLinkResult;
import ru.astrakhan.admin.entity.Order;

/**
 * Заготовка под интеграцию с онлайн-кассой / платёжным провайдером
 * (YooKassa, Robokassa, Tinkoff и т.д.).
 */
public interface PaymentService {

    /**
     * Создать платёж для заказа и получить ссылку на страницу оплаты.
     *
     * @param order     заказ
     * @param returnUrl URL возврата после успешной оплаты (на фронте)
     * @param cancelUrl URL возврата при отмене оплаты
     * @return ссылка на оплату или null, если платёж создать нельзя
     */
    PaymentLinkResult createPaymentLink(Order order, String returnUrl, String cancelUrl);

    /**
     * Обработать webhook от платёжного провайдера (успешная оплата / отмена).
     * В реализации: проверка подписи, обновление заказа (paidAt, status).
     *
     * @param rawPayload тело запроса (JSON от провайдера)
     * @return true если webhook обработан успешно
     */
    boolean processWebhook(String rawPayload);

    /**
     * Проверить статус оплаты заказа (опционально — запрос к API провайдера).
     *
     * @param order заказ
     * @return true если заказ уже оплачен (paidAt != null или статус у провайдера success)
     */
    boolean isOrderPaid(Order order);
}
