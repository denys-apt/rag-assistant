package com.denys.rag_assistant.persistence.repository;

import com.denys.rag_assistant.persistence.entity.DialogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DialogRepository extends JpaRepository<DialogEntity, UUID> {
    List<DialogEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
