package com.chama.backend.modules.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Locale;

/** Payload de entrada para cadastro de novo estudante (com minimização LGPD). */
public record RegisterRequest(
		@NotBlank(message = "E-mail é obrigatório")
		@Email(message = "Formato de e-mail inválido")
		@Size(max = 254, message = "E-mail deve ter no máximo 254 caracteres")
		String email,

		@NotBlank(message = "Senha é obrigatória")
		@Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
		@Pattern(
				regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
				message = "A senha deve conter letras maiúsculas, minúsculas, número e caractere especial")
		String password,

		@NotBlank(message = "Nome completo é obrigatório")
		@Size(min = 2, max = 150, message = "Nome completo deve ter entre 2 e 150 caracteres")
		String fullName,

		@NotNull(message = "Data de nascimento é obrigatória")
		@Past(message = "Data de nascimento deve estar no passado")
		LocalDate birthDate,

		@NotBlank(message = "Cidade é obrigatória")
		@Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres")
		String city,

		@Size(max = 100, message = "Bairro deve ter no máximo 100 caracteres")
		String neighborhood,

		@NotNull(message = "Aceite dos termos é obrigatório")
		@AssertTrue(message = "É obrigatório aceitar os Termos de Uso e Política de Privacidade")
		Boolean termsAccepted,

		@NotBlank(message = "Versão dos termos é obrigatória")
		@Size(max = 32, message = "Versão dos termos deve ter no máximo 32 caracteres")
		String termsVersion,

		Boolean guardianConsent) {

	public RegisterRequest {
		// Normaliza texto nas bordas antes da validação para evitar falhas com espaços acidentais.
		email = email != null ? email.trim().toLowerCase(Locale.ROOT) : null;
		fullName = fullName != null ? fullName.trim() : null;
		city = city != null ? city.trim() : null;
		neighborhood = neighborhood != null && !neighborhood.isBlank() ? neighborhood.trim() : null;
		termsVersion = termsVersion != null ? termsVersion.trim() : null;
	}
}
