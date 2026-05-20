package ru.astrakhan.admin.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.astrakhan.admin.controller.ApiController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Ошибки REST API — всегда JSON, не HTML error.html. */
@Slf4j
@RestControllerAdvice(assignableTypes = ApiController.class)
public class ApiExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handle(Exception ex) {
        String logId = UUID.randomUUID().toString();
        log.error("API error, logId={}", logId, ex);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "error");
        body.put("message", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        body.put("logId", logId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
