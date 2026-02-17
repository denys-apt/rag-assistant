package com.denys.ollama_integration.controller;

import com.denys.ollama_integration.llm.rag.DataLakeIngestionService;
import com.denys.ollama_integration.service.DialogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DialogController {

    private final DialogService dialogService;
    private final DataLakeIngestionService ingestionService;

    @GetMapping("/ask")
    public String ask(@RequestParam String question, @RequestParam String dialogId) {
        return dialogService.ask(question, dialogId);
    }

}
