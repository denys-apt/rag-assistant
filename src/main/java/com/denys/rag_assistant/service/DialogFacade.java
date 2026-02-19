package com.denys.rag_assistant.service;

import com.denys.rag_assistant.controller.dto.request.QuestionRequest;
import com.denys.rag_assistant.persistence.entity.DialogEntity;
import com.denys.rag_assistant.persistence.entity.Role;
import com.denys.rag_assistant.ai.ChatService;
import com.denys.rag_assistant.service.data.DialogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class DialogFacade {

    private final ChatService chatService;
    private final DialogService dialogService;

    public String ask(QuestionRequest questionRequest) {
        DialogEntity dialog = dialogService.getById(questionRequest.dialogId());
        Role userRole = dialog.getUser().getRole();
        var question = questionRequest.question();


        ChatService.AskResult result = chatService.ask(question, userRole, questionRequest.dialogId());
        var messageEntity = dialogService.saveMessage(dialog, question, result.answer(), result.contextChunkIds());

        return messageEntity.getAnswer();
    }

    public SseEmitter askStreaming(QuestionRequest questionRequest) {
        DialogEntity dialog = dialogService.getById(questionRequest.dialogId());
        Role userRole = dialog.getUser().getRole();
        var question = questionRequest.question();

        ChatService.StreamAskResult result = chatService.askStreaming(question, userRole, questionRequest.dialogId());

        var emitter = new SseEmitter(0L);
        var fullAnswer = new StringBuilder();

        result.content().subscribe(
                chunk -> {
                    try {
                        fullAnswer.append(chunk);
                        emitter.send(SseEmitter.event().data(chunk));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                },
                emitter::completeWithError,
                () -> {
                    dialogService.saveMessage(dialog, question, fullAnswer.toString(), result.contextChunkIds());
                    emitter.complete();
                }
        );

        return emitter;
    }
}
