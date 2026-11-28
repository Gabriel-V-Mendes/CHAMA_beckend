package com.chama.backend.modules.event.controller;

import com.chama.backend.modules.event.dto.CreateEventRequest;
import com.chama.backend.modules.event.dto.EventResponse;
import com.chama.backend.modules.event.service.EventService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

	private final EventService eventService;

	public EventController(EventService eventService) {
		this.eventService = eventService;
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('PARTNER', 'ADMIN')")
	public ResponseEntity<EventResponse> create(
			@Valid @RequestBody CreateEventRequest request, @AuthenticationPrincipal Jwt jwt) {
		UUID organizerId = UUID.fromString(jwt.getSubject());
		return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(request, organizerId));
	}
}
