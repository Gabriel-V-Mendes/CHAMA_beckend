package com.chama.backend.modules.event.mapper;

import com.chama.backend.modules.event.dto.CreateEventRequest;
import com.chama.backend.modules.event.dto.EventResponse;
import com.chama.backend.modules.event.model.Event;
import com.chama.backend.modules.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

	public Event toEntity(CreateEventRequest request, User organizer) {
		return new Event(
				organizer,
				request.title(),
				request.description(),
				request.category(),
				request.modality(),
				request.city(),
				request.stateUf(),
				request.address(),
				request.onlineUrl(),
				request.startsAt(),
				request.endsAt(),
				request.registrationClosesAt(),
				request.capacity());
	}

	public EventResponse toResponse(Event event) {
		return new EventResponse(
				event.getId(),
				event.getOrganizer().getId(),
				event.getTitle(),
				event.getDescription(),
				event.getCategory(),
				event.getModality(),
				event.getCity(),
				event.getStateUf(),
				event.getAddress(),
				event.getOnlineUrl(),
				event.getStartsAt(),
				event.getEndsAt(),
				event.getRegistrationClosesAt(),
				event.getCapacity(),
				event.getStatus(),
				event.getCreatedAt());
	}
}
