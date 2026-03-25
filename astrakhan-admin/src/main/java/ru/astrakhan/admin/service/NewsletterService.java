package ru.astrakhan.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.astrakhan.admin.entity.NewsSubscriber;
import ru.astrakhan.admin.repository.NewsSubscriberRepository;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class NewsletterService {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final NewsSubscriberRepository repo;

    @Transactional
    public void subscribe(String email) {
        String e = normalizeEmail(email);
        if (e == null) {
            throw new IllegalArgumentException("Укажите корректный email");
        }
        Optional<NewsSubscriber> existing = repo.findByEmailIgnoreCase(e);
        if (existing.isPresent()) {
            NewsSubscriber s = existing.get();
            s.setActive(true);
            repo.save(s);
            return;
        }
        repo.save(NewsSubscriber.builder().email(e).build());
    }

    @Transactional
    public boolean unsubscribeByToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return repo.findByUnsubscribeToken(token.trim()).map(s -> {
            s.setActive(false);
            repo.save(s);
            return true;
        }).orElse(false);
    }

    public List<NewsSubscriber> findActiveSubscribers() {
        return repo.findByActiveTrue();
    }

    public static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String e = email.trim().toLowerCase();
        if (e.isEmpty() || !EMAIL.matcher(e).matches()) {
            return null;
        }
        return e;
    }
}
