package com.chama.backend.modules.event.model;

import com.chama.backend.modules.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "events")
public class Event {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "organizer_id", nullable = false)
	private User organizer;

	@Column(name = "title", nullable = false, length = 150)
	private String title;

	@Column(name = "description", nullable = false)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "category", nullable = false, length = 30)
	private EventCategory category;

	@Enumerated(EnumType.STRING)
	@Column(name = "modality", nullable = false, length = 20)
	private EventModality modality;

	@Column(name = "city", length = 100)
	private String city;

	@Column(name = "state_uf", length = 2)
	private String stateUf;

	@Column(name = "address", length = 255)
	private String address;

	@Column(name = "online_url", length = 500)
	private String onlineUrl;

	@Column(name = "starts_at", nullable = false)
	private Instant startsAt;

	@Column(name = "ends_at", nullable = false)
	private Instant endsAt;

	@Column(name = "registration_closes_at")
	private Instant registrationClosesAt;

	@Column(name = "capacity")
	private Integer capacity;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private EventStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	protected Event() {}

	public Event(
			User organizer,
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
			Integer capacity) {
		this.organizer = organizer;
		this.title = title;
		this.description = description;
		this.category = category;
		this.modality = modality;
		this.city = city;
		this.stateUf = stateUf;
		this.address = address;
		this.onlineUrl = onlineUrl;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
		this.registrationClosesAt = registrationClosesAt;
		this.capacity = capacity;
		this.status = EventStatus.DRAFT;
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

	public User getOrganizer() {
		return organizer;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public EventCategory getCategory() {
		return category;
	}

	public EventModality getModality() {
		return modality;
	}

	public String getCity() {
		return city;
	}

	public String getStateUf() {
		return stateUf;
	}

	public String getAddress() {
		return address;
	}

	public String getOnlineUrl() {
		return onlineUrl;
	}

	public Instant getStartsAt() {
		return startsAt;
	}

	public Instant getEndsAt() {
		return endsAt;
	}

	public Instant getRegistrationClosesAt() {
		return registrationClosesAt;
	}

	public Integer getCapacity() {
		return capacity;
	}

	public EventStatus getStatus() {
		return status;
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
