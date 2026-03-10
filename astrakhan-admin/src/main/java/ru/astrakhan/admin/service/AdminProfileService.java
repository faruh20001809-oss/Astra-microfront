package ru.astrakhan.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.astrakhan.admin.entity.AdminUser;
import ru.astrakhan.admin.repository.AdminUserRepository;

@Service
@RequiredArgsConstructor
public class AdminProfileService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUser getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    @Transactional
    public void changePassword(String currentPassword, String newPassword) {
        AdminUser user = getCurrentUser();
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Неверный текущий пароль");
        }
        if (newPassword == null || newPassword.length() < 4) {
            throw new IllegalArgumentException("Новый пароль должен быть не короче 4 символов");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        adminUserRepository.save(user);
    }
}
