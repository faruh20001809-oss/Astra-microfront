package ru.astrakhan.admin.repository;

import ru.astrakhan.admin.entity.SharedRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SharedRoleRepository extends JpaRepository<SharedRole, Long> {

    Optional<SharedRole> findByName(String name);
}
