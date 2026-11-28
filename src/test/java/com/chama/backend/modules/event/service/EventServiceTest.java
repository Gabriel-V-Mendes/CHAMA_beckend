package com.chama.backend.modules.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.chama.backend.core.exception.AppException;
import com.chama.backend.modules.event.dto.CreateEventRequest;
import com.chama.backend.modules.event.dto.EventResponse;
import com.chama.backend.modules.event.mapper.EventMapper;
import com.chama.backend.modules.event.model.Event;
import com.chama.backend.modules.event.model.EventCategory;
import com.chama.backend.modules.event.model.EventModality;
import com.chama.backend.modules.event.repository.EventRepository;
import com.chama.backend.modules.user.model.User;
import com.chama.backend.modules.user.model.UserRole;
import com.chama.backend.modules.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

/** Testes unitários das regras de negócio de criação de eventos (Spec 06). */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

	@Mock
	private EventRepository eventRepository;

	@Mock
	private UserRepository userRepository;

	private EventService eventService;
	private User partnerOrganizer;
	private UUID organizerId;

	@BeforeEach
	void setUp() {
		eventService = new EventService(eventRepository, userRepository, new EventMapper());

		partnerOrganizer =
				new User(
						"parceiro@chama.org", "hash", UserRole.PARTNER, "2026-11", Instant.now());
		organizerId = UUID.randomUUID();
		ReflectionTestUtils.setField(partnerOrganizer, "id", organizerId);
	}

	/** Cria evento presencial com cidade/UF e sem exigir link online. */
	@Test
	void shouldCreatePresencialEventWithCityAndState() {
		when(userRepository.findById(organizerId)).thenReturn(Optional.of(partnerOrganizer));
		when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

		CreateEventRequest request = presencialRequest();
		EventResponse response = eventService.createEvent(request, organizerId);

		assertThat(response.organizerId()).isEqualTo(organizerId);
		assertThat(response.city()).isEqualTo("Porto Alegre");
		assertThat(response.stateUf()).isEqualTo("RS");
		assertThat(response.status().name()).isEqualTo("DRAFT");
	}

	/** Cria evento 100% online sem cidade/UF, desde que tenha link de acesso. */
	@Test
	void shouldCreateOnlineEventWithoutCityWhenOnlineUrlPresent() {
		when(userRepository.findById(organizerId)).thenReturn(Optional.of(partnerOrganizer));
		when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

		CreateEventRequest request =
				new CreateEventRequest(
						"Live de Carreiras Tech",
						"Bate-papo online sobre primeiras vagas em tecnologia.",
						EventCategory.PALESTRA,
						EventModality.ONLINE,
						null,
						null,
						null,
						"https://meet.example.org/chama-live",
						Instant.now().plus(5, ChronoUnit.DAYS),
						Instant.now().plus(5, ChronoUnit.DAYS).plusSeconds(3600),
						null,
						null);

		EventResponse response = eventService.createEvent(request, organizerId);

		assertThat(response.city()).isNull();
		assertThat(response.onlineUrl()).isEqualTo("https://meet.example.org/chama-live");
	}

	/** Bloqueia evento presencial/híbrido sem cidade e UF informadas. */
	@Test
	void shouldRejectPresencialEventWithoutLocation() {
		when(userRepository.findById(organizerId)).thenReturn(Optional.of(partnerOrganizer));

		CreateEventRequest request =
				new CreateEventRequest(
						"Workshop sem local",
						"Descrição qualquer.",
						EventCategory.WORKSHOP,
						EventModality.PRESENCIAL,
						null,
						null,
						null,
						null,
						Instant.now().plus(5, ChronoUnit.DAYS),
						Instant.now().plus(5, ChronoUnit.DAYS).plusSeconds(3600),
						null,
						null);

		assertThatThrownBy(() -> eventService.createEvent(request, organizerId))
				.isInstanceOf(AppException.class)
				.satisfies(
						ex -> {
							AppException appEx = (AppException) ex;
							assertThat(appEx.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
							assertThat(appEx.getCode()).isEqualTo("EVENT_LOCATION_REQUIRED");
						});
	}

	/** Bloqueia evento híbrido/online sem link de acesso informado. */
	@Test
	void shouldRejectHibridoEventWithoutOnlineUrl() {
		when(userRepository.findById(organizerId)).thenReturn(Optional.of(partnerOrganizer));

		CreateEventRequest request =
				new CreateEventRequest(
						"Hackathon híbrido",
						"Descrição qualquer.",
						EventCategory.HACKATHON,
						EventModality.HIBRIDO,
						"Porto Alegre",
						"RS",
						null,
						null,
						Instant.now().plus(5, ChronoUnit.DAYS),
						Instant.now().plus(5, ChronoUnit.DAYS).plusSeconds(3600),
						null,
						null);

		assertThatThrownBy(() -> eventService.createEvent(request, organizerId))
				.isInstanceOf(AppException.class)
				.satisfies(
						ex -> {
							AppException appEx = (AppException) ex;
							assertThat(appEx.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
							assertThat(appEx.getCode()).isEqualTo("EVENT_ONLINE_URL_REQUIRED");
						});
	}

	/** Bloqueia evento cujo término não é posterior ao início. */
	@Test
	void shouldRejectEventWhenEndsAtIsNotAfterStartsAt() {
		when(userRepository.findById(organizerId)).thenReturn(Optional.of(partnerOrganizer));

		Instant startsAt = Instant.now().plus(5, ChronoUnit.DAYS);
		CreateEventRequest request =
				new CreateEventRequest(
						"Workshop com datas inválidas",
						"Descrição qualquer.",
						EventCategory.WORKSHOP,
						EventModality.PRESENCIAL,
						"Porto Alegre",
						"RS",
						null,
						null,
						startsAt,
						startsAt.minusSeconds(60),
						null,
						null);

		assertThatThrownBy(() -> eventService.createEvent(request, organizerId))
				.isInstanceOf(AppException.class)
				.satisfies(
						ex -> {
							AppException appEx = (AppException) ex;
							assertThat(appEx.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
							assertThat(appEx.getCode()).isEqualTo("EVENT_INVALID_DATE_RANGE");
						});
	}

	/** Bloqueia criação quando o organizador do token está inativo ou foi removido. */
	@Test
	void shouldRejectWhenOrganizerIsInactive() {
		when(userRepository.findById(organizerId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> eventService.createEvent(presencialRequest(), organizerId))
				.isInstanceOf(AppException.class)
				.satisfies(
						ex -> {
							AppException appEx = (AppException) ex;
							assertThat(appEx.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
							assertThat(appEx.getCode()).isEqualTo("INVALID_TOKEN");
						});
	}

	private CreateEventRequest presencialRequest() {
		Instant startsAt = Instant.now().plus(5, ChronoUnit.DAYS);
		return new CreateEventRequest(
				"Workshop de Introdução à Programação",
				"Workshop gratuito para jovens de escolas públicas de Porto Alegre.",
				EventCategory.WORKSHOP,
				EventModality.PRESENCIAL,
				"Porto Alegre",
				"RS",
				"Rua Exemplo, 123",
				null,
				startsAt,
				startsAt.plusSeconds(7200),
				null,
				50);
	}
}
