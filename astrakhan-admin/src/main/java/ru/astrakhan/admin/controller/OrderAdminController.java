package ru.astrakhan.admin.controller;

import jakarta.servlet.http.HttpServletRequest; // 👈 Добавить импорт
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.astrakhan.admin.entity.Order;
import ru.astrakhan.admin.service.OrderService;

@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class OrderAdminController {
    private final OrderService orderService;

    @GetMapping
    public String list(@RequestParam(required = false) String status, Model model, HttpServletRequest request) { // 👈 Добавить параметр
        // 🔹 Явно добавляем request в модель для использования в шаблонах
        model.addAttribute("request", request); // 👈 Ключевая строка!

        model.addAttribute("orders", (status != null && !status.isEmpty())
                ? orderService.findByStatus(Order.OrderStatus.valueOf(status))
                : orderService.findAll());
        model.addAttribute("currentStatus", status);
        model.addAttribute("statusCounts", orderService.countByStatus());
        return "orders/list";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable Long id, Model model, HttpServletRequest request) { // 👈 Добавить параметр
        model.addAttribute("request", request); // 👈 Для шаблона orders/view

        model.addAttribute("order", orderService.findById(id).orElseThrow(() -> new RuntimeException("Not found")));
        return "orders/view";
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
        ra.addFlashAttribute("success", "Статус обновлён!");
        return "redirect:/admin/orders"; // 👈 Redirect, request не нужен
    }
}