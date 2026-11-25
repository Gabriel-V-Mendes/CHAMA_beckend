package com.chama.backend.modules.auth.controller;

import com.chama.backend.modules.auth.dto.AuthResponse;
import com.chama.backend.modules.auth.dto.LoginRequest;
import com.chama.backend.modules.auth.dto.RefreshTokenRequest;
import com.chama.backend.modules.auth.dto.RegisterRequest;
import com.chama.backend.modules.auth.dto.UserSummaryResponse;
import com.chama.backend.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints REST de cadastro, login, renovação de sessão e perfil autenticado. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	/** Cadastra um novo estudante e retorna tokens iniciais de acesso. */
	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
	}

	/** Autentica usuário existente com e-mail e senha. */
	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	/** Rotaciona o refresh token e emite um novo par de tokens. */
	@PostMapping("/refresh")
	public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ResponseEntity.ok(authService.refresh(request));
	}

	/** Revoga o refresh token informado encerrando a sessão correspondente. */
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
		authService.logout(request);
		return ResponseEntity.noContent().build();
	}

	/** Retorna os dados seguros do usuário autenticado pelo JWT. */
	@GetMapping("/me")
	public ResponseEntity<UserSummaryResponse> me(@AuthenticationPrincipal Jwt jwt) {
		UUID userId = UUID.fromString(jwt.getSubject());
		return ResponseEntity.ok(authService.getCurrentUser(userId));
	}
}
