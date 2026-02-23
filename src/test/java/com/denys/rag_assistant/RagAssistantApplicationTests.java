package com.denys.rag_assistant;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.vectorstore.pgvector.initialize-schema=false"
})
class RagAssistantApplicationTests {

    @MockitoBean
    OllamaChatModel ollamaChatModel;

    @MockitoBean
    EmbeddingModel embeddingModel;

    @MockitoBean
    VectorStore vectorStore;

    @Test
    void contextLoads() {
    }
}
