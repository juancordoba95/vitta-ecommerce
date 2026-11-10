package com.vitta.backend.common.error;

public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String resource, String identifier) {
		super("%s '%s' not found".formatted(resource, identifier));
	}

}
