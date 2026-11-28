package com.chama.backend.modules.event.model;

import com.fasterxml.jackson.annotation.JsonValue;

/** Só eventos PUBLISHED aparecem na listagem pública. */
public enum EventStatus {
	DRAFT("draft"),
	PUBLISHED("published"),
	CANCELLED("cancelled"),
	COMPLETED("completed");

	private final String value;

	EventStatus(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}
}
