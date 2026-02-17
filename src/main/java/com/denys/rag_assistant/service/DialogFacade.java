package com.denys.rag_assistant.service;

import com.denys.rag_assistant.persistence.entity.DialogEntity;
import com.denys.rag_assistant.persistence.entity.MessageEntity;
import com.denys.rag_assistant.persistence.entity.Role;
import com.denys.rag_assistant.ai.ChatService;
import com.denys.rag_assistant.service.data.DialogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DialogFacade {

    private final ChatService chatService;
    private final DialogService dialogService;

    public MessageEntity ask(UUID dialogId, String question) {
        DialogEntity dialog = dialogService.getById(dialogId);
        Role userRole = dialog.getUser().getRole();

        long start = System.currentTimeMillis();
        ChatService.AskResult result = chatService.ask(question, userRole);
        long processTimeMs = System.currentTimeMillis() - start;

        return dialogService.saveMessage(dialog, question, result.answer(),
                result.contextChunkIds(), processTimeMs);
    }
}
