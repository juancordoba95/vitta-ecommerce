package com.vitta.backend.auth.exception;

/**
 * Login failed. Deliberately says nothing about whether the email exists.
 */
public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException() {
		super("Invalid email or password");
	}

}
