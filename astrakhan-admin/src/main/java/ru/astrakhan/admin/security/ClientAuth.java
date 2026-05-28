package ru.astrakhan.admin.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.Optional;

public final class ClientAuth {

    private ClientAuth() {}

    public static Optional<ClientPrincipal> principal(Authentication authentication) {
        if (authentication == null) {
            return Optional.empty();
        }
        Object p = authentication.getPrincipal();
        if (p instanceof ClientPrincipal client) {
            return Optional.of(client);
        }
        return Optional.empty();
    }

    public static String requireEmail(Authentication authentication) {
        return principal(authentication)
                .map(ClientPrincipal::getEmail)
                .orElseThrow(() -> new AccessDeniedException("Требуется вход в профиль."));
    }

    /** Email из JWT для проверки доступа к платному маршруту; без входа — null. */
    public static String emailForAccess(Authentication authentication) {
        return principal(authentication).map(ClientPrincipal::getEmail).orElse(null);
    }
}
