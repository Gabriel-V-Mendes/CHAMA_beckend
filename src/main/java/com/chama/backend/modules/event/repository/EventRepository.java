package com.chama.backend.modules.event.repository;

import com.chama.backend.modules.event.model.Event;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, UUID> {}
