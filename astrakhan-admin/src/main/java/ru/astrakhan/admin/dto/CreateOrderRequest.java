package ru.astrakhan.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    private String userId;
    private Map<String, Object> shippingAddress;
    private String shippingMethod;
    private String paymentMethod;
    private List<OrderItemRequest> items;
    private String customerComment;
    /** Подписка на рассылку о новых точках на карте (опционально). */
    private Boolean newsletterSubscribe;
}