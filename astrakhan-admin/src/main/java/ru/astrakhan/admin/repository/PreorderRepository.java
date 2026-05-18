package ru.astrakhan.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.astrakhan.admin.entity.Preorder;

import java.util.List;
import java.util.Optional;

public interface PreorderRepository extends JpaRepository<Preorder, Long> {

    Optional<Preorder> findByPreorderId(String preorderId);

    List<Preorder> findAllByOrderByCreatedAtDesc();

    List<Preorder> findByStatusOrderByCreatedAtDesc(Preorder.PreorderStatus status);

    long countByStatus(Preorder.PreorderStatus status);
}
