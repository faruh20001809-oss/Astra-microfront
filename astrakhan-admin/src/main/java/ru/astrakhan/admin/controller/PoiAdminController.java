package ru.astrakhan.admin.controller;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.astrakhan.admin.entity.PoiSuggestion;
import ru.astrakhan.admin.entity.PointOfInterest;
import ru.astrakhan.admin.entity.Route;
import ru.astrakhan.admin.repository.ReviewRepository;
import ru.astrakhan.admin.service.PoiPublicationNotificationService;
import ru.astrakhan.admin.service.PoiService;
import ru.astrakhan.admin.service.PoiSuggestionService;
import ru.astrakhan.admin.service.RouteService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller @RequestMapping("/admin/poi") @RequiredArgsConstructor
public class PoiAdminController {
    private final PoiService poiService;
    private final PoiSuggestionService poiSuggestionService;
    private final RouteService routeService;
    private final ReviewRepository reviewRepository;
    private final ObjectMapper objectMapper;
    private final PoiPublicationNotificationService publicationNotificationService;

    @Value("${app.ai.enabled:false}")
    private boolean poiAiEnabled;

    @Value("${app.dgis.map-key:2fa2df2d-9b29-4877-ac65-818e02de807d}")
    private String dgisMapKey;

    @GetMapping public String list(@RequestParam(required = false) String status, Model model) {
        List<PointOfInterest> pois = (status != null && !status.isEmpty()) ?
            poiService.findByStatus(PointOfInterest.PoiStatus.valueOf(status)) : poiService.findAll();
        model.addAttribute("pois", pois);
        model.addAttribute("currentStatus", status);
        return "poi/list";
    }

    @GetMapping("/create") public String createForm(@RequestParam(required = false) Long fromSuggestion,
            Model model,
            org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        model.addAttribute("isEdit", false);
        model.addAttribute("poiAiEnabled", poiAiEnabled);
        model.addAttribute("dgisMapKey", dgisMapKey);
        if (fromSuggestion == null) {
            model.addAttribute("poi", new PointOfInterest());
            model.addAttribute("fromSuggestionId", null);
            return "poi/form";
        }
        PoiSuggestion s = poiSuggestionService.findById(fromSuggestion).orElse(null);
        if (s == null) {
            ra.addFlashAttribute("error", "Предложение не найдено");
            return "redirect:/admin/poi-suggestions";
        }
        if (s.getImportedPoiId() != null) {
            ra.addFlashAttribute("error", "Из этого предложения уже создана точка интереса (POI #" + s.getImportedPoiId() + ").");
            return "redirect:/admin/poi-suggestions/view/" + fromSuggestion;
        }
        if (s.getStatus() != PoiSuggestion.SuggestionStatus.APPROVED) {
            ra.addFlashAttribute("error", "Сначала одобрите предложение.");
            return "redirect:/admin/poi-suggestions/view/" + fromSuggestion;
        }
        PointOfInterest poi = new PointOfInterest();
        poi.setName(s.getName());
        poi.setAddress(s.getPlace());
        poi.setDescription(s.getDescription());
        poi.setCategory("Прочее");
        if (s.getLatitude() != null) poi.setLatitude(s.getLatitude());
        if (s.getLongitude() != null) poi.setLongitude(s.getLongitude());
        model.addAttribute("poi", poi);
        model.addAttribute("fromSuggestionId", s.getId());
        return "poi/form";
    }

