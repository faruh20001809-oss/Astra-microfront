package ru.astrakhan.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.astrakhan.admin.entity.AdminUser;
import ru.astrakhan.admin.service.AdminUserService;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public String list(Model model, HttpServletRequest request) {
        model.addAttribute("request", request);
        model.addAttribute("users", adminUserService.findAll());
        model.addAttribute("currentUsername", adminUserService.getCurrentUsername());
        return "users/list";
    }

    @GetMapping("/new")
    public String newForm(Model model, HttpServletRequest request) {
        model.addAttribute("request", request);
        return "users/form";
    }

    @PostMapping
    public String create(@RequestParam String username,
                         @RequestParam String password,
                         @RequestParam(required = false) String role,
                         RedirectAttributes ra) {
        if (username == null || username.isBlank()) {
            ra.addFlashAttribute("error", "Логин не может быть пустым");
            return "redirect:/admin/users/new";
        }
        if (password == null || password.isBlank()) {
            ra.addFlashAttribute("error", "Пароль не может быть пустым");
            return "redirect:/admin/users/new";
        }
        AdminUser created = adminUserService.create(username, password, role);
        if (created == null) {
            ra.addFlashAttribute("error", "Пользователь с таким логином уже существует");
            return "redirect:/admin/users/new";
        }
        ra.addFlashAttribute("success", "Пользователь «" + created.getUsername() + "» создан");
        return "redirect:/admin/users";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model, HttpServletRequest request) {
        model.addAttribute("request", request);
        AdminUser user = adminUserService.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", user);
        model.addAttribute("currentUsername", adminUserService.getCurrentUsername());
        return "users/edit";
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam(required = false) String newPassword,
                         @RequestParam(required = false) String role,
                         RedirectAttributes ra) {
        boolean ok = adminUserService.update(id, newPassword, role);
        if (!ok) {
            ra.addFlashAttribute("error", "Пользователь не найден");
            return "redirect:/admin/users";
        }
        ra.addFlashAttribute("success", "Пользователь обновлён");
        return "redirect:/admin/users";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        boolean deleted = adminUserService.delete(id);
        if (!deleted) {
            ra.addFlashAttribute("error", "Нельзя удалить последнего пользователя");
        } else {
            ra.addFlashAttribute("success", "Пользователь удалён");
        }
        return "redirect:/admin/users";
    }
}
