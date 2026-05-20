package ru.astrakhan.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "points_of_interest")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PointOfInterest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    @Column(columnDefinition = "TEXT") private String description;
    /** Краткое описание (карточный текст). Fallback: description. */
    @Column(name = "short_description", columnDefinition = "TEXT") private String shortDescription;
    /** Расширенный текст для страницы деталей точки (`/pois/:id`). Может содержать форматирование. */
    @Column(name = "detail_text", columnDefinition = "TEXT") private String detailText;
    /** Прямая ссылка на аудио для встроенного плеера MAX (опционально). */
    @Column(name = "max_audio_url", length = 1024) private String maxAudioUrl;
    /** Прямая ссылка на видео для встроенного плеера MAX (опционально). */
    @Column(name = "max_video_url", length = 1024) private String maxVideoUrl;
    /** Ссылка на сборный плейлист MAX, который точка показывает на странице деталей (опционально). */
    @Column(name = "max_playlist_url", length = 1024) private String maxPlaylistUrl;
    @Column(nullable = false) private String category;
    private Double latitude;
    private Double longitude;
    private String address;
    @Column(name = "image_url") private String imageUrl;
    @Column(name = "image_data", columnDefinition = "bytea") private byte[] imageData;
    @Column(name = "image_filename") private String imageFilename;
    private String phone;
    private String email;
    private String website;
    @Column(name = "working_hours", columnDefinition = "TEXT") private String workingHours;
    @Column(name = "founded_year") private Integer foundedYear;
    private String architect;
    private String material;
    private String style;
    private String tags;
    @Column(nullable = false) @Enumerated(EnumType.STRING)
    @Builder.Default private PoiStatus status = PoiStatus.DRAFT;
    @Builder.Default private Double rating = 0.0;
    @Column(name = "views_count") @Builder.Default private Long viewsCount = 0L;
    @Column(name = "reviews_count") @Builder.Default private Integer reviewsCount = 0;
    @Column(name = "publish_at") private LocalDateTime publishAt;
    @Column(name = "unpublish_at") private LocalDateTime unpublishAt;
    /** Рассылка подписчикам о новой точке уже выполнялась (один раз на ТОИ). */
    @Column(name = "newsletter_notified") @Builder.Default private Boolean newsletterNotified = false;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() {
        createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now();
        if (status == null) status = PoiStatus.DRAFT;
        if (viewsCount == null) viewsCount = 0L;
        if (rating == null) rating = 0.0;
        if (reviewsCount == null) reviewsCount = 0;
        if (newsletterNotified == null) newsletterNotified = false;
    }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
    public enum PoiStatus { DRAFT, PENDING, PUBLISHED, ARCHIVED }
}
