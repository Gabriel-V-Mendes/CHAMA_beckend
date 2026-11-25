package com.chama.backend.modules.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Perfil mínimo do estudante atendendo à minimização de dados e Art. 14 da LGPD. */
@Entity
@Table(name = "student_profiles")
public class StudentProfile {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Column(name = "full_name", nullable = false, length = 150)
	private String fullName;

	@Column(name = "birth_date", nullable = false)
	private LocalDate birthDate;

	@Column(name = "neighborhood", length = 100)
	private String neighborhood;

	@Column(name = "city", nullable = false, length = 100)
	private String city;

	@Column(name = "guardian_consent_at")
	private Instant guardianConsentAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected StudentProfile() {}

	public StudentProfile(
			User user,
			String fullName,
			LocalDate birthDate,
			String neighborhood,
			String city,
			Instant guardianConsentAt) {
		this.user = user;
		this.fullName = fullName;
		this.birthDate = birthDate;
		this.neighborhood = neighborhood;
		this.city = city;
		this.guardianConsentAt = guardianConsentAt;
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

	public User getUser() {
		return user;
	}

	public String getFullName() {
		return fullName;
	}

	public LocalDate getBirthDate() {
		return birthDate;
	}

	public String getNeighborhood() {
		return neighborhood;
	}

	public String getCity() {
		return city;
	}

	public Instant getGuardianConsentAt() {
		return guardianConsentAt;
	}
}
