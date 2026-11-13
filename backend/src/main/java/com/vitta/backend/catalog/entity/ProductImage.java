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
@Table(name = "product_images",
		uniqueConstraints = @UniqueConstraint(name = "uk_product_images_product_position",
				columnNames = { "product_id", "position" }))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(nullable = false, length = 500)
	private String url;

	/** Identifier in the image storage; {@code null} for external images. */
	@Column(name = "storage_key", length = 300)
	private String storageKey;

	@Column(nullable = false)
	private int position;

	ProductImage(Product product, String url, String storageKey, int position) {
		this.product = product;
		this.url = url;
		this.storageKey = storageKey;
		this.position = position;
	}

	public void moveTo(int position) {
		this.position = position;
	}

}
