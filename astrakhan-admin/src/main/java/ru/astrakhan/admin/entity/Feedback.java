package ru.astrakhan.admin.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "feedback")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Feedback {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String email;
    private String phone;
    private String subject;
    @Column(columnDefinition = "TEXT", nullable = false) private String message;
    @Column(nullable = false) @Enumerated(EnumType.STRING) @Builder.Default private FeedbackStatus status = FeedbackStatus.NEW;
    @Column(name = "admin_response", columnDefinition = "TEXT") private String adminResponse;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "responded_at") private LocalDateTime respondedAt;
    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); if(status==null)status=FeedbackStatus.NEW; }
    public enum FeedbackStatus { NEW, READ, RESPONDED, ARCHIVED }
}
