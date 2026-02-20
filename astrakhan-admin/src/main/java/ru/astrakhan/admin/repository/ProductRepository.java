package ru.astrakhan.admin.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.astrakhan.admin.entity.Product;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByPublishedTrue();
    List<Product> findByCategory(String category);
}
