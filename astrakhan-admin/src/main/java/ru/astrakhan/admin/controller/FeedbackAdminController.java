package ru.astrakhan.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.astrakhan.admin.entity.Feedback;
import ru.astrakhan.admin.service.FeedbackService;

@Controller
@RequestMapping("/admin/feedback")
@RequiredArgsConstructor
public class FeedbackAdminController {
    private final FeedbackService feedbackService;

    @GetMapping
    public String list(@RequestParam(required = false) String status,
                       @RequestParam(required = false) String q,
                       Model model, HttpServletRequest request) {
        // 🔹 Явно добавляем request в модель для использования в шаблонах
        model.addAttribute("request", request);

        var feedbacks = (status != null && !status.isEmpty())
                ? feedbackService.findByStatus(Feedback.FeedbackStatus.valueOf(status))
                : feedbackService.findAll();

        if (q != null && !q.isBlank()) {
            String query = q.toLowerCase();
            feedbacks = feedbacks.stream()
                    .filter(f ->
                            (f.getName() != null && f.getName().toLowerCase().contains(query)) ||
                            (f.getEmail() != null && f.getEmail().toLowerCase().contains(query)) ||
                            (f.getSubject() != null && f.getSubject().toLowerCase().contains(query)))
                    .toList();
        }

        model.addAttribute("feedbacks", feedbacks);
        model.addAttribute("currentStatus", status);
        model.addAttribute("q", q);
        model.addAttribute("newCount", feedbackService.countNew());
        return "feedback/list";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable Long id, Model model, HttpServletRequest request) {
        model.addAttribute("request", request);

        Feedback fb = feedbackService.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        if (fb.getStatus() == Feedback.FeedbackStatus.NEW) feedbackService.markAsRead(id);
        model.addAttribute("feedback", fb);
        return "feedback/view";
    }

    @PostMapping("/respond/{id}")
    public String respond(@PathVariable Long id, @RequestParam String response, RedirectAttributes ra) {
        feedbackService.respond(id, response);
        ra.addFlashAttribute("success", "Ответ отправлен!");
        return "redirect:/admin/feedback";
    }

    @PostMapping("/archive/{id}")
    public String archive(@PathVariable Long id, RedirectAttributes ra) {
        Feedback fb = feedbackService.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        fb.setStatus(Feedback.FeedbackStatus.ARCHIVED);
        feedbackService.save(fb);
        ra.addFlashAttribute("success", "Архивировано");
        return "redirect:/admin/feedback";
    }
}