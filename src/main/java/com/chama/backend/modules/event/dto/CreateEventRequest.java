package com.chama.backend.modules.event.dto;

import com.chama.backend.modules.event.model.EventCategory;
import com.chama.backend.modules.event.model.EventModality;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Locale;

public record CreateEventRequest(
		@NotBlank(message = "Título é obrigatório")
		@Size(max = 150, message = "Título deve ter no máximo 150 caracteres")
		String title,

		@NotBlank(message = "Descrição é obrigatória")
		String description,

		@NotNull(message = "Categoria é obrigatória")
		EventCategory category,

		@NotNull(message = "Modalidade é obrigatória")
		EventModality modality,

		@Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres")
		String city,

		@Pattern(regexp = "^[A-Z]{2}$", message = "UF deve ter exatamente 2 letras")
		String stateUf,

		@Size(max = 255, message = "Endereço deve ter no máximo 255 caracteres")
		String address,

		@Size(max = 500, message = "Link online deve ter no máximo 500 caracteres")
		String onlineUrl,

		@NotNull(message = "Data/hora de início é obrigatória")
		@Future(message = "Data/hora de início deve estar no futuro")
		Instant startsAt,

		@NotNull(message = "Data/hora de término é obrigatória")
		Instant endsAt,

		Instant registrationClosesAt,

		@Positive(message = "Capacidade deve ser maior que zero")
		Integer capacity) {

	public CreateEventRequest {
		title = title != null ? title.trim() : null;
		description = description != null ? description.trim() : null;
		city = city != null && !city.isBlank() ? city.trim() : null;
		stateUf = stateUf != null && !stateUf.isBlank() ? stateUf.trim().toUpperCase(Locale.ROOT) : null;
		address = address != null && !address.isBlank() ? address.trim() : null;
		onlineUrl = onlineUrl != null && !onlineUrl.isBlank() ? onlineUrl.trim() : null;
	}
}
