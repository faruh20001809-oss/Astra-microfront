package ru.astrakhan.admin.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.astrakhan.admin.entity.Order;
import ru.astrakhan.admin.repository.OrderRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service @RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final AtomicLong orderCounter = new AtomicLong(1000);
    public List<Order> findAll() { return orderRepository.findAllByOrderByCreatedAtDesc(); }
    public List<Order> findByStatus(Order.OrderStatus status) { return orderRepository.findByStatus(status); }
    public Optional<Order> findById(Long id) { return orderRepository.findById(id); }
    public Optional<Order> findByOrderId(String orderId) { return orderRepository.findByOrderId(orderId); }
    public Order create(Order order) {

        if (order.getCreatedAt() == null) {
            order.setCreatedAt(LocalDateTime.now());
        }

        return orderRepository.save(order);
    }
    public Order save(Order order) { return orderRepository.save(order); }
    public Order updateStatus(Long id, Order.OrderStatus status) {
        Order o = orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));
        o.setStatus(status); return orderRepository.save(o);
    }
    public List<Object[]> countByStatus() { return orderRepository.countByStatus(); }
    public Double totalRevenue() { Double r = orderRepository.totalRevenue(); return r != null ? r : 0.0; }
}
