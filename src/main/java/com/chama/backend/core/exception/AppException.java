package com.chama.backend.core.exception;

import org.springframework.http.HttpStatus;

/** Exceção base de regras de negócio e segurança mapeada para código HTTP controlado. */
public class AppException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	public AppException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}

	public static AppException conflict(String code, String message) {
		return new AppException(HttpStatus.CONFLICT, code, message);
	}

	public static AppException unauthorized(String code, String message) {
		return new AppException(HttpStatus.UNAUTHORIZED, code, message);
	}

	public static AppException unprocessable(String code, String message) {
		return new AppException(HttpStatus.UNPROCESSABLE_CONTENT, code, message);
	}

	public static AppException notFound(String code, String message) {
		return new AppException(HttpStatus.NOT_FOUND, code, message);
	}

	public static AppException tooManyRequests(String message) {
		return new AppException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", message);
	}
}
