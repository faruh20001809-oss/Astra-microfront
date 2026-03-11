package ru.astrakhan.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.astrakhan.admin.entity.SharedUser;
import ru.astrakhan.admin.repository.SharedUserRepository;

@Service
@RequiredArgsConstructor
public class AdminProfileService {

    private final SharedUserRepository sharedUserRepository;
    private final PasswordEncoder passwordEncoder;

    public SharedUser getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return sharedUserRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    @Transactional
    public void changePassword(String currentPassword, String newPassword) {
        SharedUser user = getCurrentUser();
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Неверный текущий пароль");
        }
        if (newPassword == null || newPassword.length() < 4) {
            throw new IllegalArgumentException("Новый пароль должен быть не короче 4 символов");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        sharedUserRepository.save(user);
    }
}
