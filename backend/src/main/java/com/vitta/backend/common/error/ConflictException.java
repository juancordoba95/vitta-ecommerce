package com.vitta.backend.common.error;

/**
 * The request is valid but clashes with the current state (duplicate slug or size, image limit
 * reached). Mapped to {@code 409}.
 */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}

}
