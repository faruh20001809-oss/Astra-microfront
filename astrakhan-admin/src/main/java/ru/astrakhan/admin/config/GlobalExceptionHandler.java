package ru.astrakhan.admin.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Object handleException(Exception ex, HttpServletRequest request, Model model) {
        String logId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        log.error("Unhandled exception, logId={}", logId, ex);

        String uri = request.getRequestURI();
        if (uri != null && uri.contains("/api/")) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", "error");
            body.put("message", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
            body.put("logId", logId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }

        model.addAttribute("logId", logId);
        model.addAttribute("timestamp", now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
        model.addAttribute("path", uri);

        return "error";
    }
}