    @GetMapping("/edit/{id}") public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("poi", poiService.findById(id).orElseThrow(() -> new RuntimeException("Not found")));
        model.addAttribute("isEdit", true);
        model.addAttribute("poiAiEnabled", poiAiEnabled);
        model.addAttribute("dgisMapKey", dgisMapKey);
        model.addAttribute("fromSuggestionId", null);
        return "poi/form";
    }

    @PostMapping("/save") public String save(@ModelAttribute PointOfInterest poi,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "fromSuggestionId", required = false) Long fromSuggestionId,
            RedirectAttributes ra) {
        try {
            Long prevId = poi.getId();
            if (imageFile != null && !imageFile.isEmpty()) {
                poi.setImageData(imageFile.getBytes()); poi.setImageFilename(imageFile.getOriginalFilename());
            } else if (poi.getId() != null) {
                poiService.findById(poi.getId()).ifPresent(ex -> {
                    if (poi.getImageData() == null) { poi.setImageData(ex.getImageData()); poi.setImageFilename(ex.getImageFilename()); }
                });
            }
            PointOfInterest saved = poiService.save(poi);
            if (fromSuggestionId != null && prevId == null) {
                try {
                    poiSuggestionService.markImportedFromPoi(fromSuggestionId, saved.getId());
                } catch (Exception ex) {
                    ra.addFlashAttribute("error", "Точка сохранена, но не удалось связать с предложением: " + ex.getMessage());
                    return "redirect:/admin/poi";
                }
            }
            ra.addFlashAttribute("success", "Точка интереса сохранена!");
        } catch (IOException e) { ra.addFlashAttribute("error", "Ошибка: " + e.getMessage()); }
        return "redirect:/admin/poi";
    }

    @PostMapping("/publish/{id}") public String publish(@PathVariable Long id, RedirectAttributes ra) {
        poiService.publish(id); ra.addFlashAttribute("success", "Точка опубликована! JSON отправлен в Модуль 2."); return "redirect:/admin/poi";
    }

    /** Повторная ручная рассылка подписчикам о точке (кнопка в списке). */
    @PostMapping("/broadcast-newsletter/{id}") public String broadcastNewsletter(@PathVariable Long id, RedirectAttributes ra) {
        return poiService.findById(id).map(poi -> {
            publicationNotificationService.sendNewPoiToSubscribersAsync(poi);
            ra.addFlashAttribute("success", "Рассылка подписчикам поставлена в очередь.");
            return "redirect:/admin/poi";
        }).orElseGet(() -> {
            ra.addFlashAttribute("error", "Точка не найдена");
            return "redirect:/admin/poi";
        });
    }
    @PostMapping("/archive/{id}") public String archive(@PathVariable Long id, RedirectAttributes ra) {
        poiService.archive(id); ra.addFlashAttribute("success", "Точка архивирована"); return "redirect:/admin/poi";
    }
    @PostMapping("/delete/{id}") public String delete(@PathVariable Long id, RedirectAttributes ra) {
        poiService.deleteById(id); ra.addFlashAttribute("success", "Удалено"); return "redirect:/admin/poi";
    }

    @GetMapping("/reviews") public String reviews(Model model) {
        model.addAttribute("pendingReviews", reviewRepository.findByApprovedFalse());
        model.addAttribute("pendingCount", reviewRepository.countByApprovedFalse());
        return "poi/reviews";
    }
    @PostMapping("/reviews/approve/{id}") public String approveReview(@PathVariable Long id, RedirectAttributes ra) {
        reviewRepository.findById(id).ifPresent(r -> { r.setApproved(true); reviewRepository.save(r); });
        ra.addFlashAttribute("success", "Отзыв одобрен"); return "redirect:/admin/poi/reviews";
    }
    @PostMapping("/reviews/reject/{id}") public String rejectReview(@PathVariable Long id, RedirectAttributes ra) {
        reviewRepository.deleteById(id); ra.addFlashAttribute("success", "Отзыв отклонён"); return "redirect:/admin/poi/reviews";
    }

    @GetMapping("/image/{id}") @ResponseBody
    public ResponseEntity<byte[]> getImage(@PathVariable Long id) {
        return poiService.findById(id).filter(p -> p.getImageData() != null)
            .map(p -> ResponseEntity.ok().header("Content-Type", "image/jpeg").body(p.getImageData()))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/bulk")
    public String bulkForm(Model model) {
        return "poi/bulk";
    }

    @PostMapping("/bulk")
    public String bulkUpload(@RequestParam("json") String json, RedirectAttributes ra) {
        if (json == null || json.isBlank()) {
            ra.addFlashAttribute("error", "Вставьте JSON");
            return "redirect:/admin/poi/bulk";
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode poisNode = root.get("pois");
            List<Long> createdPoiIds = new ArrayList<>();
            if (poisNode != null && poisNode.isArray()) {
                for (JsonNode p : poisNode) {
                    String name = p.has("name") ? p.get("name").asText().trim() : null;
                    String category = p.has("category") ? p.get("category").asText().trim() : "Прочее";
                    if (name == null || name.isEmpty()) continue;
                    PointOfInterest poi = new PointOfInterest();
                    poi.setName(name);
                    poi.setCategory(category);
                    poi.setDescription(p.has("description") ? p.get("description").asText(null) : null);
                    poi.setAddress(p.has("address") ? p.get("address").asText(null) : null);
                    if (p.has("latitude")) poi.setLatitude(p.get("latitude").asDouble());
                    if (p.has("longitude")) poi.setLongitude(p.get("longitude").asDouble());
                    if (p.has("foundedYear")) poi.setFoundedYear(p.get("foundedYear").asInt());
                    if (p.has("architect")) poi.setArchitect(p.get("architect").asText(null));
                    poi.setStatus(PointOfInterest.PoiStatus.DRAFT);
                    poi.setImageData(null);
                    poi.setImageFilename(null);
                    createdPoiIds.add(poiService.save(poi).getId());
                }
            }
            JsonNode routesNode = root.get("routes");
            if (routesNode != null && routesNode.isArray() && !createdPoiIds.isEmpty()) {
                for (JsonNode r : routesNode) {
                    String name = r.has("name") ? r.get("name").asText().trim() : null;
                    if (name == null || name.isEmpty()) continue;
                    Route route = new Route();
                    route.setName(name);
                    route.setDescription(r.has("description") ? r.get("description").asText(null) : null);
                    route.setCategory(r.has("category") ? r.get("category").asText("Маршрут") : "Маршрут");
                    route.setPublished(false);
                    route.setPaid(false);
                    route.setPrice(0.0);
                    if (r.has("poiOrder") && r.get("poiOrder").isArray()) {
                        List<String> ids = new ArrayList<>();
                        for (JsonNode idx : r.get("poiOrder")) {
                            int i = idx.asInt(-1);
                            if (i >= 0 && i < createdPoiIds.size()) ids.add(createdPoiIds.get(i).toString());
                        }
                        route.setPoiIds(String.join(",", ids));
                    }
                    routeService.save(route);
                }
            }
            int routesCount = (routesNode != null && routesNode.isArray()) ? routesNode.size() : 0;
            ra.addFlashAttribute("success", "Создано точек: " + createdPoiIds.size() + (routesCount > 0 ? ", маршрутов: " + routesCount : "") + ". Добавьте картинки в карточках точек.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Ошибка разбора JSON: " + e.getMessage());
            return "redirect:/admin/poi/bulk";
        }
        return "redirect:/admin/poi";
    }
}
