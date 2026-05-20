package ru.astrakhan.admin.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String name;
    @Column(columnDefinition = "TEXT") private String description;
    private String category;
    @Column(nullable = false) private Double price;
    @Builder.Default private String currency = "RUB";
    @Column(name = "image_url") private String imageUrl;
    @Column(name = "image_data", columnDefinition = "bytea") private byte[] imageData;
    @Column(name = "image_filename") private String imageFilename;
    @Builder.Default private Double rating = 0.0;
    @Column(name = "reviews_count") @Builder.Default private Integer reviewsCount = 0;
    @Column(name = "in_stock") @Builder.Default private Boolean inStock = true;
    @Builder.Default private Integer quantity = 0;
    private String sizes;
    private String colors;
    private String material;
    private String weight;
    @Column(columnDefinition = "TEXT") private String details;
    @Builder.Default private Boolean published = true;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
