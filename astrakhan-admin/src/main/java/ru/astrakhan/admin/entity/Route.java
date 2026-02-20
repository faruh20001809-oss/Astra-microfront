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
    @Lob @Column(name = "image_data") private byte[] imageData;
    @Column(name = "image_filename") private String imageFilename;
    private Double distance;
    private Integer duration;
    private String difficulty;
    @Builder.Default private Boolean published = false;
    @Builder.Default private Boolean paid = false;
    @Builder.Default private Double price = 0.0;
    @Builder.Default private Double rating = 0.0;
    @Column(name = "poi_ids") private String poiIds;
    @Column(name = "waypoints", columnDefinition = "TEXT") private String waypoints;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
