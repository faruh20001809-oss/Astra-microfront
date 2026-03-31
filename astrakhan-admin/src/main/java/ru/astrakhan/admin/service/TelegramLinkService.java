package ru.astrakhan.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.astrakhan.admin.entity.TelegramLinkToken;
import ru.astrakhan.admin.entity.UserTelegramLink;
import ru.astrakhan.admin.repository.TelegramLinkTokenRepository;
import ru.astrakhan.admin.repository.UserTelegramLinkRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TelegramLinkService {

    private final TelegramLinkTokenRepository tokenRepository;
    private final UserTelegramLinkRepository linkRepository;

    @Value("${app.telegram.bot-username:}")
    private String botUsername;

    @Value("${app.telegram.link-ttl-sec:900}")
    private long linkTtlSec;

    public Map<String, Object> requestLink(String emailRaw, String phoneRaw) {
        String email = normalize(emailRaw);
        String phone = normalize(phoneRaw);
        if (email.isEmpty() && phone.isEmpty()) {
            throw new IllegalArgumentException("Укажите email или телефон для привязки Telegram.");
        }
        if (botUsername == null || botUsername.isBlank()) {
            throw new IllegalStateException("Не задано имя бота (app.telegram.bot-username).");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();
        TelegramLinkToken row = TelegramLinkToken.builder()
                .tokenHash(hashToken(token))
                .email(email.isEmpty() ? null : email)
                .phone(phone.isEmpty() ? null : phone)
                .expiresAt(now.plusSeconds(Math.max(linkTtlSec, 60)))
                .createdAt(now)
                .build();
        tokenRepository.save(row);

        String deepLink = "https://t.me/" + botUsername.trim() + "?start=link_" + token;
        return Map.of(
                "botDeepLink", deepLink,
                "expiresInSec", Math.max(linkTtlSec, 60)
        );
    }

    public Map<String, Object> status(String emailRaw, String phoneRaw) {
        String email = normalize(emailRaw);
        String phone = normalize(phoneRaw);
        Optional<UserTelegramLink> link = findLink(email, phone);
        if (link.isEmpty()) {
            return Map.of("linked", false);
        }
        UserTelegramLink l = link.get();
        return Map.of(
                "linked", true,
                "chatId", l.getTelegramChatId(),
                "username", l.getTelegramUsername() != null ? l.getTelegramUsername() : ""
        );
    }

    @Transactional
    public Map<String, Object> confirmLink(String token, Long chatId, String username) {
        if (token == null || token.isBlank() || chatId == null) {
            throw new IllegalArgumentException("Требуются token и chatId.");
        }
        String tokenHash = hashToken(token.trim());
        TelegramLinkToken row = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Токен не найден."));
        LocalDateTime now = LocalDateTime.now();
        if (row.getUsedAt() != null) {
            throw new IllegalArgumentException("Токен уже использован.");
        }
        if (row.getExpiresAt() != null && !row.getExpiresAt().isAfter(now)) {
            throw new IllegalArgumentException("Токен истёк.");
        }

        UserTelegramLink link = linkRepository.findByTelegramChatId(chatId).orElseGet(UserTelegramLink::new);
        link.setTelegramChatId(chatId);
        link.setTelegramUsername(normalize(username));
        if (row.getEmail() != null && !row.getEmail().isBlank()) {
            link.setEmail(row.getEmail());
        }
        if (row.getPhone() != null && !row.getPhone().isBlank()) {
            link.setPhone(row.getPhone());
        }
        link.setVerified(true);
        if (link.getLinkedAt() == null) {
            link.setLinkedAt(now);
        }
        link.setLastUsedAt(now);
        linkRepository.save(link);

        row.setUsedAt(now);
        tokenRepository.save(row);
        return Map.of(
                "linked", true,
                "chatId", chatId
        );
    }

    @Transactional
    public void touchByOrderContact(String emailRaw, String phoneRaw) {
        findLink(normalize(emailRaw), normalize(phoneRaw)).ifPresent(link -> {
            link.setLastUsedAt(LocalDateTime.now());
            linkRepository.save(link);
        });
    }

    public Optional<UserTelegramLink> findLink(String emailRaw, String phoneRaw) {
        String email = normalize(emailRaw);
        String phone = normalize(phoneRaw);
        if (!email.isEmpty()) {
            Optional<UserTelegramLink> byEmail = linkRepository.findFirstByEmailAndVerifiedTrue(email);
            if (byEmail.isPresent()) {
                return byEmail;
            }
        }
        if (!phone.isEmpty()) {
            return linkRepository.findFirstByPhoneAndVerifiedTrue(phone);
        }
        return Optional.empty();
    }

    private static String normalize(String v) {
        return v == null ? "" : v.trim();
    }

    private static String hashToken(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Token hashing error", e);
        }
    }
}
