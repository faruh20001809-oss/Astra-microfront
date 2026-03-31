package ru.astrakhan.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.astrakhan.admin.entity.UserProgressProfile;

import java.util.Optional;

public interface UserProgressProfileRepository extends JpaRepository<UserProgressProfile, Long> {
    Optional<UserProgressProfile> findByEmailIgnoreCase(String email);
}
