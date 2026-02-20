package ru.astrakhan.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "order")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private Long productId;           // ID продукта (для связи)

    @Column(nullable = false)
    private String productName;       // Название на момент заказа (snapshot)

    @Column(nullable = false)
    private Double price;             // Цена на момент заказа

    @Column(nullable = false)
    private Integer quantity;         // Количество

    @Column
    private String productCategory;   // Категория (для аналитики)

    // Вычисляемое поле (не сохраняется в БД)
    @Transient
    public Double getSubtotal() {
        return price * quantity;
    }
}