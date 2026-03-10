package ru.astrakhan.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.astrakhan.admin.entity.PoiSuggestion;
import ru.astrakhan.admin.service.PoiSuggestionService;

@Controller
@RequestMapping("/admin/poi-suggestions")
@RequiredArgsConstructor
public class PoiSuggestionAdminController {

    private final PoiSuggestionService poiSuggestionService;

    @GetMapping
    public String list(@RequestParam(required = false) String status, Model model, HttpServletRequest request) {
        model.addAttribute("request", request);
        model.addAttribute("suggestions", (status != null && !status.isEmpty())
                ? poiSuggestionService.findByStatus(PoiSuggestion.SuggestionStatus.valueOf(status))
                : poiSuggestionService.findAll());
        model.addAttribute("currentStatus", status);
        model.addAttribute("newCount", poiSuggestionService.countNew());
        return "poi-suggestions/list";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable Long id, Model model, HttpServletRequest request) {
        model.addAttribute("request", request);
        PoiSuggestion s = poiSuggestionService.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        model.addAttribute("suggestion", s);
        return "poi-suggestions/view";
    }

    @PostMapping("/approve/{id}")
    public String approve(@PathVariable Long id, @RequestParam(required = false) String adminNotes, RedirectAttributes ra) {
        poiSuggestionService.approve(id, adminNotes);
        ra.addFlashAttribute("success", "Предложение одобрено");
        return "redirect:/admin/poi-suggestions";
    }

    @PostMapping("/reject/{id}")
    public String reject(@PathVariable Long id, @RequestParam(required = false) String adminNotes, RedirectAttributes ra) {
        poiSuggestionService.reject(id, adminNotes);
        ra.addFlashAttribute("success", "Предложение отклонено");
        return "redirect:/admin/poi-suggestions";
    }
}
