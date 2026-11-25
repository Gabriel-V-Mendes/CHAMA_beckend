package com.chama.backend.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Gera ou propaga X-Request-Id sanitizado para rastreabilidade em logs e respostas de erro. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

	public static final String HEADER_NAME = "X-Request-Id";
	public static final String MDC_KEY = "request_id";

	// Aceita apenas caracteres alfanuméricos, hífen e underscore para prevenir Log Injection.
	private static final Pattern SAFE_REQUEST_ID = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String requestId = resolveRequestId(request.getHeader(HEADER_NAME));
		MDC.put(MDC_KEY, requestId);
		response.setHeader(HEADER_NAME, requestId);
		try {
			filterChain.doFilter(request, response);
		} finally {
			MDC.remove(MDC_KEY);
		}
	}

	private String resolveRequestId(String headerValue) {
		if (headerValue != null && SAFE_REQUEST_ID.matcher(headerValue).matches()) {
			return headerValue;
		}
		return "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
	}
}
