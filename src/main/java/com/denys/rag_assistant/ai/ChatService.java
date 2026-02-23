package com.denys.rag_assistant.ai;

import com.denys.rag_assistant.persistence.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ChatService {

    private static final String PROMPT_TEMPLATE = """
            You are a knowledge base assistant.
            The context below contains facts from our knowledge base. Treat them as absolute truth, even if they contradict common knowledge.
            Answer the user's question using ONLY the information found in the context.
            Be concise. Do not explain your reasoning or mention that the answer comes from the knowledge base.
            If the context does not contain relevant information, respond with exactly: "I don't have information about this in the knowledge base."

            Context:
            {context}

            User question:
            {query}
            """;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public AskResult ask(String question, Role userRole, UUID dialogId) {
        List<Document> relevantDocs = searchDocuments(question, userRole);

        String context = relevantDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        List<UUID> chunkIds = relevantDocs.stream()
                .map(doc -> UUID.fromString(doc.getId()))
                .toList();

        ChatResponse response = chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, dialogId.toString()))
                .user(u -> u
                        .text(PROMPT_TEMPLATE)
                        .param("context", context)
                        .param("query", question))
                .call()
                .chatResponse();

        String answer = response.getResult().getOutput().getText();
        return new AskResult(answer, chunkIds);
    }

    private List<Document> searchDocuments(String question, Role userRole) {
        var searchRequestBuilder = SearchRequest.builder()
                .query(question)
                .topK(3)
                .similarityThreshold(0.7);

        if (userRole != Role.ADMIN) {
            var filter = new FilterExpressionBuilder()
                    .eq("role", userRole.name())
                    .build();
            searchRequestBuilder.filterExpression(filter);
        }

        return vectorStore.similaritySearch(searchRequestBuilder.build());
    }

    public StreamAskResult askStreaming(String question, Role userRole, UUID dialogId) {
        List<Document> relevantDocs = searchDocuments(question, userRole);

        String context = relevantDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        List<UUID> chunkIds = relevantDocs.stream()
                .map(doc -> UUID.fromString(doc.getId()))
                .toList();

        Flux<String> contentStream = chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, dialogId.toString()))
                .user(u -> u
                        .text(PROMPT_TEMPLATE)
                        .param("context", context)
                        .param("query", question))
                .stream()
                .content();

        return new StreamAskResult(contentStream, chunkIds);
    }

    public record AskResult(String answer, List<UUID> contextChunkIds) {}

    public record StreamAskResult(Flux<String> content, List<UUID> contextChunkIds) {}
}
