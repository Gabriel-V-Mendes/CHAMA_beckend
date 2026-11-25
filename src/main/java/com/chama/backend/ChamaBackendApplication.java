package com.chama.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Ponto de entrada da aplicação CHAMA Backend. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ChamaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChamaBackendApplication.class, args);
	}
}
