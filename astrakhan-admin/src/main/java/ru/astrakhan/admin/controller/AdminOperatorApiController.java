package ru.astrakhan.admin.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.astrakhan.admin.entity.Preorder;
import ru.astrakhan.admin.entity.Route;
import ru.astrakhan.admin.service.PreorderService;
import ru.astrakhan.admin.service.RouteService;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Операторские мутации (только для staff-сессии админки), не публичный /api/v1.
 */
@RestController
@RequestMapping("/admin/api/v1")
@RequiredArgsConstructor
@Slf4j
public class AdminOperatorApiController {

    private final RouteService routeService;
    private final PreorderService preorderService;

    /**
     * Смена статуса маршрута (ACTIVE / DRAFT / OUTDATED).
     * Тело: { "status": "...", "outdatedReason": "..." }
     */
    @PatchMapping("/routes/{id}/status")
    public ResponseEntity<Map<String, Object>> updateRouteStatus(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> b = body != null ? body : Collections.emptyMap();
        String statusStr = b.get("status") instanceof String ? (String) b.get("status") : "";
        String reason = b.get("outdatedReason") instanceof String ? (String) b.get("outdatedReason") : null;
        return routeService.findById(id).map(r -> {
            try {
                Route.RouteStatus newStatus = Route.RouteStatus.valueOf(statusStr);
                r.setStatus(newStatus);
                if (newStatus == Route.RouteStatus.OUTDATED) {
                    r.setOutdatedReason(reason != null ? reason : "Установлено вручную оператором");
                } else {
                    r.setOutdatedReason(null);
                }
                routeService.save(r);
                log.info("Route {} status changed to {} (reason: {})", id, newStatus, reason);
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("id", r.getId());
                data.put("status", r.getStatus().name());
                data.put("outdatedReason", r.getOutdatedReason());
                return okSingle(data);
            } catch (IllegalArgumentException e) {
                return apiError(HttpStatus.BAD_REQUEST,
                        "Недопустимый статус: " + statusStr + ". Допустимы: DRAFT, ACTIVE, OUTDATED.");
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/preorders/{preorderId}/status")
    public ResponseEntity<Map<String, Object>> updatePreorderStatus(
            @PathVariable String preorderId,
            @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> b = body != null ? body : Collections.emptyMap();
        String statusStr = b.get("status") instanceof String ? (String) b.get("status") : "";
        try {
            Preorder.PreorderStatus next = Preorder.PreorderStatus.valueOf(statusStr);
            Preorder saved = preorderService.updateStatus(preorderId, next);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("preorderId", saved.getPreorderId());
            data.put("status", saved.getStatus().name());
            return okSingle(data);
        } catch (IllegalArgumentException e) {
            return apiError(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private static ResponseEntity<Map<String, Object>> okSingle(Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "success");
        body.put("data", data);
        return ResponseEntity.ok(body);
    }

    private static ResponseEntity<Map<String, Object>> apiError(HttpStatus status, String message) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("status", "error");
        err.put("message", message);
        return ResponseEntity.status(status).body(err);
    }
}
