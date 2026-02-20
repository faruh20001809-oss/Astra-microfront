package ru.astrakhan.admin.controller;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.astrakhan.admin.entity.Route;
import ru.astrakhan.admin.service.PoiService;
import ru.astrakhan.admin.service.RouteService;
import java.io.IOException;

@Controller @RequestMapping("/admin/routes") @RequiredArgsConstructor
public class RouteAdminController {
    private final RouteService routeService;
    private final PoiService poiService;

    @GetMapping public String list(Model model) { model.addAttribute("routes", routeService.findAll()); return "routes/list"; }

    @GetMapping("/create") public String createForm(Model model) {
        model.addAttribute("route", new Route()); model.addAttribute("isEdit", false);
        model.addAttribute("allPois", poiService.findPublished()); return "routes/form";
    }
    @GetMapping("/edit/{id}") public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("route", routeService.findById(id).orElseThrow(() -> new RuntimeException("Not found")));
        model.addAttribute("isEdit", true); model.addAttribute("allPois", poiService.findPublished()); return "routes/form";
    }
    @PostMapping("/save") public String save(@ModelAttribute Route route,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "selectedPois", required = false) String selectedPois, RedirectAttributes ra) {
        try {
            if (imageFile != null && !imageFile.isEmpty()) { route.setImageData(imageFile.getBytes()); route.setImageFilename(imageFile.getOriginalFilename()); }
            else if (route.getId() != null) { routeService.findById(route.getId()).ifPresent(ex -> { if (route.getImageData()==null) { route.setImageData(ex.getImageData()); route.setImageFilename(ex.getImageFilename()); }}); }
            if (selectedPois != null) route.setPoiIds(selectedPois);
            routeService.save(route); ra.addFlashAttribute("success", "Маршрут сохранён!");
        } catch (IOException e) { ra.addFlashAttribute("error", "Ошибка: " + e.getMessage()); }
        return "redirect:/admin/routes";
    }
    @PostMapping("/publish/{id}") public String publish(@PathVariable Long id, RedirectAttributes ra) {
        routeService.publish(id); ra.addFlashAttribute("success", "Маршрут опубликован!"); return "redirect:/admin/routes";
    }
    @PostMapping("/unpublish/{id}") public String unpublish(@PathVariable Long id, RedirectAttributes ra) {
        routeService.unpublish(id); ra.addFlashAttribute("success", "Снят с публикации"); return "redirect:/admin/routes";
    }
    @PostMapping("/delete/{id}") public String delete(@PathVariable Long id, RedirectAttributes ra) {
        routeService.deleteById(id); ra.addFlashAttribute("success", "Удалено"); return "redirect:/admin/routes";
    }
}
