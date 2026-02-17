package com.denys.rag_assistant.controller;

import com.denys.rag_assistant.ai.KnowledgeBaseIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@RestController
@RequestMapping("/data-lake")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseIngestionService knowledgeBaseIngestionService;

    @Value("${app.data-lake.path}")
    private String dataLakePath;

    @PostMapping("/ingest")
    public String ingest() {
        knowledgeBaseIngestionService.ingestDataLake();
        return "Data lake ingested successfully";
    }

    @PostMapping(value = "/upload/admin", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadAdmin(@RequestParam("file") MultipartFile file) throws IOException {
        saveFile(file, "admin");
        knowledgeBaseIngestionService.ingestDataLake();
        return "File uploaded to admin: " + file.getOriginalFilename();
    }

    @PostMapping(value = "/upload/user", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadUser(@RequestParam("file") MultipartFile file) throws IOException {
        saveFile(file, "user");
        knowledgeBaseIngestionService.ingestDataLake();
        return "File uploaded to user: " + file.getOriginalFilename();
    }

    private void saveFile(MultipartFile file, String folder) throws IOException {
        Path dir = Paths.get(dataLakePath, folder);
        Files.createDirectories(dir);
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("File name is missing");
        }
        Path target = dir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
    }
}
