package com.denys.rag_assistant.ai;

import com.denys.rag_assistant.service.data.VectorStoreService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseIngestionService {

    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            "txt", "md", "rst", "csv", "json", "xml", "yaml", "yml", "properties", "log", "ini", "toml"
    );

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg", "gif", "webp", "bmp", "tiff", "tif"
    );

    private final VectorStoreService vectorStoreService;
    private final ChatClient.Builder chatClientBuilder;
    private final List<String> ingestedChunkIds = new ArrayList<>();

    private ChatClient imageChatClient;

    @Value("${app.data-lake.path}")
    private String dataLakePath;

    @Value("${app.data-lake.clear-on-shutdown:true}")
    private boolean clearOnShutdown;

    @Value("${app.data-lake.vision-model:llava}")
    private String visionModel;

    @Value("${app.data-lake.image-support:false}")
    private boolean imageSupport;

    @PostConstruct
    void init() {
        this.imageChatClient = chatClientBuilder.build();
    }

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
                        String relativePath = dataLakeDir.relativize(file).toString().replace('\\', '/');
                        String role = resolveRole(relativePath);
                        documents.addAll(parseFile(file, relativePath, role));
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

    public void saveFile(MultipartFile file, String folder) throws IOException {
        Path dir = Paths.get(dataLakePath, folder);
        Files.createDirectories(dir);
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("File name is missing");
        }
        Path target = dir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
    }

    private List<Document> parseFile(Path file, String relativePath, String role) {
        String ext = getExtension(file);
        if (IMAGE_EXTENSIONS.contains(ext)) {
            return parseImageFile(file, relativePath, role);
        } else if (TEXT_EXTENSIONS.contains(ext)) {
            return parseTextFile(file, relativePath, role);
        } else {
            return parseTikaFile(file, relativePath, role);
        }
    }

    private List<Document> parseTextFile(Path file, String relativePath, String role) {
        try {
            String content = Files.readString(file);
            log.info("Loaded text file: {} (role={})", relativePath, role);
            return List.of(new Document(content, Map.of("source", relativePath, "role", role)));
        } catch (IOException e) {
            log.error("Failed to read text file: {}", file, e);
            return List.of();
        }
    }

    private List<Document> parseTikaFile(Path file, String relativePath, String role) {
        try {
            var resource = new FileSystemResource(file);
            var reader = new TikaDocumentReader(resource);
            List<Document> docs = reader.get().stream()
                    .map(doc -> new Document(doc.getText(), Map.of("source", relativePath, "role", role)))
                    .toList();
            log.info("Loaded document via Tika: {} ({} parts, role={})", relativePath, docs.size(), role);
            return docs;
        } catch (Exception e) {
            log.error("Failed to parse document with Tika: {}", file, e);
            return List.of();
        }
    }

    private List<Document> parseImageFile(Path file, String relativePath, String role) {
        if (!imageSupport) {
            log.warn("Image support is disabled, skipping: {}. Enable with app.data-lake.image-support=true", relativePath);
            return List.of();
        }
        try {
            String ext = getExtension(file);
            MimeType mimeType = switch (ext) {
                case "jpg", "jpeg" -> MimeTypeUtils.IMAGE_JPEG;
                case "gif" -> MimeTypeUtils.IMAGE_GIF;
                case "webp" -> MimeType.valueOf("image/webp");
                default -> MimeTypeUtils.IMAGE_PNG;
            };
            var resource = new FileSystemResource(file);
            String description = imageChatClient.prompt()
                    .options(OllamaChatOptions.builder().model(visionModel).build())
                    .user(u -> u
//                                    .text("Describe the content of this image in detail. Extract all visible text, data, tables, and meaningful information for a knowledge base search system.")
                                    .text("Extract and transcribe ALL visible text and data from this image exactly as shown. Do NOT analyze, interpret, correct, or add any commentary. Output only what is literally visible in the image.")
                    .media(mimeType, resource))
                    .call()
                    .content();
            log.info("Described image via AI (model={}): {} (role={}), description: {}", visionModel, relativePath, role, description);
            return List.of(new Document(description, Map.of("source", relativePath, "role", role, "type", "image")));
        } catch (Exception e) {
            log.error("Failed to process image with AI: {}", file, e);
            return List.of();
        }
    }

    private String getExtension(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase() : "";
    }

    private String resolveRole(String relativePath) {
        String topFolder = relativePath.contains("/") ? relativePath.substring(0, relativePath.indexOf('/')) : "";
        return topFolder.equalsIgnoreCase("admin") ? "ADMIN" : "USER";
    }
}
