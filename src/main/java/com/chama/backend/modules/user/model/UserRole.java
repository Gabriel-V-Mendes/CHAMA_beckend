package com.chama.backend.modules.user.model;

import com.fasterxml.jackson.annotation.JsonValue;

/** Papéis de acesso previstos para RBAC (Spec 02). */
public enum UserRole {
	STUDENT("student"),
	PARTNER("partner"),
	ADMIN("admin");

	private final String value;

	UserRole(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}
}
