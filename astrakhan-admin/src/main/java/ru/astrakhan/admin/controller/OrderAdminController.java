package ru.astrakhan.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.astrakhan.admin.entity.Order;
import ru.astrakhan.admin.service.OrderService;
import ru.astrakhan.admin.service.OrderEmailService;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class OrderAdminController {
    private final OrderService orderService;
    private final OrderEmailService orderEmailService;

    @GetMapping
    public String list(@RequestParam(required = false) String status,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                       @RequestParam(required = false) Boolean showArchived,
                       @RequestParam(required = false) String q,
                       Model model, HttpServletRequest request) {
        model.addAttribute("request", request);
        List<Order> orders = orderService.findFiltered(showArchived, status != null && !status.isEmpty() ? Order.OrderStatus.valueOf(status) : null, dateFrom, dateTo);

        if (q != null && !q.isBlank()) {
            String query = q.toLowerCase();
            orders = orders.stream()
                    .filter(o ->
                            (o.getOrderId() != null && o.getOrderId().toLowerCase().contains(query)) ||
                            (o.getCustomerName() != null && o.getCustomerName().toLowerCase().contains(query)) ||
                            (o.getEmail() != null && o.getEmail().toLowerCase().contains(query)) ||
                            (o.getPhone() != null && o.getPhone().toLowerCase().contains(query)))
                    .collect(Collectors.toList());
        }
        model.addAttribute("orders", orders);
        model.addAttribute("currentStatus", status);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("showArchived", Boolean.TRUE.equals(showArchived));
        model.addAttribute("statusCounts", orderService.countByStatus());
        model.addAttribute("q", q);
        return "orders/list";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable Long id, Model model, HttpServletRequest request) { // 👈 Добавить параметр
        model.addAttribute("request", request); // 👈 Для шаблона orders/view

        model.addAttribute("order", orderService.findById(id).orElseThrow(() -> new RuntimeException("Not found")));
        return "orders/view";
    }

    @PostMapping("/archive/{id}")
    public String archive(@PathVariable Long id, RedirectAttributes ra) {
        orderService.archive(id);
        ra.addFlashAttribute("success", "Заказ перемещён в архив.");
        return "redirect:/admin/orders";
    }

    @PostMapping("/status/{id}")
    public String updateStatus(@PathVariable Long id, @RequestParam String status,
                               @RequestParam(required = false) String trackingNumber,
                               @RequestParam(required = false) String adminNotes, RedirectAttributes ra) {
        Order order = orderService.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        order.setStatus(Order.OrderStatus.valueOf(status));
        if (trackingNumber != null && !trackingNumber.isEmpty()) order.setTrackingNumber(trackingNumber);
        if (adminNotes != null && !adminNotes.isEmpty()) order.setAdminNotes(adminNotes);
        orderService.save(order);

        if (order.getEmail() != null && !order.getEmail().isBlank()
                && trackingNumber != null && !trackingNumber.isEmpty()) {
            orderEmailService.sendTrackingUpdate(order);
        }

        ra.addFlashAttribute("success", "Статус обновлён!");
        return "redirect:/admin/orders";
    }

    @PostMapping("/send-confirmation/{id}")
    public String sendConfirmation(@PathVariable Long id, RedirectAttributes ra) {
        Order order = orderService.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        orderEmailService.sendOrderConfirmation(order);
        ra.addFlashAttribute("success", "Письмо с информацией о заказе отправлено на " + order.getEmail());
        return "redirect:/admin/orders/view/" + id;
    }

    @PostMapping("/send-tracking/{id}")
    public String sendTracking(@PathVariable Long id, RedirectAttributes ra) {
        Order order = orderService.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        orderEmailService.sendTrackingUpdate(order);
        ra.addFlashAttribute("success", "Письмо с трек-номером отправлено на " + order.getEmail());
        return "redirect:/admin/orders/view/" + id;
    }
}