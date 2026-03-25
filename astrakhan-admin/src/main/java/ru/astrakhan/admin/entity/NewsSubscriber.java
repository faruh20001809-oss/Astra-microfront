package ru.astrakhan.admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "news_subscribers", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsSubscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "subscribed_at")
    private LocalDateTime subscribedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "unsubscribe_token", nullable = false, unique = true)
    private String unsubscribeToken;

    @PrePersist
    void onCreate() {
        if (subscribedAt == null) {
            subscribedAt = LocalDateTime.now();
        }
        if (active == null) {
            active = true;
        }
        if (unsubscribeToken == null || unsubscribeToken.isBlank()) {
            unsubscribeToken = java.util.UUID.randomUUID().toString().replace("-", "");
        }
    }
}
