package com.chama.backend.modules.health.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Testes de integração para os endpoints públicos de liveness e readiness. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void shouldReturnLiveStatus() throws Exception {
		// Garante que o probe de liveness responde 200 sem exigir autenticação.
		mockMvc.perform(get("/health/live"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.check").value("liveness"))
				.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	void shouldReturnReadyStatusWithDatabaseUp() throws Exception {
		// Garante que o probe de readiness confirma conectividade com o banco de dados.
		mockMvc.perform(get("/health/ready"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.database").value("UP"))
				.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	void shouldKeepLegacyApiHealthEndpointWorking() throws Exception {
		// Mantém compatibilidade com o endpoint /api/health existente desde o bootstrap inicial.
		mockMvc.perform(get("/api/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.service").value("chama-backend"));
	}
}
