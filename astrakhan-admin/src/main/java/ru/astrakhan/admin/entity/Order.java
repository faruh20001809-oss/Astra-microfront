package ru.astrakhan.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// 👇 ДОБАВЬТЕ ЭТИ ИМПОРТЫ:
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Transient;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", unique = true, nullable = false)
    private String orderId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "customer_name")
    private String customerName;

    private String phone;
    private String email;

    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "shipping_method")
    private String shippingMethod;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "items_json", columnDefinition = "TEXT")
    private String itemsJson;

    private Double total;

    @Builder.Default
    private String currency = "RUB";

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrderStatus status = OrderStatus.CONFIRMED;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Column(name = "estimated_delivery")
    private LocalDateTime estimatedDelivery;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = true)
    @Builder.Default
    private Boolean deleted = false;

    /** Внешний ID платежа (платёжный провайдер) */
    @Column(name = "payment_id", length = 100)
    private String paymentId;

    /** Дата и время успешной оплаты */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = OrderStatus.CONFIRMED;
        if (deleted == null) deleted = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum OrderStatus {
        PROCESSING,      // Новый заказ
        CONFIRMED,       // Подтверждён оператором
        SHIPPED,         // Отправлен
        DELIVERED,       // Доставлен
        CANCELLED        // Отменён
    }

    // ===== 🔹 ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ (исправленные) =====

    /**
     * Форматирует адрес доставки из JSON в читаемый вид
     */
    @Transient  // 👈 БЫЛО: Transient (без @) — теперь исправлено!
    public String getFormattedAddress() {
        if (shippingAddress == null || shippingAddress.isEmpty()) return "Не указан";
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> addr = mapper.readValue(shippingAddress, Map.class);
            List<String> parts = new ArrayList<>();

            if (addr.get("city") != null) parts.add(addr.get("city").toString());
            if (addr.get("address") != null) parts.add(addr.get("address").toString());
            if (addr.get("zip") != null) parts.add("инд. " + addr.get("zip"));

            return parts.isEmpty() ? "Не указан" : String.join(", ", parts);
        } catch (Exception e) {
            return shippingAddress; // fallback: показать как есть
        }
    }

    /**
     * Возвращает список товаров из itemsJson
     */
    @Transient
    public List<Map<String, Object>> getItemsList() {
        if (itemsJson == null || itemsJson.isEmpty()) return List.of();
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(itemsJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Рассчитывает итоговую сумму из itemsJson (на случай, если total не установлен)
     */
    @Transient
    public Double calculateTotalFromItems() {
        try {
            if (itemsJson == null || itemsJson.isEmpty()) return 0.0;
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> items = mapper.readValue(itemsJson, new TypeReference<List<Map<String, Object>>>() {});

            return items.stream()
                    .mapToDouble(item -> {
                        Double price = (Double) item.getOrDefault("price", 0.0);
                        Integer qty = (Integer) item.getOrDefault("qty", 1);
                        return price * qty;
                    })
                    .sum();
        } catch (Exception e) {
            return total != null ? total : 0.0;
        }
    }
}