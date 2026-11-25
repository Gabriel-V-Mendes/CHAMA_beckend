package com.chama.backend.modules.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chama.backend.modules.auth.repository.RefreshTokenRepository;
import com.chama.backend.modules.user.repository.StudentProfileRepository;
import com.chama.backend.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Testes de integração end-to-end dos endpoints de autenticação, segurança HTTP e rate limit. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private StudentProfileRepository studentProfileRepository;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void cleanDatabase() {
		refreshTokenRepository.deleteAll();
		studentProfileRepository.deleteAll();
		userRepository.deleteAll();
	}

	/** Valida ciclo completo: cadastro, login com normalização, /me, rotação, reuso e logout. */
	@Test
	void shouldCompleteRegisterLoginMeRefreshReuseDetectionAndLogoutFlow() throws Exception {
		String registerBody =
				"""
				{
				  "email": "Jovem.Chama@Exemplo.org",
				  "password": "Chama@2026Strong",
				  "full_name": "Lucas Oliveira",
				  "birth_date": "2004-05-15",
				  "city": "Belo Horizonte",
				  "neighborhood": "Barreiro",
				  "terms_accepted": true,
				  "terms_version": "2026-11"
				}
				""";

		MvcResult registerResult =
				mockMvc
						.perform(
								post("/api/v1/auth/register")
										.with(remoteIp("10.10.0.1"))
										.contentType(MediaType.APPLICATION_JSON)
										.content(registerBody))
						.andExpect(status().isCreated())
						.andExpect(header().string("X-Content-Type-Options", "nosniff"))
						.andExpect(header().string("X-Frame-Options", "DENY"))
						.andExpect(header().exists("X-Request-Id"))
						.andExpect(jsonPath("$.access_token").isNotEmpty())
						.andExpect(jsonPath("$.refresh_token").isNotEmpty())
						.andExpect(jsonPath("$.token_type").value("Bearer"))
						.andExpect(jsonPath("$.user.email").value("jovem.chama@exemplo.org"))
						.andExpect(jsonPath("$.user.role").value("student"))
						.andExpect(jsonPath("$.user.password_hash").doesNotExist())
						.andReturn();

		JsonNode registerJson =
				jsonMapper.readTree(registerResult.getResponse().getContentAsString());
		String initialRefreshToken = registerJson.get("refresh_token").asString();

		// Login aceita e-mail com espaços nas bordas e letras maiúsculas.
		String loginBody =
				"""
				{
				  "email": "  JOVEM.CHAMA@exemplo.org  ",
				  "password": "Chama@2026Strong"
				}
				""";

		MvcResult loginResult =
				mockMvc
						.perform(
								post("/api/v1/auth/login")
										.with(remoteIp("10.10.0.2"))
										.contentType(MediaType.APPLICATION_JSON)
										.content(loginBody))
						.andExpect(status().isOk())
						.andExpect(jsonPath("$.access_token").isNotEmpty())
						.andExpect(jsonPath("$.user.full_name").value("Lucas Oliveira"))
						.andReturn();

		String accessToken =
				jsonMapper
						.readTree(loginResult.getResponse().getContentAsString())
						.get("access_token")
						.asString();

		mockMvc
				.perform(
						get("/api/v1/auth/me")
								.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("jovem.chama@exemplo.org"))
				.andExpect(jsonPath("$.city").value("Belo Horizonte"));

		// Rotaciona o refresh token inicial.
		MvcResult refreshResult =
				mockMvc
						.perform(
								post("/api/v1/auth/refresh")
										.with(remoteIp("10.10.0.3"))
										.contentType(MediaType.APPLICATION_JSON)
										.content("{\"refresh_token\":\"" + initialRefreshToken + "\"}"))
						.andExpect(status().isOk())
						.andExpect(jsonPath("$.refresh_token").isNotEmpty())
						.andReturn();

		String rotatedRefreshToken =
				jsonMapper
						.readTree(refreshResult.getResponse().getContentAsString())
						.get("refresh_token")
						.asString();
		assertThat(rotatedRefreshToken).isNotEqualTo(initialRefreshToken);

		// Reuso do refresh token antigo deve falhar e revogar os tokens ativos do usuário.
		mockMvc
				.perform(
						post("/api/v1/auth/refresh")
								.with(remoteIp("10.10.0.3"))
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"refresh_token\":\"" + initialRefreshToken + "\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));

		mockMvc
				.perform(
						post("/api/v1/auth/refresh")
								.with(remoteIp("10.10.0.3"))
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"refresh_token\":\"" + rotatedRefreshToken + "\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));

		// Novo login e logout explícito revogam o token emitido.
		MvcResult secondLoginResult =
				mockMvc
						.perform(
								post("/api/v1/auth/login")
										.with(remoteIp("10.10.0.4"))
										.contentType(MediaType.APPLICATION_JSON)
										.content(loginBody))
						.andExpect(status().isOk())
						.andReturn();
		String activeRefreshToken =
				jsonMapper
						.readTree(secondLoginResult.getResponse().getContentAsString())
						.get("refresh_token")
						.asString();

		mockMvc
				.perform(
						post("/api/v1/auth/logout")
								.with(remoteIp("10.10.0.4"))
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"refresh_token\":\"" + activeRefreshToken + "\"}"))
				.andExpect(status().isNoContent());

		mockMvc
				.perform(
						post("/api/v1/auth/refresh")
								.with(remoteIp("10.10.0.4"))
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"refresh_token\":\"" + activeRefreshToken + "\"}"))
				.andExpect(status().isUnauthorized());
	}

	/** Garante bloqueio de Mass Assignment rejeitando campos desconhecidos como 'role'. */
	@Test
	void shouldRejectMassAssignmentWhenUnknownRoleFieldIsSent() throws Exception {
		String bodyWithExtraField =
				"""
				{
				  "email": "invasor@exemplo.org",
				  "password": "Chama@2026Strong",
				  "full_name": "Teste",
				  "birth_date": "2002-01-10",
				  "city": "Fortaleza",
				  "terms_accepted": true,
				  "terms_version": "2026-11",
				  "role": "admin"
				}
				""";

		mockMvc
				.perform(
						post("/api/v1/auth/register")
								.with(remoteIp("10.20.0.1"))
								.contentType(MediaType.APPLICATION_JSON)
								.content(bodyWithExtraField))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"))
				.andExpect(jsonPath("$.error.request_id").isNotEmpty());
	}

	/** Garante resposta 422 padronizada com campos inválidos convertidos para snake_case. */
	@Test
	void shouldReturnValidationErrorWithSnakeCaseDetailsForWeakPassword() throws Exception {
		String weakPayload =
				"""
				{
				  "email": "email-invalido",
				  "password": "123",
				  "full_name": "A",
				  "birth_date": "2002-01-10",
				  "city": "Curitiba",
				  "terms_accepted": false,
				  "terms_version": "2026-11"
				}
				""";

		mockMvc
				.perform(
						post("/api/v1/auth/register")
								.with(remoteIp("10.20.0.2"))
								.contentType(MediaType.APPLICATION_JSON)
								.content(weakPayload))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.error.details").isArray());
	}

	/** Garante resposta 401 UNAUTHORIZED ao acessar rota protegida sem token Bearer. */
	@Test
	void shouldRejectProtectedEndpointWithoutToken() throws Exception {
		mockMvc
				.perform(get("/api/v1/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}

	/** Garante resposta 429 Too Many Requests e Retry-After ao exceder o limite por IP. */
	@Test
	void shouldEnforceRateLimitOnLoginEndpoint() throws Exception {
		String payload = "{\"email\":\"inexistente@chama.org\",\"password\":\"errada\"}";

		for (int i = 0; i < 20; i++) {
			mockMvc
					.perform(
							post("/api/v1/auth/login")
									.with(remoteIp("198.51.100.42"))
									.contentType(MediaType.APPLICATION_JSON)
									.content(payload))
					.andExpect(status().isUnauthorized());
		}

		mockMvc
				.perform(
						post("/api/v1/auth/login")
								.with(remoteIp("198.51.100.42"))
								.contentType(MediaType.APPLICATION_JSON)
								.content(payload))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().string("Retry-After", "60"))
				.andExpect(jsonPath("$.error.code").value("RATE_LIMIT_EXCEEDED"));
	}

	private RequestPostProcessor remoteIp(String ip) {
		return request -> {
			request.setRemoteAddr(ip);
			return request;
		};
	}
}
