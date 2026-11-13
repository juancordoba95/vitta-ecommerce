package com.vitta.backend.common.error;

/**
 * Input that Bean Validation cannot check on its own (file contents, id lists that must match
 * the current state). Mapped to {@code 400}.
 */
public class InvalidRequestException extends RuntimeException {

	public InvalidRequestException(String message) {
		super(message);
	}

}
