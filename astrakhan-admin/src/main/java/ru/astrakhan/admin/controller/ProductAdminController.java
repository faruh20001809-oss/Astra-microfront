package ru.astrakhan.admin.controller;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.astrakhan.admin.entity.Product;
import ru.astrakhan.admin.service.ProductService;
import java.io.IOException;

@Controller @RequestMapping("/admin/products") @RequiredArgsConstructor
public class ProductAdminController {
    private final ProductService productService;
    @GetMapping public String list(Model model) { model.addAttribute("products", productService.findAll()); return "products/list"; }
    @GetMapping("/create") public String createForm(Model model) { model.addAttribute("product", new Product()); model.addAttribute("isEdit", false); return "products/form"; }
    @GetMapping("/edit/{id}") public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.findById(id).orElseThrow(() -> new RuntimeException("Not found")));
        model.addAttribute("isEdit", true); return "products/form";
    }
    @PostMapping("/save") public String save(@ModelAttribute Product product,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile, RedirectAttributes ra) {
        try {
            if (imageFile != null && !imageFile.isEmpty()) { product.setImageData(imageFile.getBytes()); product.setImageFilename(imageFile.getOriginalFilename()); }
            else if (product.getId() != null) { productService.findById(product.getId()).ifPresent(ex -> { if (product.getImageData()==null) { product.setImageData(ex.getImageData()); product.setImageFilename(ex.getImageFilename()); }}); }
            productService.save(product); ra.addFlashAttribute("success", "Товар сохранён!");
        } catch (IOException e) { ra.addFlashAttribute("error", "Ошибка: " + e.getMessage()); }
        return "redirect:/admin/products";
    }
    @PostMapping("/delete/{id}") public String delete(@PathVariable Long id, RedirectAttributes ra) {
        productService.deleteById(id); ra.addFlashAttribute("success", "Удалено"); return "redirect:/admin/products";
    }
}
