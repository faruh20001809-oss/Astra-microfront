package ru.astrakhan.admin.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Редирект управления пользователями в админку (module3).
 * Создание и редактирование пользователей — только в админке.
 */
@Controller
@RequestMapping("/admin/users")
public class AdminRedirectController {

    @Value("${app.admin.url:https://152665.ip-ptr.tech/admin}")
    private String adminUrl;

    @GetMapping
    public String list() {
        return "redirect:" + adminUrl + "/users";
    }

    @GetMapping("/new")
    public String newUser() {
        return "redirect:" + adminUrl + "/users";
    }

    @GetMapping("/edit/{id}")
    public String edit() {
        return "redirect:" + adminUrl + "/users";
    }
}
