package ru.astrakhan.admin.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex, HttpServletRequest request, Model model) {
        String logId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        log.error("Unhandled exception, logId={}", logId, ex);

        model.addAttribute("logId", logId);
        model.addAttribute("timestamp", now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
        model.addAttribute("path", request.getRequestURI());

        return "error";
    }
}

