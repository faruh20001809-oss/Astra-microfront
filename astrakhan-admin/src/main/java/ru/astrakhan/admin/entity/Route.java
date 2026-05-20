package ru.astrakhan.admin.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "routes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Route {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String name;
    @Column(columnDefinition = "TEXT") private String description;
    private String category;
    @Column(name = "image_url") private String imageUrl;
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image_data", columnDefinition = "bytea") private byte[] imageData;
    @Column(name = "image_filename") private String imageFilename;
    private Double distance;
    private Integer duration;
    private String difficulty;
    @Builder.Default private Boolean published = false;
    @Builder.Default private Boolean paid = false;
    @Builder.Default private Double price = 0.0;
    @Builder.Default private Double rating = 0.0;
    /** Чем выше — тем выше маршрут в публичной выдаче. По умолчанию 0. */
    @Column(nullable = false) @Builder.Default private Integer priority = 0;
    /**
     * Жизненный цикл маршрута:
     *  - DRAFT — черновик, не виден гостям;
     *  - ACTIVE — все обязательные точки валидны, маршрут публикуется;
     *  - OUTDATED — обнаружены проблемы (см. {@link #outdatedReason}), маршрут скрывается из публичной выдачи.
     */
    @Column(nullable = false) @Enumerated(EnumType.STRING)
    @Builder.Default private RouteStatus status = RouteStatus.DRAFT;
    /** Человекочитаемая причина деактулизации (для админки и логов). */
    @Column(name = "outdated_reason", columnDefinition = "TEXT") private String outdatedReason;
    @Column(name = "poi_ids") private String poiIds;
    @Column(name = "waypoints", columnDefinition = "TEXT") private String waypoints;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() {
        createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now();
        if (priority == null) priority = 0;
        if (status == null) status = RouteStatus.DRAFT;
    }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum RouteStatus { DRAFT, ACTIVE, OUTDATED }
}
