package com.vitta.backend.catalog.repository;

import java.math.BigDecimal;
import java.util.Optional;

import com.vitta.backend.TestcontainersConfiguration;
import com.vitta.backend.catalog.entity.Category;
import com.vitta.backend.catalog.entity.Product;
import com.vitta.backend.catalog.entity.ProductImage;
import com.vitta.backend.catalog.entity.ProductVariant;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Detail query against a real MySQL (Testcontainers, schema from Flyway). Runs only where Docker
 * is available (CI).
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ProductRepositoryTest {

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private EntityManager entityManager;

	private Statistics statistics;

	@BeforeEach
	void setUp() {
		Category conjuntos = categoryRepository.save(new Category("Conjuntos", "conjuntos"));

		Product encaje = new Product("Conjunto Encaje", "conjunto-encaje", "Encaje negro", new BigDecimal("42900.00"),
				conjuntos, "https://img/1.jpg", true);
		encaje.addVariant("S", 4);
		encaje.addVariant("M", 0);
		encaje.addVariant("L", 3);
		encaje.addImage("https://img/1.jpg", 0);
		encaje.addImage("https://img/2.jpg", 1);
		productRepository.save(encaje);
		productRepository.save(new Product("Body Inactivo", "body-inactivo", null, new BigDecimal("28900.00"),
				conjuntos, null, false));

		entityManager.flush();
		entityManager.clear();
		statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
		statistics.clear();
	}

	@Test
	void findsActiveProductWithCategoryVariantsAndImagesInASingleQuery() {
		Optional<Product> result = productRepository.findBySlugAndActiveTrue("conjunto-encaje");

		assertThat(result).isPresent();
		Product product = result.get();
		// Touch every association: none of them may trigger an extra query (no N+1).
		assertThat(product.getCategory().getSlug()).isEqualTo("conjuntos");
		assertThat(product.getVariants()).extracting(ProductVariant::getSize)
			.containsExactlyInAnyOrder("S", "M", "L");
		assertThat(product.getImages()).extracting(ProductImage::getUrl)
			.containsExactlyInAnyOrder("https://img/1.jpg", "https://img/2.jpg");
		assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
	}

	@Test
	void doesNotFindInactiveProduct() {
		assertThat(productRepository.findBySlugAndActiveTrue("body-inactivo")).isEmpty();
	}

	@Test
	void doesNotFindUnknownProduct() {
		assertThat(productRepository.findBySlugAndActiveTrue("does-not-exist")).isEmpty();
	}

}
