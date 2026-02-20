package ru.astrakhan.admin.controller;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.astrakhan.admin.entity.PointOfInterest;
import ru.astrakhan.admin.repository.ReviewRepository;
import ru.astrakhan.admin.service.PoiService;
import java.io.IOException;
import java.util.List;

@Controller @RequestMapping("/admin/poi") @RequiredArgsConstructor
public class PoiAdminController {
    private final PoiService poiService;
    private final ReviewRepository reviewRepository;

    @GetMapping public String list(@RequestParam(required = false) String status, Model model) {
        List<PointOfInterest> pois = (status != null && !status.isEmpty()) ?
            poiService.findByStatus(PointOfInterest.PoiStatus.valueOf(status)) : poiService.findAll();
        model.addAttribute("pois", pois);
        model.addAttribute("currentStatus", status);
        return "poi/list";
    }

    @GetMapping("/create") public String createForm(Model model) {
        model.addAttribute("poi", new PointOfInterest()); model.addAttribute("isEdit", false); return "poi/form";
    }

    @GetMapping("/edit/{id}") public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("poi", poiService.findById(id).orElseThrow(() -> new RuntimeException("Not found")));
        model.addAttribute("isEdit", true); return "poi/form";
    }

    @PostMapping("/save") public String save(@ModelAttribute PointOfInterest poi,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes ra) {
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                poi.setImageData(imageFile.getBytes()); poi.setImageFilename(imageFile.getOriginalFilename());
            } else if (poi.getId() != null) {
                poiService.findById(poi.getId()).ifPresent(ex -> {
                    if (poi.getImageData() == null) { poi.setImageData(ex.getImageData()); poi.setImageFilename(ex.getImageFilename()); }
                });
            }
            poiService.save(poi); ra.addFlashAttribute("success", "Точка интереса сохранена!");
        } catch (IOException e) { ra.addFlashAttribute("error", "Ошибка: " + e.getMessage()); }
        return "redirect:/admin/poi";
    }

    @PostMapping("/publish/{id}") public String publish(@PathVariable Long id, RedirectAttributes ra) {
        poiService.publish(id); ra.addFlashAttribute("success", "Точка опубликована! JSON отправлен в Модуль 2."); return "redirect:/admin/poi";
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
}
