package com.vitta.backend.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_variants",
		uniqueConstraints = @UniqueConstraint(name = "uk_product_variants_product_size",
				columnNames = { "product_id", "size" }))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductVariant {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(nullable = false, length = 10)
	private String size;

	@Column(nullable = false)
	private int stock;

	ProductVariant(Product product, String size, int stock) {
		this.product = product;
		this.size = size;
		this.stock = stock;
	}

}
