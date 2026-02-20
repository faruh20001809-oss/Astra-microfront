package ru.astrakhan.admin.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.astrakhan.admin.entity.Product;
import ru.astrakhan.admin.repository.ProductRepository;
import java.util.List;
import java.util.Optional;

@Service @RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    public List<Product> findAll() { return productRepository.findAll(); }
    public List<Product> findPublished() { return productRepository.findByPublishedTrue(); }
    public Optional<Product> findById(Long id) { return productRepository.findById(id); }
    public Product save(Product product) { return productRepository.save(product); }
    public void deleteById(Long id) { productRepository.deleteById(id); }
}
