package com.chama.backend.modules.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Entidade de credenciais e papel de acesso do usuário (tabela users). */
@Entity
@Table(name = "users")
public class User {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "email", nullable = false, unique = true, length = 254)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 20)
	private UserRole role;

	@Column(name = "is_active", nullable = false)
	private boolean isActive = true;

	@Column(name = "terms_version", nullable = false, length = 32)
	private String termsVersion;

	@Column(name = "terms_accepted_at", nullable = false)
	private Instant termsAcceptedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	protected User() {}

	public User(
			String email,
			String passwordHash,
			UserRole role,
			String termsVersion,
			Instant termsAcceptedAt) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.role = role;
		this.termsVersion = termsVersion;
		this.termsAcceptedAt = termsAcceptedAt;
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		if (this.id == null) {
			this.id = UUID.randomUUID();
		}
		if (this.createdAt == null) {
			this.createdAt = now;
		}
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public UserRole getRole() {
		return role;
	}

	public boolean isActive() {
		return isActive;
	}

	public String getTermsVersion() {
		return termsVersion;
	}

	public Instant getTermsAcceptedAt() {
		return termsAcceptedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public Instant getDeletedAt() {
		return deletedAt;
	}
}
