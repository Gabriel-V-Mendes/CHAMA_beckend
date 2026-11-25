package com.chama.backend.core.exception;

import java.util.List;

/** Envelope padrão de erro conforme RFC 7807 simplificado (Spec 02). */
public record ApiErrorResponse(ErrorBody error) {

	public static ApiErrorResponse of(
			String code, String message, List<FieldIssue> details, String requestId) {
		return new ApiErrorResponse(new ErrorBody(code, message, details, requestId));
	}

	/** Corpo detalhado do erro retornado ao cliente. */
	public record ErrorBody(String code, String message, List<FieldIssue> details, String requestId) {}

	/** Detalhe de validação por campo inválido. */
	public record FieldIssue(String field, String issue) {}
}
