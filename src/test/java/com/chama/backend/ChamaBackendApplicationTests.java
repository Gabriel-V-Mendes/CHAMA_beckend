package com.chama.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Valida que o contexto completo (Spring, Flyway, JPA e Security) inicializa sem erros. */
@SpringBootTest
@ActiveProfiles("test")
class ChamaBackendApplicationTests {

	@Test
	void contextLoads() {}
}
