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
import java.util.stream.Collectors;

@Controller @RequestMapping("/admin/products") @RequiredArgsConstructor
public class ProductAdminController {
    private final ProductService productService;
    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) Boolean onlyInStock,
                       Model model) {
        var products = productService.findAll();
        if (q != null && !q.isBlank()) {
            String query = q.toLowerCase();
            products = products.stream()
                    .filter(p ->
                            (p.getName() != null && p.getName().toLowerCase().contains(query)) ||
                            (p.getCategory() != null && p.getCategory().toLowerCase().contains(query)))
                    .collect(Collectors.toList());
        }
        if (Boolean.TRUE.equals(onlyInStock)) {
            products = products.stream()
                    .filter(p -> Boolean.TRUE.equals(p.getInStock()))
                    .collect(Collectors.toList());
        }
        model.addAttribute("products", products);
        model.addAttribute("q", q);
        model.addAttribute("onlyInStock", onlyInStock);
        return "products/list";
    }
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
