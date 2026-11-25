package com.chama.backend.modules.auth.service;

import com.chama.backend.core.exception.AppException;
import com.chama.backend.core.security.JwtTokenService;
import com.chama.backend.modules.auth.dto.AuthResponse;
import com.chama.backend.modules.auth.dto.LoginRequest;
import com.chama.backend.modules.auth.dto.RefreshTokenRequest;
import com.chama.backend.modules.auth.dto.RegisterRequest;
import com.chama.backend.modules.auth.dto.UserSummaryResponse;
import com.chama.backend.modules.auth.model.RefreshToken;
import com.chama.backend.modules.auth.repository.RefreshTokenRepository;
import com.chama.backend.modules.user.model.StudentProfile;
import com.chama.backend.modules.user.model.User;
import com.chama.backend.modules.user.model.UserRole;
import com.chama.backend.modules.user.repository.StudentProfileRepository;
import com.chama.backend.modules.user.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Regras de negócio de cadastro, autenticação, rotação de refresh token e sessão. */
@Service
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);
	private static final int LEGAL_AGE_YEARS = 18;

	private final UserRepository userRepository;
	private final StudentProfileRepository studentProfileRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenService jwtTokenService;
	private final String dummyPasswordHash;

	public AuthService(
			UserRepository userRepository,
			StudentProfileRepository studentProfileRepository,
			RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder,
			JwtTokenService jwtTokenService) {
		this.userRepository = userRepository;
		this.studentProfileRepository = studentProfileRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenService = jwtTokenService;
		// Hash dummy usado quando o e-mail não existe para manter tempo de resposta constante.
		this.dummyPasswordHash = passwordEncoder.encode("ChamaDummy#2026!TimingSafe");
	}

	/** Cadastra novo estudante aplicando validação de menor de idade (LGPD Art. 14) e unicidade. */
	@Transactional
	public AuthResponse register(RegisterRequest request) {
		Instant now = Instant.now();
		Instant guardianConsentAt = resolveGuardianConsent(request.birthDate(), request.guardianConsent(), now);

		if (userRepository.existsByEmailIgnoreCase(request.email())) {
			throw AppException.conflict(
					"EMAIL_ALREADY_EXISTS", "Já existe uma conta cadastrada com este e-mail.");
		}

		User user =
				new User(
						request.email(),
						passwordEncoder.encode(request.password()),
						UserRole.STUDENT,
						request.termsVersion(),
						now);
		try {
			user = userRepository.saveAndFlush(user);
		} catch (DataIntegrityViolationException ex) {
			throw AppException.conflict(
					"EMAIL_ALREADY_EXISTS", "Já existe uma conta cadastrada com este e-mail.");
		}

		StudentProfile profile =
				new StudentProfile(
						user,
						request.fullName(),
						request.birthDate(),
						request.neighborhood(),
						request.city(),
						guardianConsentAt);
		studentProfileRepository.save(profile);

		log.info("Novo usuário registrado [user_id={}]", user.getId());
		return issueSessionTokens(user, profile, now);
	}

	/** Autentica usuário existente validando hash BCrypt e status da conta. */
	@Transactional
	public AuthResponse login(LoginRequest request) {
		User user = userRepository.findByEmailIgnoreCase(request.email()).orElse(null);
		if (user == null) {
			passwordEncoder.matches(request.password(), dummyPasswordHash);
			throw invalidCredentials();
		}

		boolean passwordMatches = passwordEncoder.matches(request.password(), user.getPasswordHash());
		if (!passwordMatches || !user.isActive() || user.getDeletedAt() != null) {
			throw invalidCredentials();
		}

		StudentProfile profile = studentProfileRepository.findByUserId(user.getId()).orElse(null);
		log.info("Login realizado com sucesso [user_id={}]", user.getId());
		return issueSessionTokens(user, profile, Instant.now());
	}

	/** Rotaciona refresh token e revoga todos os tokens ativos se houver tentativa de reuso. */
	@Transactional(noRollbackFor = AppException.class)
	public AuthResponse refresh(RefreshTokenRequest request) {
		Instant now = Instant.now();
		String tokenHash = jwtTokenService.hashToken(request.refreshToken().trim());
		RefreshToken storedToken =
				refreshTokenRepository
						.findByTokenHash(tokenHash)
						.orElseThrow(
								() -> AppException.unauthorized("INVALID_TOKEN", "Refresh token inválido."));

		if (storedToken.getRevokedAt() != null) {
			// Reuso de refresh token revogado indica possível comprometimento: revoga a família inteira.
			refreshTokenRepository.revokeAllActiveByUserId(storedToken.getUser().getId(), now);
			log.warn("Reuso de refresh token detectado [user_id={}]", storedToken.getUser().getId());
			throw AppException.unauthorized("INVALID_TOKEN", "Refresh token inválido ou revogado.");
		}

		if (storedToken.getExpiresAt().isBefore(now)) {
			throw AppException.unauthorized("INVALID_TOKEN", "Refresh token expirado.");
		}

		User user = storedToken.getUser();
		if (!user.isActive() || user.getDeletedAt() != null) {
			throw AppException.unauthorized("INVALID_TOKEN", "Conta inativa ou indisponível.");
		}

		storedToken.revoke(now);
		refreshTokenRepository.save(storedToken);

		StudentProfile profile = studentProfileRepository.findByUserId(user.getId()).orElse(null);
		return issueSessionTokens(user, profile, now);
	}

	/** Revoga explicitamente o refresh token informado no logout (operação idempotente). */
	@Transactional
	public void logout(RefreshTokenRequest request) {
		String tokenHash = jwtTokenService.hashToken(request.refreshToken().trim());
		refreshTokenRepository
				.findByTokenHash(tokenHash)
				.ifPresent(
						token -> {
							if (token.getRevokedAt() == null) {
								token.revoke(Instant.now());
								refreshTokenRepository.save(token);
							}
						});
	}

	/** Retorna o resumo seguro do usuário autenticado pelo ID do subject JWT. */
	@Transactional(readOnly = true)
	public UserSummaryResponse getCurrentUser(UUID userId) {
		User user =
				userRepository
						.findById(userId)
						.filter(u -> u.isActive() && u.getDeletedAt() == null)
						.orElseThrow(
								() -> AppException.notFound("RESOURCE_NOT_FOUND", "Usuário não encontrado."));
		StudentProfile profile = studentProfileRepository.findByUserId(userId).orElse(null);
		return toUserSummary(user, profile);
	}

	private Instant resolveGuardianConsent(
			LocalDate birthDate, Boolean guardianConsent, Instant now) {
		LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
		int age = Period.between(birthDate, todayUtc).getYears();
		if (age < LEGAL_AGE_YEARS) {
			if (!Boolean.TRUE.equals(guardianConsent)) {
				throw AppException.unprocessable(
						"GUARDIAN_CONSENT_REQUIRED",
						"Menores de 18 anos precisam do consentimento de um responsável legal.");
			}
			return now;
		}
		return Boolean.TRUE.equals(guardianConsent) ? now : null;
	}

	private AuthResponse issueSessionTokens(User user, StudentProfile profile, Instant now) {
		String accessToken = jwtTokenService.generateAccessToken(user, now);
		String rawRefreshToken = jwtTokenService.generateOpaqueRefreshToken();
		String refreshTokenHash = jwtTokenService.hashToken(rawRefreshToken);

		Instant refreshExpiresAt = now.plusSeconds(jwtTokenService.getRefreshTokenTtlSeconds());
		refreshTokenRepository.save(new RefreshToken(user, refreshTokenHash, refreshExpiresAt));

		return AuthResponse.bearer(
				accessToken,
				rawRefreshToken,
				jwtTokenService.getAccessTokenTtlSeconds(),
				toUserSummary(user, profile));
	}

	private UserSummaryResponse toUserSummary(User user, StudentProfile profile) {
		String fullName = profile != null ? profile.getFullName() : null;
		String city = profile != null ? profile.getCity() : null;
		return new UserSummaryResponse(user.getId(), user.getEmail(), fullName, user.getRole(), city);
	}

	private AppException invalidCredentials() {
		return AppException.unauthorized("INVALID_CREDENTIALS", "E-mail ou senha inválidos.");
	}
}
