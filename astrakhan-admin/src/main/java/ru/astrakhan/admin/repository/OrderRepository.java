package ru.astrakhan.admin.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.astrakhan.admin.entity.Order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderId(String orderId);

    List<Order> findAllByDeletedFalseOrderByCreatedAtDesc();
    List<Order> findAllByOrderByCreatedAtDesc();
    List<Order> findByStatusAndDeletedFalseOrderByCreatedAtDesc(Order.OrderStatus status);
    List<Order> findByDeletedFalseAndCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);
    List<Order> findByStatusAndDeletedFalseAndCreatedAtBetweenOrderByCreatedAtDesc(Order.OrderStatus status, LocalDateTime from, LocalDateTime to);

    List<Order> findAllByStatusOrderByCreatedAtDesc(Order.OrderStatus status);
    List<Order> findAllByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);
    List<Order> findAllByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(Order.OrderStatus status, LocalDateTime from, LocalDateTime to);

    @Query("SELECT o.status, COUNT(o) FROM Order o WHERE o.deleted = false GROUP BY o.status")
    List<Object[]> countByStatus();
    @Query("SELECT COALESCE(SUM(o.total),0) FROM Order o WHERE o.status != 'CANCELLED' AND (o.deleted = false OR o.deleted IS NULL)")
    Double totalRevenue();

    @Modifying
    @Query("UPDATE Order o SET o.deleted = false WHERE o.deleted IS NULL")
    void setDeletedFalseWhereNull();
}
