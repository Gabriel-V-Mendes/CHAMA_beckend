package com.chama.backend.modules.user.repository;

import com.chama.backend.modules.user.model.StudentProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório de acesso aos perfis de estudantes. */
public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID> {

	Optional<StudentProfile> findByUserId(UUID userId);
}
