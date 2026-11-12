package com.vitta.backend.cart.dto;

public enum CartItemStatus {

	/** Enough stock for the requested quantity. */
	OK,

	/** Some stock, but less than the requested quantity. */
	INSUFFICIENT_STOCK,

	/** The variant exists but has no stock. */
	OUT_OF_STOCK,

	/** Unknown variant, or its product is inactive. */
	NOT_FOUND

}
