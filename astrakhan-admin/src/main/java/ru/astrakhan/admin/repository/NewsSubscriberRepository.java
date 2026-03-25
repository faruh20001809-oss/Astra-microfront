package ru.astrakhan.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.astrakhan.admin.entity.NewsSubscriber;

import java.util.List;
import java.util.Optional;

public interface NewsSubscriberRepository extends JpaRepository<NewsSubscriber, Long> {
    Optional<NewsSubscriber> findByEmailIgnoreCase(String email);

    Optional<NewsSubscriber> findByUnsubscribeToken(String token);

    List<NewsSubscriber> findByActiveTrue();
}
