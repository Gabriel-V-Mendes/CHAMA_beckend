package com.chama.backend.core.security;

import com.chama.backend.core.config.AppSecurityProperties;
import com.chama.backend.core.exception.ApiErrorResponse;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.json.JsonMapper;

/** Configuração central de segurança HTTP, CORS, headers OWASP, BCrypt e JWT stateless. */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	private final AppSecurityProperties properties;
	private final JsonMapper jsonMapper;

	public SecurityConfig(AppSecurityProperties properties, JsonMapper jsonMapper) {
		this.properties = properties;
		this.jsonMapper = jsonMapper;
	}

	/** Cadeia principal de filtros HTTP stateless, CORS, headers OWASP e regras de autorização. */
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		AuthRateLimitFilter rateLimitFilter =
				new AuthRateLimitFilter(properties.authRateLimitPerMinute(), jsonMapper);

		return http
				.csrf(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.sessionManagement(
						session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.headers(
						headers ->
								headers
										.contentTypeOptions(options -> {})
										.frameOptions(FrameOptionsConfig::deny)
										.httpStrictTransportSecurity(
												hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
										.contentSecurityPolicy(
												csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'")))
				.authorizeHttpRequests(
						auth ->
								auth.requestMatchers(
												HttpMethod.POST,
												"/api/v1/auth/register",
												"/api/v1/auth/login",
												"/api/v1/auth/refresh",
												"/api/v1/auth/logout")
										.permitAll()
										.requestMatchers(
												HttpMethod.GET,
												"/health/live",
												"/health/ready",
												"/api/health",
												"/actuator/health")
										.permitAll()
										.anyRequest()
										.authenticated())
				.oauth2ResourceServer(
						oauth2 ->
								oauth2
										.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
										.authenticationEntryPoint(
												(req, res, ex) ->
														writeSecurityError(
																res,
																HttpStatus.UNAUTHORIZED,
																"UNAUTHORIZED",
																"Autenticação ausente, inválida ou expirada.")))
				.exceptionHandling(
						ex ->
								ex.authenticationEntryPoint(
												(req, res, authEx) ->
														writeSecurityError(
																res,
																HttpStatus.UNAUTHORIZED,
																"UNAUTHORIZED",
																"Autenticação ausente, inválida ou expirada."))
										.accessDeniedHandler(
												(req, res, deniedEx) ->
														writeSecurityError(
																res,
																HttpStatus.FORBIDDEN,
																"FORBIDDEN",
																"Você não tem permissão para acessar este recurso.")))
				.addFilterBefore(rateLimitFilter, BearerTokenAuthenticationFilter.class)
				.build();
	}

	/** Codificador de senhas BCrypt com custo configurado (>= 12 conforme Spec 04). */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(properties.bcryptStrength());
	}

	/** Assina JWTs de acesso usando chave simétrica HMAC-SHA256. */
	@Bean
	public JwtEncoder jwtEncoder() {
		return new NimbusJwtEncoder(new ImmutableSecret<>(hmacSecretKey()));
	}

	/** Valida assinatura e expiração dos JWTs recebidos no header Bearer. */
	@Bean
	public JwtDecoder jwtDecoder() {
		return NimbusJwtDecoder.withSecretKey(hmacSecretKey()).macAlgorithm(MacAlgorithm.HS256).build();
	}

	/** Converte a claim 'role' do JWT para autoridade Spring Security (ROLE_*). */
	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(
				jwt -> {
					String role = jwt.getClaimAsString("role");
					if (role == null || role.isBlank()) {
						return List.of();
					}
					return List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT)));
				});
		return converter;
	}

	/** Restringe CORS apenas às origens explícitas configuradas em CORS_ALLOWED_ORIGINS. */
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(properties.corsAllowedOrigins());
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id"));
		config.setExposedHeaders(List.of("X-Request-Id"));
		config.setAllowCredentials(false);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	private SecretKey hmacSecretKey() {
		byte[] secretBytes = properties.jwtSecret().getBytes(StandardCharsets.UTF_8);
		return new SecretKeySpec(secretBytes, "HmacSHA256");
	}

	private void writeSecurityError(
			HttpServletResponse response, HttpStatus status, String code, String message)
			throws IOException {
		String requestId = MDC.get(RequestIdFilter.MDC_KEY);
		ApiErrorResponse payload =
				ApiErrorResponse.of(code, message, null, requestId != null ? requestId : "req_unknown");
		response.setStatus(status.value());
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write(jsonMapper.writeValueAsString(payload));
	}
}
