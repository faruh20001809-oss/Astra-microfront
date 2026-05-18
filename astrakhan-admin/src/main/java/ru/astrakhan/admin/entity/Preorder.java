package ru.astrakhan.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Предзаказ из публичной корзины модуля 2.
 *
 * Бизнес-сценарий: пользователь формирует корзину и оставляет контакты
 * (telegramUsername / maxUsername). Сотрудник обрабатывает заявку вручную:
 * связывается с клиентом, подтверждает позиции, согласует доставку.
 *
 * Жизненный цикл: {@link PreorderStatus#NEW_PREORDER} -> IN_PROCESS -> CONFIRMED | REJECTED.
 */
@Entity
@Table(name = "preorders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Preorder {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Внешний человекочитаемый идентификатор предзаказа (например, PRE-AB12CD34). */
    @Column(name = "preorder_id", unique = true, nullable = false, length = 40)
    private String preorderId;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(length = 64)
    private String phone;

    @Column(length = 200)
    private String email;

    /** Логин в Telegram без `@`. Один из (telegramUsername, maxUsername) обязателен. */
    @Column(name = "telegram_username", length = 64)
    private String telegramUsername;

    /** Логин в MAX без `@`. Один из (telegramUsername, maxUsername) обязателен. */
    @Column(name = "max_username", length = 64)
    private String maxUsername;

    @Column(columnDefinition = "TEXT")
    private String comment;

    /** JSON-массив позиций: [{ id, name, price, qty, category, variant }]. */
    @Column(name = "items_json", columnDefinition = "TEXT", nullable = false)
    private String itemsJson;

    /** Сумма позиций на момент создания (без учета доставки — она убрана из расчета). */
    private Double total;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PreorderStatus status = PreorderStatus.NEW_PREORDER;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = PreorderStatus.NEW_PREORDER;
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum PreorderStatus {
        /** Новая заявка, ждет сотрудника. */
        NEW_PREORDER,
        /** Сотрудник связался с клиентом, согласует позиции/доставку. */
        IN_PROCESS,
        /** Предзаказ подтвержден и переведен в работу. */
        CONFIRMED,
        /** Отклонен (клиент не выходит на связь, нет позиций и т.п.). */
        REJECTED
    }
}
