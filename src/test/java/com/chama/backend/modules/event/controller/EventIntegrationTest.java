package com.chama.backend.modules.event.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chama.backend.core.security.JwtTokenService;
import com.chama.backend.modules.event.repository.EventRepository;
import com.chama.backend.modules.user.model.User;
import com.chama.backend.modules.user.model.UserRole;
import com.chama.backend.modules.user.repository.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Testes de integração end-to-end da criação de eventos (Spec 06). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EventIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtTokenService jwtTokenService;

	@BeforeEach
	void cleanDatabase() {
		eventRepository.deleteAll();
		userRepository.deleteAll();
	}

	/** Organizador PARTNER consegue criar evento presencial válido e recebe 201 com status DRAFT. */
	@Test
	void shouldCreatePresencialEventWhenCallerIsPartner() throws Exception {
		String token = accessTokenFor(UserRole.PARTNER);

		String body =
				"""
				{
				  "title": "Workshop de Introdução à Programação",
				  "description": "Workshop gratuito para jovens de Porto Alegre.",
				  "category": "workshop",
				  "modality": "presencial",
				  "city": "Porto Alegre",
				  "state_uf": "rs",
				  "address": "Rua Exemplo, 123",
				  "starts_at": "2027-03-10T18:00:00Z",
				  "ends_at": "2027-03-10T20:00:00Z",
				  "capacity": 50
				}
				""";

		mockMvc
				.perform(
						post("/api/v1/events")
								.header("Authorization", "Bearer " + token)
								.contentType(MediaType.APPLICATION_JSON)
								.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.title").value("Workshop de Introdução à Programação"))
				.andExpect(jsonPath("$.city").value("Porto Alegre"))
				.andExpect(jsonPath("$.state_uf").value("RS"))
				.andExpect(jsonPath("$.status").value("draft"))
				.andExpect(jsonPath("$.id").isNotEmpty());
	}

	/** Organizador ADMIN também pode criar eventos. */
	@Test
	void shouldCreateEventWhenCallerIsAdmin() throws Exception {
		String token = accessTokenFor(UserRole.ADMIN);

		String body =
				"""
				{
				  "title": "Live de Carreiras Tech",
				  "description": "Bate-papo online sobre vagas em tecnologia.",
				  "category": "palestra",
				  "modality": "online",
				  "online_url": "https://meet.example.org/chama-live",
				  "starts_at": "2027-03-15T19:00:00Z",
				  "ends_at": "2027-03-15T20:00:00Z"
				}
				""";

		mockMvc
				.perform(
						post("/api/v1/events")
								.header("Authorization", "Bearer " + token)
								.contentType(MediaType.APPLICATION_JSON)
								.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.city").doesNotExist())
				.andExpect(jsonPath("$.online_url").value("https://meet.example.org/chama-live"));
	}

	/** Estudante autenticado não pode criar eventos (RBAC restrito a PARTNER/ADMIN). */
	@Test
	void shouldRejectEventCreationWhenCallerIsStudent() throws Exception {
		String token = accessTokenFor(UserRole.STUDENT);

		mockMvc
				.perform(
						post("/api/v1/events")
								.header("Authorization", "Bearer " + token)
								.contentType(MediaType.APPLICATION_JSON)
								.content(minimalPresencialEventBody()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
	}

	/** Requisição sem token é rejeitada com 401. */
	@Test
	void shouldRejectEventCreationWithoutToken() throws Exception {
		mockMvc
				.perform(
						post("/api/v1/events")
								.contentType(MediaType.APPLICATION_JSON)
								.content(minimalPresencialEventBody()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}

	/** Evento presencial sem cidade/UF é rejeitado com 422 e código de negócio específico. */
	@Test
	void shouldReturnUnprocessableWhenPresencialEventHasNoLocation() throws Exception {
		String token = accessTokenFor(UserRole.PARTNER);
		String body =
				"""
				{
				  "title": "Workshop sem local",
				  "description": "Descrição qualquer.",
				  "category": "workshop",
				  "modality": "presencial",
				  "starts_at": "2027-03-10T18:00:00Z",
				  "ends_at": "2027-03-10T20:00:00Z"
				}
				""";

		mockMvc
				.perform(
						post("/api/v1/events")
								.header("Authorization", "Bearer " + token)
								.contentType(MediaType.APPLICATION_JSON)
								.content(body))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.error.code").value("EVENT_LOCATION_REQUIRED"));
	}

	/** Título em branco é rejeitado pela validação de borda (Bean Validation) com 422. */
	@Test
	void shouldReturnValidationErrorForBlankTitle() throws Exception {
		String token = accessTokenFor(UserRole.PARTNER);
		String body =
				"""
				{
				  "title": "",
				  "description": "Descrição qualquer.",
				  "category": "workshop",
				  "modality": "presencial",
				  "city": "Porto Alegre",
				  "state_uf": "RS",
				  "starts_at": "2027-03-10T18:00:00Z",
				  "ends_at": "2027-03-10T20:00:00Z"
				}
				""";

		mockMvc
				.perform(
						post("/api/v1/events")
								.header("Authorization", "Bearer " + token)
								.contentType(MediaType.APPLICATION_JSON)
								.content(body))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	/** Campo desconhecido no payload é bloqueado como Mass Assignment (400). */
	@Test
	void shouldRejectMassAssignmentWhenUnknownStatusFieldIsSent() throws Exception {
		String token = accessTokenFor(UserRole.PARTNER);
		String body =
				"""
				{
				  "title": "Workshop",
				  "description": "Descrição qualquer.",
				  "category": "workshop",
				  "modality": "presencial",
				  "city": "Porto Alegre",
				  "state_uf": "RS",
				  "starts_at": "2027-03-10T18:00:00Z",
				  "ends_at": "2027-03-10T20:00:00Z",
				  "status": "published"
				}
				""";

		mockMvc
				.perform(
						post("/api/v1/events")
								.header("Authorization", "Bearer " + token)
								.contentType(MediaType.APPLICATION_JSON)
								.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));
	}

	private String minimalPresencialEventBody() {
		return """
				{
				  "title": "Workshop de Introdução à Programação",
				  "description": "Workshop gratuito para jovens de Porto Alegre.",
				  "category": "workshop",
				  "modality": "presencial",
				  "city": "Porto Alegre",
				  "state_uf": "RS",
				  "starts_at": "2027-03-10T18:00:00Z",
				  "ends_at": "2027-03-10T20:00:00Z"
				}
				""";
	}

	private String accessTokenFor(UserRole role) {
		User user =
				new User(
						role.getValue() + "@chama.org", "hash", role, "2026-11", Instant.now());
		user = userRepository.saveAndFlush(user);
		return jwtTokenService.generateAccessToken(user, Instant.now());
	}
}
