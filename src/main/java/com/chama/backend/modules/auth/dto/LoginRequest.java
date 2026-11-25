package com.chama.backend.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

/** Payload de entrada para autenticação de usuário existente. */
public record LoginRequest(
		@NotBlank(message = "E-mail é obrigatório")
		@Email(message = "Formato de e-mail inválido")
		@Size(max = 254, message = "E-mail deve ter no máximo 254 caracteres")
		String email,

		@NotBlank(message = "Senha é obrigatória")
		@Size(min = 1, max = 72, message = "Senha deve ter no máximo 72 caracteres")
		String password) {

	public LoginRequest {
		// Remove espaços e padroniza caixa para busca consistente no login.
		email = email != null ? email.trim().toLowerCase(Locale.ROOT) : null;
	}
}
