package com.chama.backend.modules.event.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum EventModality {
	PRESENCIAL("presencial"),
	ONLINE("online"),
	HIBRIDO("hibrido");

	private final String value;

	EventModality(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}
}
