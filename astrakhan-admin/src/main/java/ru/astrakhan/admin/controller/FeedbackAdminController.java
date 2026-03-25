package ru.astrakhan.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.astrakhan.admin.entity.Feedback;
import ru.astrakhan.admin.entity.PointOfInterest;
import ru.astrakhan.admin.repository.ReviewRepository;
import ru.astrakhan.admin.service.FeedbackService;
import ru.astrakhan.admin.service.PoiService;

import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/feedback")
@RequiredArgsConstructor
public class FeedbackAdminController {
    private final FeedbackService feedbackService;
    private final ReviewRepository reviewRepository;
    private final PoiService poiService;

    @GetMapping
    public String list(@RequestParam(required = false) String status,
                       @RequestParam(required = false) String q,
                       @RequestParam(required = false) String from,
                       @RequestParam(required = false) String to,
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
                    .collect(Collectors.toList());
        }

        model.addAttribute("feedbacks", feedbacks);
        model.addAttribute("currentStatus", status);
        model.addAttribute("q", q);
        model.addAttribute("newCount", feedbackService.countNew());

        LocalDate toDate = parseDateOr(to, LocalDate.now());
        LocalDate fromDate = parseDateOr(from, toDate.minusDays(30));
        LocalDateTime fromDt = fromDate.atStartOfDay();
        LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();
        model.addAttribute("periodFrom", fromDate.toString());
        model.addAttribute("periodTo", toDate.toString());
        model.addAttribute("feedbackStatusInPeriod", feedbackService.feedbackStatusCountsInPeriod(fromDt, toDt));
        model.addAttribute("feedbackTopSubjects", feedbackService.topFeedbackSubjectsInPeriod(fromDt, toDt, 8));
        var avgResp = feedbackService.averageResponseHoursInPeriod(fromDt, toDt);
        model.addAttribute("feedbackAvgResponseHoursPresent", avgResp.isPresent());
        model.addAttribute("feedbackAvgResponseHours", avgResp.isPresent() ? avgResp.getAsDouble() : 0.0);
        model.addAttribute("reviewsInPeriod", reviewRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(fromDt, toDt));
        model.addAttribute("reviewRatingDist", reviewRepository.countByRatingInPeriod(fromDt, toDt));
        model.addAttribute("topPoisPendingReviews", reviewRepository.topPoisByPendingReviews(PageRequest.of(0, 10)));
        model.addAttribute("topPoisPendingReviewsDetail", enrichPendingPoiRows(reviewRepository.topPoisByPendingReviews(PageRequest.of(0, 10))));

        return "feedback/list";
    }

    private List<Map<String, Object>> enrichPendingPoiRows(List<Object[]> rows) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (rows == null) {
            return out;
        }
        for (Object[] row : rows) {
            if (row == null || row.length < 2) {
                continue;
            }
            Long pid = row[0] instanceof Long ? (Long) row[0] : Long.valueOf(String.valueOf(row[0]));
            long cnt = row[1] instanceof Long ? (Long) row[1] : Long.parseLong(String.valueOf(row[1]));
            String name = poiService.findById(pid).map(PointOfInterest::getName).orElse("ТОИ #" + pid);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("poiId", pid);
            m.put("name", name);
            m.put("count", cnt);
            out.add(m);
        }
        return out;
    }

    private static LocalDate parseDateOr(String s, LocalDate fallback) {
        if (s == null || s.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(s.trim());
        } catch (Exception e) {
            return fallback;
        }
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