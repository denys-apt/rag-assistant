package com.denys.rag_assistant.controller;

import com.denys.rag_assistant.ai.KnowledgeBaseIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/data-lake")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseIngestionService knowledgeBaseIngestionService;

    @PostMapping("/ingest")
    public String ingest() {
        knowledgeBaseIngestionService.ingestDataLake();
        return "Data lake ingested successfully";
    }
}
