package ru.astrakhan.admin.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Глобальные атрибуты модели для всех Thymeleaf-шаблонов.
 * Автоматически добавляет объект request во все шаблоны.
 */
@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute
    public void addCommonAttributes(Model model, HttpServletRequest request) {
        model.addAttribute("request", request);
    }
}