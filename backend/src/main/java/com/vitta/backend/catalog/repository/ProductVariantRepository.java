package com.vitta.backend.catalog.repository;

import java.util.Collection;
import java.util.List;

import com.vitta.backend.catalog.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

	/**
	 * Variants with the given ids whose product is active, with the product fetched in the same
	 * query. Ids of unknown variants or of inactive products are simply absent from the result.
	 */
	@Query("select v from ProductVariant v join fetch v.product p where v.id in :ids and p.active = true")
	List<ProductVariant> findAllWithActiveProductByIdIn(@Param("ids") Collection<Long> ids);

}
