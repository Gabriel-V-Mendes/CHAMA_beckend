package com.chama.backend.modules.auth.dto;

/** Resposta de autenticação contendo tokens e dados básicos do usuário. */
public record AuthResponse(
		String accessToken,
		String refreshToken,
		String tokenType,
		long expiresIn,
		UserSummaryResponse user) {

	public static AuthResponse bearer(
			String accessToken, String refreshToken, long expiresIn, UserSummaryResponse user) {
		return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
	}
}
