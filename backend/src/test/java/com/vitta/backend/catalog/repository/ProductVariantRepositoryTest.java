package com.vitta.backend.catalog.repository;

import java.math.BigDecimal;
import java.util.List;

import com.vitta.backend.TestcontainersConfiguration;
import com.vitta.backend.catalog.entity.Category;
import com.vitta.backend.catalog.entity.Product;
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
 * Cart lookup query against a real MySQL (Testcontainers). Runs only where Docker is available (CI).
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ProductVariantRepositoryTest {

	@Autowired
	private ProductVariantRepository productVariantRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private EntityManager entityManager;

	private Long activeVariantId;

	private Long otherActiveVariantId;

	private Long inactiveVariantId;

	@BeforeEach
	void setUp() {
		Category conjuntos = categoryRepository.save(new Category("Conjuntos", "conjuntos"));
		Product active = new Product("Conjunto Encaje", "conjunto-encaje", null, new BigDecimal("42900.00"), conjuntos,
				null, true);
		ProductVariant small = active.addVariant("S", 4);
		ProductVariant medium = active.addVariant("M", 0);
		productRepository.save(active);
		Product inactive = new Product("Body Inactivo", "body-inactivo", null, new BigDecimal("28900.00"), conjuntos,
				null, false);
		ProductVariant hidden = inactive.addVariant("S", 9);
		productRepository.save(inactive);

		entityManager.flush();
		activeVariantId = small.getId();
		otherActiveVariantId = medium.getId();
		inactiveVariantId = hidden.getId();
		entityManager.clear();
	}

	@Test
	void returnsOnlyVariantsOfActiveProductsWithTheProductFetched() {
		Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
		statistics.clear();

		List<ProductVariant> result = productVariantRepository.findAllWithActiveProductByIdIn(
				List.of(activeVariantId, otherActiveVariantId, inactiveVariantId, Long.MAX_VALUE));

		assertThat(result).extracting(ProductVariant::getId)
			.containsExactlyInAnyOrder(activeVariantId, otherActiveVariantId);
		assertThat(result).extracting((variant) -> variant.getProduct().getSlug())
			.containsOnly("conjunto-encaje");
		assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
	}

}
