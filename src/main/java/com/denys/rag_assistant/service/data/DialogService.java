package com.denys.rag_assistant.service.data;

import com.denys.rag_assistant.persistence.entity.DialogEntity;
import com.denys.rag_assistant.persistence.entity.MessageEntity;
import com.denys.rag_assistant.persistence.entity.UserEntity;
import com.denys.rag_assistant.persistence.repository.DialogRepository;
import com.denys.rag_assistant.persistence.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DialogService {

    private final DialogRepository dialogRepository;
    private final MessageRepository messageRepository;

    public DialogEntity create(UserEntity user, String title) {
        var dialog = new DialogEntity();
        dialog.setUser(user);
        dialog.setTitle(title);
        return dialogRepository.save(dialog);
    }

    public DialogEntity getById(UUID id) {
        return dialogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dialog not found: " + id));
    }

    public List<DialogEntity> getByUserId(UUID userId) {
        return dialogRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public MessageEntity saveMessage(DialogEntity dialog, String question, String answer,
                                     List<UUID> contextChunkIds, long processTimeMs) {
        var message = new MessageEntity();
        message.setDialog(dialog);
        message.setQuestion(question);
        message.setAnswer(answer);
        message.setContextChunkIds(contextChunkIds);
        message.setProcessTimeMs(processTimeMs);
        return messageRepository.save(message);
    }

    public List<MessageEntity> getMessages(UUID dialogId) {
        return messageRepository.findByDialogIdOrderByCreatedAtAsc(dialogId);
    }
}
