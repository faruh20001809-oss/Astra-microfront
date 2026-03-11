package ru.astrakhan.admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Пользователь из общей таблицы users (совместно с module3 — админка).
 * Пароль хранится в BCrypt для совместимости с Spring Security и Flask.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(length = 100)
    private String name;

    @Column(length = 120)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "role_id")
    private Long roleId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", insertable = false, updatable = false)
    private SharedRole role;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;
}
