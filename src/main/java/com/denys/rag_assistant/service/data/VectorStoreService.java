package com.denys.rag_assistant.service.data;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VectorStoreService {

    private final VectorStore vectorStore;

    public void addChunks(List<Document> chunks) {
        vectorStore.add(chunks);
    }

    public void deleteByIds(List<String> ids) {
        vectorStore.delete(ids);
    }
}
