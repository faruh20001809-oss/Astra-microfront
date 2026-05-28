package ru.astrakhan.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.astrakhan.admin.entity.SharedUser;
import ru.astrakhan.admin.repository.SharedUserRepository;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Загружает пользователей из общей таблицы users (БД shared с module3).
 * Роль из таблицы roles маппится в ROLE_* для Spring Security.
 */
@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

    private static final String CLIENT_ROLE_NAME = "Клиент";

    private final SharedUserRepository sharedUserRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return sharedUserRepository.findByUsernameIgnoreCase(username)
                .map(this::toUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    private UserDetails toUserDetails(SharedUser u) {
        String roleName = u.getRole() != null ? u.getRole().getName() : null;
        if (CLIENT_ROLE_NAME.equalsIgnoreCase(roleName != null ? roleName.trim() : "")) {
            throw new UsernameNotFoundException("Клиентские аккаунты входят через профиль на сайте.");
        }
        String authority = roleNameToAuthority(roleName);
        return new User(
                u.getUsername(),
                u.getPassword(),
                true,
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority(authority))
        );
    }

    /** Маппинг имён ролей (админка) в Spring Security authority. */
    private static String roleNameToAuthority(String roleName) {
        if (roleName == null || roleName.isBlank()) return "ROLE_USER";
        String r = roleName.trim();
        if (r.equals("Администратор")) return "ROLE_ADMIN";
        if (r.equals("Сотрудник")) return "ROLE_USER";
        if (r.equals("Модератор")) return "ROLE_MODERATOR";
        if (r.equals("Редактор")) return "ROLE_EDITOR";
        return "ROLE_" + r.toUpperCase(Locale.ROOT).replace(" ", "_");
    }
}
