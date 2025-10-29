package com.ankit.projects.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Service
public class SignatureService {

    @Value("${app.s3.secret:miniS3SecretKey}")
    private String secret;

    public String generate(String method, String path, long expires) {
        try {
            String data = method + "\n" + path + "\n" + expires;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isValid(String method, String path, long expires, String provided) {
        if (System.currentTimeMillis() / 1000 > expires) return false;
        String expected = generate(method, path, expires);
        return expected.equalsIgnoreCase(provided);
    }
}
