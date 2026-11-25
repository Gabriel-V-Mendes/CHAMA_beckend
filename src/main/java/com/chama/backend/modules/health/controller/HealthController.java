package com.chama.backend.modules.health.controller;

import java.sql.Connection;
import java.time.Instant;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de verificação de disponibilidade (liveness/readiness) da aplicação. */
@RestController
public class HealthController {

	private final DataSource dataSource;

	public HealthController(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/** Confirma que o processo da aplicação está ativo (liveness probe). */
	@GetMapping("/health/live")
	public ResponseEntity<Map<String, Object>> live() {
		return ResponseEntity.ok(
				Map.of(
						"status", "UP",
						"check", "liveness",
						"timestamp", Instant.now().toString()));
	}

	/** Verifica conectividade real com o PostgreSQL antes de aceitar tráfego (readiness probe). */
	@GetMapping("/health/ready")
	public ResponseEntity<Map<String, Object>> ready() {
		try (Connection connection = dataSource.getConnection()) {
			if (connection.isValid(2)) {
				return ResponseEntity.ok(
						Map.of(
								"status", "UP",
								"database", "UP",
								"timestamp", Instant.now().toString()));
			}
		} catch (Exception ignored) {
			// Não expõe detalhes internos de conexão ou driver JDBC na resposta HTTP.
		}
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(
						Map.of(
								"status", "DOWN",
								"database", "DOWN",
								"timestamp", Instant.now().toString()));
	}

	/** Endpoint legado mantido para compatibilidade com verificações iniciais. */
	@GetMapping("/api/health")
	public ResponseEntity<Map<String, Object>> legacyHealth() {
		return ResponseEntity.ok(
				Map.of(
						"status", "UP",
						"service", "chama-backend",
						"timestamp", Instant.now().toString()));
	}
}
