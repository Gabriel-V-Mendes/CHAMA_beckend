package com.chama.backend.modules.event.dto;

import com.chama.backend.modules.event.model.EventCategory;
import com.chama.backend.modules.event.model.EventModality;
import com.chama.backend.modules.event.model.EventStatus;
import java.time.Instant;
import java.util.UUID;

public record EventResponse(
		UUID id,
		UUID organizerId,
		String title,
		String description,
		EventCategory category,
		EventModality modality,
		String city,
		String stateUf,
		String address,
		String onlineUrl,
		Instant startsAt,
		Instant endsAt,
		Instant registrationClosesAt,
		Integer capacity,
		EventStatus status,
		Instant createdAt) {}
