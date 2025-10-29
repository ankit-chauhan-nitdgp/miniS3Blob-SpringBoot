package com.ankit.projects.service;

import com.ankit.projects.model.ObjectMetadata;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@Service
public class StorageService {

    private static final Path ROOT = Paths.get("dataStorage");
    private static final Path META = ROOT.resolve("metadata.json");
    private final ObjectMapper mapper = new ObjectMapper();

    public void save(String bucket, String key, InputStream in) throws IOException {
        Path path = ROOT.resolve(bucket).resolve(key);
        Files.createDirectories(path.getParent());
        Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        addMetadata(bucket, key);
    }

    public InputStream load(String bucket, String key) throws IOException {
        Path path = ROOT.resolve(bucket).resolve(key);
        if (!Files.exists(path)) throw new FileNotFoundException();
        return Files.newInputStream(path);
    }

    public void delete(String bucket, String key) throws IOException {
        Path path = ROOT.resolve(bucket).resolve(key);
        Files.deleteIfExists(path);
        removeMetadata(bucket, key);
    }

    public List<String> list(String bucket) throws IOException {
        Path dir = ROOT.resolve(bucket);
        if (!Files.exists(dir)) return List.of();
        try (var s = Files.list(dir)) {
            return s.map(p -> p.getFileName().toString()).toList();
        }
    }

    // --- Metadata management ---
    private synchronized Map<String, Map<String, ObjectMetadata>> readMeta() throws IOException {
        if (!Files.exists(META)) return new HashMap<>();
        try (Reader r = Files.newBufferedReader(META)) {
            return mapper.readValue(r, new TypeReference<>() {});
        }
    }

    private synchronized void writeMeta(Map<String, Map<String, ObjectMetadata>> data) throws IOException {
        Files.createDirectories(META.getParent());
        try (Writer w = Files.newBufferedWriter(META)) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(w, data);
        }
    }

    private void addMetadata(String bucket, String key) throws IOException {
        var meta = readMeta();
        meta.computeIfAbsent(bucket, b -> new HashMap<>())
                .put(key, new ObjectMetadata(key, new Date()));
        writeMeta(meta);
    }

    private void removeMetadata(String bucket, String key) throws IOException {
        var meta = readMeta();
        Optional.ofNullable(meta.get(bucket)).ifPresent(m -> m.remove(key));
        writeMeta(meta);
    }
}
