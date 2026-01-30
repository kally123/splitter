package com.splitter.receipt.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Local filesystem storage implementation for development/testing.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {
    
    @Value("${storage.local.path:./receipts}")
    private String basePath;
    
    @Override
    public Mono<String> upload(byte[] data, String filename, String contentType) {
        return Mono.fromCallable(() -> {
            Path baseDir = Paths.get(basePath);
            String dateDir = LocalDate.now().toString().replace("-", "/");
            Path directory = baseDir.resolve(dateDir);
            
            Files.createDirectories(directory);
            
            String extension = getExtension(filename);
            String key = UUID.randomUUID() + extension;
            Path filePath = directory.resolve(key);
            
            Files.write(filePath, data, StandardOpenOption.CREATE_NEW);
            
            String storagePath = dateDir + "/" + key;
            log.info("Saved file to local storage: {}", storagePath);
            return storagePath;
        }).subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<byte[]> download(String storagePath) {
        return Mono.fromCallable(() -> {
            Path filePath = Paths.get(basePath).resolve(storagePath);
            return Files.readAllBytes(filePath);
        }).subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<Void> delete(String storagePath) {
        return Mono.fromCallable(() -> {
            Path filePath = Paths.get(basePath).resolve(storagePath);
            Files.deleteIfExists(filePath);
            log.info("Deleted file from local storage: {}", storagePath);
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
    
    @Override
    public Mono<String> getPresignedUrl(String storagePath, int expirationMinutes) {
        // Local storage doesn't support presigned URLs
        // Return a placeholder URL for development
        return Mono.just("/api/v1/receipts/download/" + storagePath);
    }
    
    private String getExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex) : "";
    }
}
