package com.chama.backend.modules.event.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum EventCategory {
	WORKSHOP("workshop"),
	PALESTRA("palestra"),
	HACKATHON("hackathon"),
	MENTORIA("mentoria"),
	VISITA_TECNICA("visita_tecnica"),
	FEIRA("feira"),
	OUTRO("outro");

	private final String value;

	EventCategory(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}
}
