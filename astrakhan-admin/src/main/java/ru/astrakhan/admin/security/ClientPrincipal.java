package ru.astrakhan.admin.security;

import lombok.Getter;

/**
 * Аутентифицированный клиент витрины (JWT Bearer).
 */
@Getter
public final class ClientPrincipal {

    private final Long userId;
    private final String email;
    private final String username;

    public ClientPrincipal(Long userId, String email, String username) {
        this.userId = userId;
        this.email = email;
        this.username = username;
    }
}
