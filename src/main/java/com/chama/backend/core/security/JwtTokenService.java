package com.chama.backend.core.security;

import com.chama.backend.core.config.AppSecurityProperties;
import com.chama.backend.modules.user.model.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Emite Access Tokens JWT (HS256) e Refresh Tokens opacos com hash SHA-256. */
@Service
public class JwtTokenService {

	private static final int REFRESH_TOKEN_BYTES = 32;

	private final JwtEncoder jwtEncoder;
	private final AppSecurityProperties properties;
	private final SecureRandom secureRandom = new SecureRandom();

	public JwtTokenService(JwtEncoder jwtEncoder, AppSecurityProperties properties) {
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
	}

	/** Gera Access Token JWT assinado com expiração curta (Spec 04). */
	public String generateAccessToken(User user, Instant issuedAt) {
		Instant expiresAt = issuedAt.plusSeconds(properties.accessTokenTtlSeconds());
		JwtClaimsSet claims =
				JwtClaimsSet.builder()
						.issuer(properties.jwtIssuer())
						.issuedAt(issuedAt)
						.expiresAt(expiresAt)
						.subject(user.getId().toString())
						.claim("role", user.getRole().getValue())
						.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	/** Gera token opaco aleatório de 256 bits para fluxo de refresh rotacionável. */
	public String generateOpaqueRefreshToken() {
		byte[] randomBytes = new byte[REFRESH_TOKEN_BYTES];
		secureRandom.nextBytes(randomBytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
	}

	/** Calcula hash SHA-256 hexadecimal para armazenar refresh token sem texto puro no banco. */
	public String hashToken(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("Algoritmo SHA-256 indisponível na JVM", ex);
		}
	}

	public long getAccessTokenTtlSeconds() {
		return properties.accessTokenTtlSeconds();
	}

	public long getRefreshTokenTtlSeconds() {
		return properties.refreshTokenTtlSeconds();
	}
}
