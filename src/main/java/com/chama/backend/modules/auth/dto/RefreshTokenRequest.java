package com.chama.backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload para rotação ou revogação explícita de refresh token. */
public record RefreshTokenRequest(
		@NotBlank(message = "Refresh token é obrigatório")
		@Size(max = 256, message = "Refresh token inválido")
		String refreshToken) {
}
