package com.chama.backend.modules.auth.dto;

import com.chama.backend.modules.user.model.UserRole;
import java.util.UUID;

/** Resumo seguro do usuário autenticado (sem hash de senha ou dados sensíveis desnecessários). */
public record UserSummaryResponse(
		UUID id, String email, String fullName, UserRole role, String city) {
}
