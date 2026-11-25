package com.chama.backend.core.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Configurações tipadas e validadas de segurança da aplicação (app.security.*). */
@Validated
@ConfigurationProperties(prefix = "app.security")
public record AppSecurityProperties(
		@NotBlank(message = "JWT_SECRET é obrigatório")
		@Size(min = 32, message = "JWT_SECRET deve ter no mínimo 32 caracteres (256 bits)")
		String jwtSecret,

		@NotBlank
		String jwtIssuer,

		@Positive
		long accessTokenTtlSeconds,

		@Positive
		long refreshTokenTtlSeconds,

		@Min(value = 12, message = "BCRYPT_STRENGTH deve ser >= 12 conforme Spec 04")
		int bcryptStrength,

		@NotEmpty
		List<String> corsAllowedOrigins,

		@Positive
		int authRateLimitPerMinute) {
}
