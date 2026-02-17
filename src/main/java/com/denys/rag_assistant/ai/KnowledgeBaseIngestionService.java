package com.denys.rag_assistant.ai;

import com.denys.rag_assistant.service.data.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseIngestionService {

    private final VectorStoreService vectorStoreService;
    private final List<String> ingestedChunkIds = new ArrayList<>();

    @Value("${app.data-lake.path}")
    private String dataLakePath;

    @Value("${app.data-lake.clear-on-shutdown:true}")
    private boolean clearOnShutdown;

    @PreDestroy
    public void onDestroy() {
        if (!clearOnShutdown) {
            log.info("Skipping vector store cleanup on shutdown (clear-on-shutdown=false)");
            return;
        }
        if (ingestedChunkIds.isEmpty()) {
            return;
        }
        log.info("Clearing {} ingested chunks from vector store", ingestedChunkIds.size());
        vectorStoreService.deleteByIds(ingestedChunkIds);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ingestDataLake() {
        Path dataLakeDir = Paths.get(dataLakePath);
        if (!Files.exists(dataLakeDir)) {
            log.warn("Data lake directory not found: {}", dataLakeDir.toAbsolutePath());
            return;
        }

        List<Document> documents = new ArrayList<>();
        try (Stream<Path> files = Files.walk(dataLakeDir)) {
            files.filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            String content = Files.readString(file);
                            String relativePath = dataLakeDir.relativize(file).toString().replace('\\', '/');
                            String role = resolveRole(relativePath);
                            documents.add(new Document(content, Map.of("source", relativePath, "role", role)));
                            log.info("Loaded file: {} (role={})", relativePath, role);
                        } catch (IOException e) {
                            log.error("Failed to read file: {}", file, e);
                        }
                    });
        } catch (IOException e) {
            log.error("Failed to walk data lake directory", e);
            return;
        }

        if (documents.isEmpty()) {
            log.info("No documents found in data lake");
            return;
        }

        var textSplitter = TokenTextSplitter.builder()
                .withChunkSize(800)
                .withMinChunkSizeChars(10)
                .withMinChunkLengthToEmbed(1)
                .withMaxNumChunks(10000)
                .withKeepSeparator(true)
                .build();
        List<Document> chunks = textSplitter.apply(documents);
        log.info("Split {} documents into {} chunks", documents.size(), chunks.size());

        vectorStoreService.addChunks(chunks);
        ingestedChunkIds.addAll(chunks.stream().map(Document::getId).toList());
        log.info("Ingested {} chunks into vector store", chunks.size());
    }

    private String resolveRole(String relativePath) {
        String topFolder = relativePath.contains("/") ? relativePath.substring(0, relativePath.indexOf('/')) : "";
        return topFolder.equalsIgnoreCase("admin") ? "ADMIN" : "USER";
    }
}
