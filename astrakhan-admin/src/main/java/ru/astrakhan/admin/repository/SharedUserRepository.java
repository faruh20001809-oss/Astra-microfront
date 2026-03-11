package ru.astrakhan.admin.repository;

import ru.astrakhan.admin.entity.SharedUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SharedUserRepository extends JpaRepository<SharedUser, Long> {

    Optional<SharedUser> findByUsernameIgnoreCase(String username);
}
