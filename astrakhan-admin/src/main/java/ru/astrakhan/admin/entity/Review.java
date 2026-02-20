package ru.astrakhan.admin.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "reviews")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "poi_id") private Long poiId;
    @Column(name = "user_id") private String userId;
    @Column(name = "user_name") private String userName;
    private Integer rating;
    @Column(columnDefinition = "TEXT") private String text;
    @Column(name = "visit_date") private String visitDate;
    @Column(name = "helpful_count") @Builder.Default private Integer helpfulCount = 0;
    @Builder.Default private Boolean approved = false;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}
