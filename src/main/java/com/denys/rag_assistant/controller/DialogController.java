package com.denys.rag_assistant.controller;

import com.denys.rag_assistant.persistence.entity.DialogEntity;
import com.denys.rag_assistant.persistence.entity.MessageEntity;
import com.denys.rag_assistant.service.DialogFacade;
import com.denys.rag_assistant.service.data.DialogService;
import com.denys.rag_assistant.service.data.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/dialogs")
@RequiredArgsConstructor
public class DialogController {

    private final DialogFacade askService;
    private final DialogService dialogService;
    private final UserService userService;

    @PostMapping
    public DialogEntity createDialog(@RequestParam UUID userId, @RequestParam(required = false) String title) {
        var user = userService.getById(userId);
        return dialogService.create(user, title);
    }

    @GetMapping
    public List<DialogEntity> getDialogs(@RequestParam UUID userId) {
        return dialogService.getByUserId(userId);
    }

    @GetMapping("/{dialogId}/messages")
    public List<MessageEntity> getMessages(@PathVariable UUID dialogId) {
        return dialogService.getMessages(dialogId);
    }

    @PostMapping("/{dialogId}/ask")
    public MessageEntity ask(@PathVariable UUID dialogId, @RequestParam String question) {
        return askService.ask(dialogId, question);
    }
}
