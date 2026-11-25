package com.chama.backend.core.exception;

import com.chama.backend.core.exception.ApiErrorResponse.FieldIssue;
import com.chama.backend.core.security.RequestIdFilter;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Intercepta exceções da API e devolve erros padronizados sem vazar stack trace ou PII. */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/** Trata falhas de Bean Validation (@Valid) retornando 422 com campos em snake_case. */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		List<FieldIssue> details =
				ex.getBindingResult().getFieldErrors().stream().map(this::toFieldIssue).toList();
		return buildResponse(
				HttpStatus.UNPROCESSABLE_CONTENT,
				"VALIDATION_ERROR",
				"Os dados enviados são inválidos.",
				details);
	}

	/** Trata JSON malformado ou campos desconhecidos rejeitados pelo Jackson (400). */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
		// Oculta detalhes de deserialização para não expor classes internas (anti-Mass Assignment).
		return buildResponse(
				HttpStatus.BAD_REQUEST,
				"MALFORMED_REQUEST",
				"Corpo da requisição inválido ou com campos não permitidos.",
				List.of());
	}

	/** Trata exceções de regra de negócio mapeando status e código controlado. */
	@ExceptionHandler(AppException.class)
	public ResponseEntity<ApiErrorResponse> handleAppException(AppException ex) {
		return buildResponse(ex.getStatus(), ex.getCode(), ex.getMessage(), List.of());
	}

	/** Trata acessos negados por falta de permissão (RBAC) retornando 403. */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
		return buildResponse(
				HttpStatus.FORBIDDEN,
				"FORBIDDEN",
				"Você não tem permissão para acessar este recurso.",
				List.of());
	}

	/** Fallback para erros inesperados retornando 500 com request_id para auditoria. */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
		// Registra apenas request_id e classe da exceção para evitar vazamento de PII em logs.
		log.error(
				"Erro interno não tratado [request_id={}, exception={}]",
				currentRequestId(),
				ex.getClass().getSimpleName());
		return buildResponse(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"INTERNAL_ERROR",
				"Erro interno inesperado. Tente novamente mais tarde.",
				List.of());
	}

	private ResponseEntity<ApiErrorResponse> buildResponse(
			HttpStatus status, String code, String message, List<FieldIssue> details) {
		List<FieldIssue> safeDetails = details.isEmpty() ? null : details;
		return ResponseEntity.status(status)
				.body(ApiErrorResponse.of(code, message, safeDetails, currentRequestId()));
	}

	private FieldIssue toFieldIssue(FieldError error) {
		String issue =
				error.getDefaultMessage() != null ? error.getDefaultMessage() : "Valor inválido";
		return new FieldIssue(toSnakeCase(error.getField()), issue);
	}

	private String toSnakeCase(String field) {
		return field.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
	}

	private String currentRequestId() {
		String requestId = MDC.get(RequestIdFilter.MDC_KEY);
		return requestId != null ? requestId : "req_unknown";
	}
}
