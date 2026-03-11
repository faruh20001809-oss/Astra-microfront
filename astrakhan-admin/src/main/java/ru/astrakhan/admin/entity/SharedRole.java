package ru.astrakhan.admin.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Роль из общей таблицы roles (совместно с module3 — админка).
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 2000)
    private String permissions;
}
