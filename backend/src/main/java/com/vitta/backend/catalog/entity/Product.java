package com.vitta.backend.catalog.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(nullable = false, unique = true, length = 170)
	private String slug;

	@Column(length = 2000)
	private String description;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@Column(name = "image_url", length = 500)
	private String imageUrl;

	@Column(nullable = false)
	private boolean active = true;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id")
	private List<ProductVariant> variants = new ArrayList<>();

	public Product(String name, String slug, String description, BigDecimal price, Category category,
			String imageUrl, boolean active) {
		this.name = name;
		this.slug = slug;
		this.description = description;
		this.price = price;
		this.category = category;
		this.imageUrl = imageUrl;
		this.active = active;
	}

	public ProductVariant addVariant(String size, int stock) {
		ProductVariant variant = new ProductVariant(this, size, stock);
		variants.add(variant);
		return variant;
	}

}
