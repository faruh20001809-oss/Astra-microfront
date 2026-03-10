package ru.astrakhan.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.astrakhan.admin.entity.AdminUser;
import ru.astrakhan.admin.repository.AdminUserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    public List<AdminUser> findAll() {
        return adminUserRepository.findAll();
    }

    public java.util.Optional<AdminUser> findById(Long id) {
        return adminUserRepository.findById(id);
    }

    /**
     * Create new admin user. Password is encoded.
     * @return created user or null if username already exists
     */
    @Transactional
    public AdminUser create(String username, String password, String role) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return null;
        }
        if (adminUserRepository.existsByUsername(username.trim())) {
            return null;
        }
        String roleVal = (role != null && !role.isBlank()) ? role.trim() : "ADMIN";
        return adminUserRepository.save(AdminUser.builder()
                .username(username.trim())
                .passwordHash(passwordEncoder.encode(password))
                .role(roleVal)
                .build());
    }

    /**
     * Update password and/or role. If newPassword is blank, password is not changed.
     */
    @Transactional
    public boolean update(Long id, String newPassword, String role) {
        AdminUser user = adminUserRepository.findById(id).orElse(null);
        if (user == null) return false;
        if (newPassword != null && !newPassword.isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(newPassword));
        }
        if (role != null && !role.isBlank()) {
            user.setRole(role.trim());
        }
        adminUserRepository.save(user);
        return true;
    }

    /**
     * Delete user. Fails if this is the last admin (at least one must remain).
     */
    @Transactional
    public boolean delete(Long id) {
        if (adminUserRepository.count() <= 1) {
            return false;
        }
        adminUserRepository.deleteById(id);
        return true;
    }

    public boolean existsByUsername(String username) {
        return username != null && !username.isBlank() && adminUserRepository.existsByUsername(username.trim());
    }

    /** Current logged-in username, or null. */
    public String getCurrentUsername() {
        Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (p instanceof org.springframework.security.core.userdetails.UserDetails) {
            return ((org.springframework.security.core.userdetails.UserDetails) p).getUsername();
        }
        return null;
    }
}
