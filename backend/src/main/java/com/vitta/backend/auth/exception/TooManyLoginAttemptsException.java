package com.vitta.backend.auth.exception;

import java.time.Duration;

import lombok.Getter;

@Getter
public class TooManyLoginAttemptsException extends RuntimeException {

	private final Duration retryAfter;

	public TooManyLoginAttemptsException(Duration retryAfter) {
		super("Too many failed login attempts. Try again later.");
		this.retryAfter = retryAfter;
	}

}
