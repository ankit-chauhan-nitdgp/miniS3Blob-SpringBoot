package com.ankit.projects.controller;

import com.ankit.projects.service.PresignedUrlRegistry;
import com.ankit.projects.service.SignatureService;
import com.ankit.projects.service.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLConnection;
import java.util.*;

@RestController
@RequestMapping("/bucket/{bucket}")
public class StorageController {

    private final StorageService storage;
    private final SignatureService sigService;
    private final PresignedUrlRegistry registry;

    public StorageController(StorageService storage, SignatureService sigService, PresignedUrlRegistry registry) {
        this.storage = storage;
        this.sigService = sigService;
        this.registry = registry;
    }

    // ------------------ PUT ------------------
    // ------------------ PUT ------------------
    @PutMapping("/{key}")
    public ResponseEntity<String> upload(@PathVariable String bucket,
                                         @PathVariable String key,
                                         @RequestParam(required = false) Long expires,
                                         @RequestParam(required = false) String signature,
                                         HttpServletRequest request) throws Exception {

        String path = "/bucket/" + bucket + "/" + key;
        if (expires != null && signature != null) {
            String fullUrl = path + "?expires=" + expires + "&signature=" + signature;
            if (!sigService.isValid("PUT", path, expires, signature) || registry.isExpired(fullUrl))
                return ResponseEntity.status(403).body("Invalid or expired signature");
        }

        try (InputStream in = request.getInputStream()) {
            storage.save(bucket, key, in);
        }
        return ResponseEntity.ok("Uploaded");
    }

    // ------------------ GET ------------------
    @GetMapping("/{key}")
    public ResponseEntity<?> download(@PathVariable String bucket,
                                      @PathVariable String key,
                                      @RequestParam(required = false) Long expires,
                                      @RequestParam(required = false) String signature) throws Exception {

        String path = "/bucket/" + bucket + "/" + key;
        if (expires != null && signature != null) {
            String fullUrl = path + "?expires=" + expires + "&signature=" + signature;
            if (!sigService.isValid("GET", path, expires, signature) || registry.isExpired(fullUrl))
                return ResponseEntity.status(403).body("Invalid or expired signature");
        }

        InputStream in = storage.load(bucket, key);
        String mime = URLConnection.guessContentTypeFromName(key);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mime != null ? mime : "application/octet-stream"))
                .body(new InputStreamResource(in));
    }

    // ------------------ DELETE ------------------
    @DeleteMapping("/{key}")
    public ResponseEntity<String> delete(@PathVariable String bucket, @PathVariable String key) throws Exception {
        storage.delete(bucket, key);
        return ResponseEntity.ok("Deleted");
    }

    // ------------------ LIST ------------------
    @GetMapping("/list")
    public ResponseEntity<List<String>> list(@PathVariable String bucket) throws Exception {
        return ResponseEntity.ok(storage.list(bucket));
    }

    // ------------------ PRESIGNED URLS ------------------

    @GetMapping("/presign/get")
    public ResponseEntity<Map<String, Object>> presignGet(
            @PathVariable String bucket,
            @RequestParam String key,
            @RequestParam(defaultValue = "300") long ttlSeconds) {

        long expires = System.currentTimeMillis() / 1000 + ttlSeconds;
        String path = "/bucket/" + bucket + "/" + key;
        String sig = sigService.generate("GET", path, expires);
        String url = "http://localhost:8080" + path + "?expires=" + expires + "&signature=" + sig;

        registry.register(path + "?expires=" + expires + "&signature=" + sig, expires);

        return ResponseEntity.ok(Map.of(
                "method", "GET",
                "url", url,
                "expires", expires
        ));
    }

    @GetMapping("/presign/put")
    public ResponseEntity<Map<String, Object>> presignPut(
            @PathVariable String bucket,
            @RequestParam String key,
            @RequestParam(defaultValue = "300") long ttlSeconds) {

        long expires = System.currentTimeMillis() / 1000 + ttlSeconds;
        String path = "/bucket/" + bucket + "/" + key;
        String sig = sigService.generate("PUT", path, expires);
        String url = "http://localhost:8080" + path + "?expires=" + expires + "&signature=" + sig;

        registry.register(path + "?expires=" + expires + "&signature=" + sig, expires);

        return ResponseEntity.ok(Map.of(
                "method", "PUT",
                "url", url,
                "expires", expires
        ));
    }
}
