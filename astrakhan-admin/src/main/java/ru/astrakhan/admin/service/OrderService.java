package ru.astrakhan.admin.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.astrakhan.admin.entity.Order;
import ru.astrakhan.admin.repository.OrderRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service @RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;

    public List<Order> findAll() {
        return orderRepository.findAllByDeletedFalseOrderByCreatedAtDesc();
    }

    public List<Order> findAllIncludingArchived() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Order> findByStatus(Order.OrderStatus status) {
        return orderRepository.findByStatusAndDeletedFalseOrderByCreatedAtDesc(status);
    }

    public List<Order> findFiltered(Boolean showArchived, Order.OrderStatus status, LocalDate dateFrom, LocalDate dateTo) {
        LocalDateTime from = dateFrom != null ? dateFrom.atStartOfDay() : null;
        LocalDateTime to = dateTo != null ? dateTo.atTime(LocalTime.MAX) : null;
        boolean hasDateRange = from != null && to != null;

        if (Boolean.TRUE.equals(showArchived)) {
            if (status != null && !status.name().isEmpty() && hasDateRange) {
                return orderRepository.findAllByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(status, from, to);
            }
            if (hasDateRange) {
                return orderRepository.findAllByCreatedAtBetweenOrderByCreatedAtDesc(from, to);
            }
            if (status != null && !status.name().isEmpty()) {
                return orderRepository.findAllByStatusOrderByCreatedAtDesc(status);
            }
            return orderRepository.findAllByOrderByCreatedAtDesc();
        }

        if (status != null && !status.name().isEmpty() && hasDateRange) {
            return orderRepository.findByStatusAndDeletedFalseAndCreatedAtBetweenOrderByCreatedAtDesc(status, from, to);
        }
        if (hasDateRange) {
            return orderRepository.findByDeletedFalseAndCreatedAtBetweenOrderByCreatedAtDesc(from, to);
        }
        if (status != null && !status.name().isEmpty()) {
            return orderRepository.findByStatusAndDeletedFalseOrderByCreatedAtDesc(status);
        }
        return orderRepository.findAllByDeletedFalseOrderByCreatedAtDesc();
    }

    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }
    public Optional<Order> findByOrderId(String orderId) {
        return orderRepository.findByOrderId(orderId);
    }
    public Order create(Order order) {
        if (order.getCreatedAt() == null) {
            order.setCreatedAt(LocalDateTime.now());
        }
        return orderRepository.save(order);
    }
    public Order save(Order order) {
        return orderRepository.save(order);
    }
    public Order updateStatus(Long id, Order.OrderStatus status) {
        Order o = orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));
        o.setStatus(status);
        return orderRepository.save(o);
    }

    public void archive(Long id) {
        Order o = orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));
        o.setDeleted(true);
        orderRepository.save(o);
    }

    public List<Object[]> countByStatus() {
        return orderRepository.countByStatus();
    }
    public Double totalRevenue() {
        Double r = orderRepository.totalRevenue();
        return r != null ? r : 0.0;
    }
}
