package com.denys.rag_assistant.persistence.repository;

import com.denys.rag_assistant.persistence.entity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {
    List<MessageEntity> findByDialogIdOrderByCreatedAtAsc(UUID dialogId);
}
