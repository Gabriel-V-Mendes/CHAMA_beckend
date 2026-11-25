package com.chama.backend.core.security;

import com.chama.backend.core.exception.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

/** Rate limiter em memória por IP para proteger endpoints públicos de autenticação contra força bruta. */
public class AuthRateLimitFilter extends OncePerRequestFilter {

	private static final long WINDOW_SECONDS = 60L;
	private static final String AUTH_PATH_PREFIX = "/api/v1/auth/";

	private final int maxRequestsPerMinute;
	private final JsonMapper jsonMapper;
	private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

	public AuthRateLimitFilter(int maxRequestsPerMinute, JsonMapper jsonMapper) {
		this.maxRequestsPerMinute = maxRequestsPerMinute;
		this.jsonMapper = jsonMapper;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		if (!HttpMethod.POST.matches(request.getMethod())) {
			return true;
		}
		String path = request.getRequestURI();
		return path == null || !path.startsWith(AUTH_PATH_PREFIX);
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		long nowEpochSecond = Instant.now().getEpochSecond();
		cleanupExpiredEntriesIfNeeded(nowEpochSecond);

		String key = resolveClientIp(request) + ":" + request.getRequestURI();
		WindowCounter counter =
				counters.compute(
						key,
						(k, current) -> {
							if (current == null || nowEpochSecond - current.windowStartEpoch >= WINDOW_SECONDS) {
								return new WindowCounter(nowEpochSecond);
							}
							current.count.incrementAndGet();
							return current;
						});

		if (counter.count.get() > maxRequestsPerMinute) {
			writeTooManyRequests(response);
			return;
		}

		filterChain.doFilter(request, response);
	}

	private void writeTooManyRequests(HttpServletResponse response) throws IOException {
		String requestId = MDC.get(RequestIdFilter.MDC_KEY);
		ApiErrorResponse payload =
				ApiErrorResponse.of(
						"RATE_LIMIT_EXCEEDED",
						"Muitas tentativas em sequência. Aguarde alguns instantes e tente novamente.",
						null,
						requestId != null ? requestId : "req_unknown");

		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setHeader("Retry-After", String.valueOf(WINDOW_SECONDS));
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write(jsonMapper.writeValueAsString(payload));
	}

	private String resolveClientIp(HttpServletRequest request) {
		// Usa o remoteAddr direto para evitar spoofing trivial via X-Forwarded-For sem proxy confiável.
		String remoteAddr = request.getRemoteAddr();
		return remoteAddr != null ? remoteAddr : "unknown";
	}

	private void cleanupExpiredEntriesIfNeeded(long nowEpochSecond) {
		if (counters.size() > 1000) {
			counters.entrySet()
					.removeIf(entry -> nowEpochSecond - entry.getValue().windowStartEpoch >= WINDOW_SECONDS);
		}
	}

	private static final class WindowCounter {
		private final long windowStartEpoch;
		private final AtomicInteger count = new AtomicInteger(1);

		private WindowCounter(long windowStartEpoch) {
			this.windowStartEpoch = windowStartEpoch;
		}
	}
}
