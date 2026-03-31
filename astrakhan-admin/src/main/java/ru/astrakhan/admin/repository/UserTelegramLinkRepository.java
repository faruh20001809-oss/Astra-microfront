package ru.astrakhan.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.astrakhan.admin.entity.UserTelegramLink;

import java.util.Optional;

public interface UserTelegramLinkRepository extends JpaRepository<UserTelegramLink, Long> {
    Optional<UserTelegramLink> findByTelegramChatId(Long telegramChatId);

    Optional<UserTelegramLink> findFirstByEmailAndVerifiedTrue(String email);

    Optional<UserTelegramLink> findFirstByPhoneAndVerifiedTrue(String phone);
}
