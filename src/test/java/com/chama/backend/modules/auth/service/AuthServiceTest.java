package com.chama.backend.modules.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chama.backend.core.exception.AppException;
import com.chama.backend.core.security.JwtTokenService;
import com.chama.backend.modules.auth.dto.AuthResponse;
import com.chama.backend.modules.auth.dto.LoginRequest;
import com.chama.backend.modules.auth.dto.RegisterRequest;
import com.chama.backend.modules.auth.model.RefreshToken;
import com.chama.backend.modules.auth.repository.RefreshTokenRepository;
import com.chama.backend.modules.user.model.StudentProfile;
import com.chama.backend.modules.user.model.User;
import com.chama.backend.modules.user.model.UserRole;
import com.chama.backend.modules.user.repository.StudentProfileRepository;
import com.chama.backend.modules.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Testes unitários das regras de negócio, segurança e conformidade LGPD no AuthService. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private StudentProfileRepository studentProfileRepository;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private JwtTokenService jwtTokenService;

	@Spy
	private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService =
				new AuthService(
						userRepository,
						studentProfileRepository,
						refreshTokenRepository,
						passwordEncoder,
						jwtTokenService);
	}

	/** Garante cadastro de maior de idade com e-mail normalizado, papel STUDENT e hash BCrypt >= 12. */
	@Test
	void shouldRegisterAdultStudentWithNormalizedEmailAndBcryptHash() {
		RegisterRequest request =
				new RegisterRequest(
						"  Estudante@Chama.org ",
						"SenhaForte@123",
						"Ana Beatriz",
						LocalDate.now().minusYears(20),
						"São Paulo",
						"Capão Redondo",
						true,
						"2026-11",
						null);

		when(userRepository.existsByEmailIgnoreCase("estudante@chama.org")).thenReturn(false);
		when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
		when(studentProfileRepository.save(any(StudentProfile.class)))
				.thenAnswer(inv -> inv.getArgument(0));
		when(jwtTokenService.generateAccessToken(any(User.class), any())).thenReturn("jwt-access");
		when(jwtTokenService.generateOpaqueRefreshToken()).thenReturn("opaque-refresh");
		when(jwtTokenService.hashToken("opaque-refresh")).thenReturn("hash-sha256");
		when(jwtTokenService.getAccessTokenTtlSeconds()).thenReturn(900L);
		when(jwtTokenService.getRefreshTokenTtlSeconds()).thenReturn(604800L);

		AuthResponse response = authService.register(request);

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).saveAndFlush(userCaptor.capture());
		User savedUser = userCaptor.getValue();

		assertThat(savedUser.getEmail()).isEqualTo("estudante@chama.org");
		assertThat(savedUser.getRole()).isEqualTo(UserRole.STUDENT);
		assertThat(savedUser.getPasswordHash()).startsWith("$2").contains("$12$");
		assertThat(savedUser.getPasswordHash()).isNotEqualTo("SenhaForte@123");

		ArgumentCaptor<StudentProfile> profileCaptor = ArgumentCaptor.forClass(StudentProfile.class);
		verify(studentProfileRepository).save(profileCaptor.capture());
		assertThat(profileCaptor.getValue().getGuardianConsentAt()).isNull();

		verify(refreshTokenRepository).save(any(RefreshToken.class));
		assertThat(response.accessToken()).isEqualTo("jwt-access");
		assertThat(response.user().role()).isEqualTo(UserRole.STUDENT);
	}

	/** Bloqueia cadastro de menor de 18 anos sem consentimento do responsável (LGPD Art. 14). */
	@Test
	void shouldRejectMinorWithoutGuardianConsent() {
		RegisterRequest request =
				new RegisterRequest(
						"menor@chama.org",
						"SenhaForte@123",
						"João Pedro",
						LocalDate.now().minusYears(16),
						"Recife",
						null,
						true,
						"2026-11",
						false);

		assertThatThrownBy(() -> authService.register(request))
				.isInstanceOf(AppException.class)
				.satisfies(
						ex -> {
							AppException appEx = (AppException) ex;
							assertThat(appEx.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
							assertThat(appEx.getCode()).isEqualTo("GUARDIAN_CONSENT_REQUIRED");
						});
	}

	/** Registra timestamp de consentimento quando o responsável autoriza o cadastro de menor. */
	@Test
	void shouldRegisterMinorWithGuardianConsentTimestamp() {
		RegisterRequest request =
				new RegisterRequest(
						"menor@chama.org",
						"SenhaForte@123",
						"João Pedro",
						LocalDate.now().minusYears(16),
						"Recife",
						"Ibura",
						true,
						"2026-11",
						true);

		when(userRepository.existsByEmailIgnoreCase("menor@chama.org")).thenReturn(false);
		when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
		when(studentProfileRepository.save(any(StudentProfile.class)))
				.thenAnswer(inv -> inv.getArgument(0));
		when(jwtTokenService.generateAccessToken(any(User.class), any())).thenReturn("jwt-access");
		when(jwtTokenService.generateOpaqueRefreshToken()).thenReturn("opaque-refresh");
		when(jwtTokenService.hashToken("opaque-refresh")).thenReturn("hash-sha256");
		when(jwtTokenService.getAccessTokenTtlSeconds()).thenReturn(900L);
		when(jwtTokenService.getRefreshTokenTtlSeconds()).thenReturn(604800L);

		authService.register(request);

		ArgumentCaptor<StudentProfile> profileCaptor = ArgumentCaptor.forClass(StudentProfile.class);
		verify(studentProfileRepository).save(profileCaptor.capture());
		assertThat(profileCaptor.getValue().getGuardianConsentAt()).isNotNull();
	}

	/** Impede criação de conta duplicada retornando 409 EMAIL_ALREADY_EXISTS. */
	@Test
	void shouldThrowConflictWhenEmailAlreadyExists() {
		RegisterRequest request =
				new RegisterRequest(
						"duplicado@chama.org",
						"SenhaForte@123",
						"Maria Clara",
						LocalDate.now().minusYears(19),
						"Salvador",
						null,
						true,
						"2026-11",
						null);

		when(userRepository.existsByEmailIgnoreCase("duplicado@chama.org")).thenReturn(true);

		assertThatThrownBy(() -> authService.register(request))
				.isInstanceOf(AppException.class)
				.satisfies(
						ex -> {
							AppException appEx = (AppException) ex;
							assertThat(appEx.getStatus()).isEqualTo(HttpStatus.CONFLICT);
							assertThat(appEx.getCode()).isEqualTo("EMAIL_ALREADY_EXISTS");
						});
	}

	/** Executa verificação contra hash dummy quando e-mail não existe para mitigar timing attack. */
	@Test
	void shouldVerifyDummyHashWhenEmailDoesNotExistOnLogin() {
		LoginRequest request = new LoginRequest("naoexiste@chama.org", "QualquerSenha@123");
		when(userRepository.findByEmailIgnoreCase("naoexiste@chama.org")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(request))
				.isInstanceOf(AppException.class)
				.satisfies(
						ex -> {
							AppException appEx = (AppException) ex;
							assertThat(appEx.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
							assertThat(appEx.getCode()).isEqualTo("INVALID_CREDENTIALS");
						});

		verify(passwordEncoder).matches(anyString(), anyString());
	}
}
