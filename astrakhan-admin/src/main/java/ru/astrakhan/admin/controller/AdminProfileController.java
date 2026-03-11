package ru.astrakhan.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.astrakhan.admin.entity.SharedUser;
import ru.astrakhan.admin.service.AdminProfileService;

@Controller
@RequestMapping("/admin/profile")
@RequiredArgsConstructor
public class AdminProfileController {

    private final AdminProfileService adminProfileService;

    @GetMapping
    public String profile(Model model, HttpServletRequest request) {
        model.addAttribute("request", request);
        SharedUser user = adminProfileService.getCurrentUser();
        model.addAttribute("user", user);
        return "profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes ra) {
        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Пароли не совпадают");
            return "redirect:/admin/profile";
        }
        try {
            adminProfileService.changePassword(currentPassword, newPassword);
            ra.addFlashAttribute("success", "Пароль успешно изменён.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/profile";
    }
}
