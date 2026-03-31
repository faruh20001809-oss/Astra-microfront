package ru.astrakhan.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.astrakhan.admin.entity.SharedRole;
import ru.astrakhan.admin.entity.SharedUser;
import ru.astrakhan.admin.repository.SharedRoleRepository;
import ru.astrakhan.admin.repository.SharedUserRepository;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SharedUserSyncService {

    private static final String CLIENT_ROLE_NAME = "Клиент";

    private final SharedUserRepository sharedUserRepository;
    private final SharedRoleRepository sharedRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void ensureCustomerByEmail(String emailRaw, String displayNameRaw) {
        String email = normalizeEmail(emailRaw);
        if (email.isBlank() || !email.contains("@")) {
            return;
        }
        String displayName = normalize(displayNameRaw);

        SharedRole clientRole = sharedRoleRepository.findByName(CLIENT_ROLE_NAME)
                .orElseGet(() -> sharedRoleRepository.save(SharedRole.builder().name(CLIENT_ROLE_NAME).build()));

        Optional<SharedUser> byEmail = sharedUserRepository.findByEmailIgnoreCase(email);
        if (byEmail.isPresent()) {
            SharedUser existing = byEmail.get();
            boolean changed = false;
            if (existing.getRoleId() == null || !existing.getRoleId().equals(clientRole.getId())) {
                existing.setRoleId(clientRole.getId());
                changed = true;
            }
            if (!displayName.isBlank() && (existing.getName() == null || existing.getName().isBlank())) {
                existing.setName(displayName);
                changed = true;
            }
            if (changed) {
                sharedUserRepository.save(existing);
            }
            return;
        }

        String baseUsername = email.toLowerCase();
        String username = baseUsername;
        int attempt = 1;
        while (sharedUserRepository.findByUsernameIgnoreCase(username).isPresent()) {
            attempt += 1;
            username = baseUsername + "_client" + attempt;
        }

        SharedUser created = SharedUser.builder()
                .username(username)
                .email(email)
                .name(!displayName.isBlank() ? displayName : username)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .roleId(clientRole.getId())
                .createdAt(LocalDateTime.now())
                .build();
        sharedUserRepository.save(created);
        log.info("shared_client_profile_created email={} username={}", email, username);
    }

    @Transactional
    public Map<String, Object> registerClient(String loginRaw, String emailRaw, String passwordRaw) {
        String username = normalize(loginRaw).toLowerCase();
        String email = normalizeEmail(emailRaw);
        String password = normalize(passwordRaw);
        if (username.length() < 3) throw new IllegalArgumentException("Логин должен содержать минимум 3 символа.");
        if (email.isBlank() || !email.contains("@")) throw new IllegalArgumentException("Укажите корректный email.");
        if (password.length() < 6) throw new IllegalArgumentException("Пароль должен содержать минимум 6 символов.");

        SharedRole clientRole = sharedRoleRepository.findByName(CLIENT_ROLE_NAME)
                .orElseGet(() -> sharedRoleRepository.save(SharedRole.builder().name(CLIENT_ROLE_NAME).build()));

        Optional<SharedUser> byUsername = sharedUserRepository.findByUsernameIgnoreCase(username);
        Optional<SharedUser> byEmail = sharedUserRepository.findByEmailIgnoreCase(email);
        if (byUsername.isPresent() && (byEmail.isEmpty() || !byUsername.get().getId().equals(byEmail.get().getId()))) {
            throw new IllegalArgumentException("Логин уже занят.");
        }

        SharedUser user = byEmail.orElseGet(() -> SharedUser.builder()
                .createdAt(LocalDateTime.now())
                .build());
        if (user.getRoleId() != null && !user.getRoleId().equals(clientRole.getId())) {
            throw new IllegalArgumentException("Этот email уже используется служебным аккаунтом.");
        }
        user.setUsername(username);
        user.setEmail(email);
        user.setName(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoleId(clientRole.getId());
        SharedUser saved = sharedUserRepository.save(user);
        return profilePayload(saved, clientRole.getName());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> loginClient(String loginOrEmailRaw, String passwordRaw) {
        String loginOrEmail = normalize(loginOrEmailRaw).toLowerCase();
        String password = normalize(passwordRaw);
        if (loginOrEmail.isBlank() || password.isBlank()) {
            throw new IllegalArgumentException("Укажите логин/email и пароль.");
        }
        Optional<SharedUser> byUsername = sharedUserRepository.findByUsernameIgnoreCase(loginOrEmail);
        Optional<SharedUser> byEmail = sharedUserRepository.findByEmailIgnoreCase(loginOrEmail);
        SharedUser user = byUsername.or(() -> byEmail)
                .orElseThrow(() -> new IllegalArgumentException("Профиль не найден."));

        String roleName = user.getRole() != null ? user.getRole().getName() : "";
        if (!CLIENT_ROLE_NAME.equalsIgnoreCase(roleName)) {
            throw new IllegalArgumentException("Вход доступен только для клиентского профиля.");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Неверный пароль.");
        }
        return profilePayload(user, roleName);
    }

    private static Map<String, Object> profilePayload(SharedUser user, String roleName) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("email", user.getEmail());
        data.put("name", user.getName());
        data.put("role", roleName);
        data.put("profileType", "client");
        return data;
    }

    private static String normalizeEmail(String value) {
        return normalize(value).toLowerCase();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
