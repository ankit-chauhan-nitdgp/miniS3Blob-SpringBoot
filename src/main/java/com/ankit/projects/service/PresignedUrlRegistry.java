package com.ankit.projects.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PresignedUrlRegistry {

    private final Map<String, Long> activeUrls = new ConcurrentHashMap<>();

    public void register(String url, long expires) {
        activeUrls.put(url, expires);
    }

    public boolean isExpired(String url) {
        Long exp = activeUrls.get(url);
        return exp == null || System.currentTimeMillis() / 1000 > exp;
    }

    @Scheduled(fixedRate = 60000)
    public void cleanup() {
        long now = System.currentTimeMillis() / 1000;
        activeUrls.entrySet().removeIf(e -> e.getValue() < now);
    }

    public void remove(String url) {
        activeUrls.remove(url);
    }
}
