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

    private static String normalizeEmail(String value) {
        return normalize(value).toLowerCase();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
