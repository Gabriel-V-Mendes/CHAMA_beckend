package com.chama.backend.modules.event.service;

import com.chama.backend.core.exception.AppException;
import com.chama.backend.modules.event.dto.CreateEventRequest;
import com.chama.backend.modules.event.dto.EventResponse;
import com.chama.backend.modules.event.mapper.EventMapper;
import com.chama.backend.modules.event.model.Event;
import com.chama.backend.modules.event.model.EventModality;
import com.chama.backend.modules.event.repository.EventRepository;
import com.chama.backend.modules.user.model.User;
import com.chama.backend.modules.user.repository.UserRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

	private static final Logger log = LoggerFactory.getLogger(EventService.class);

	private final EventRepository eventRepository;
	private final UserRepository userRepository;
	private final EventMapper eventMapper;

	public EventService(
			EventRepository eventRepository, UserRepository userRepository, EventMapper eventMapper) {
		this.eventRepository = eventRepository;
		this.userRepository = userRepository;
		this.eventMapper = eventMapper;
	}

	@Transactional
	public EventResponse createEvent(CreateEventRequest request, UUID organizerId) {
		log.info("Iniciando chamada de criação de evento para [event_title={}, organizer_id={}]", request.title(), organizerId);
		User organizer =
				userRepository
						.findById(organizerId)
						.filter(u -> u.isActive() && u.getDeletedAt() == null)
						.orElseThrow(
								() -> AppException.unauthorized("INVALID_TOKEN", "Conta inativa ou indisponível."));

		validateLocationForModality(request);
		validateDateRange(request);

		Event event = eventMapper.toEntity(request, organizer);
		event = eventRepository.save(event);

		log.info("Evento criado [event_id={}, organizer_id={}]", event.getId(), organizerId);
		return eventMapper.toResponse(event);
	}

	private void validateLocationForModality(CreateEventRequest request) {
		boolean hasLocation = request.city() != null && request.stateUf() != null;
		if (request.modality() != EventModality.ONLINE && !hasLocation) {
			throw AppException.unprocessable(
					"EVENT_LOCATION_REQUIRED",
					"Cidade e UF são obrigatórias para eventos presenciais ou híbridos.");
		}

		boolean hasOnlineUrl = request.onlineUrl() != null;
		if (request.modality() != EventModality.PRESENCIAL && !hasOnlineUrl) {
			throw AppException.unprocessable(
					"EVENT_ONLINE_URL_REQUIRED",
					"Link de acesso é obrigatório para eventos online ou híbridos.");
		}
	}

	private void validateDateRange(CreateEventRequest request) {
		if (!request.endsAt().isAfter(request.startsAt())) {
			throw AppException.unprocessable(
					"EVENT_INVALID_DATE_RANGE", "A data/hora de término deve ser posterior à de início.");
		}
	}
}
