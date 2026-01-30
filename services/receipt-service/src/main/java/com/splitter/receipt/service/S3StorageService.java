package com.splitter.receipt.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class S3StorageService implements StorageService {
    
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    
    @Value("${storage.s3.bucket:splitter-receipts}")
    private String bucketName;
    
    @Value("${storage.s3.prefix:receipts}")
    private String prefix;
    
    @Override
    public Mono<String> upload(byte[] data, String filename, String contentType) {
        return Mono.fromCallable(() -> {
            String key = generateKey(filename);
            
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();
            
            s3Client.putObject(request, RequestBody.fromBytes(data));
            
            log.info("Uploaded file to S3: bucket={}, key={}", bucketName, key);
            return key;
        }).subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<byte[]> download(String storagePath) {
        return Mono.fromCallable(() -> {
            GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(storagePath)
                .build();
            
            return s3Client.getObjectAsBytes(request).asByteArray();
        }).subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<Void> delete(String storagePath) {
        return Mono.fromCallable(() -> {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(storagePath)
                .build();
            
            s3Client.deleteObject(request);
            log.info("Deleted file from S3: bucket={}, key={}", bucketName, storagePath);
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
    
    @Override
    public Mono<String> getPresignedUrl(String storagePath, int expirationMinutes) {
        return Mono.fromCallable(() -> {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(storagePath)
                .build();
            
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expirationMinutes))
                .getObjectRequest(getObjectRequest)
                .build();
            
            return s3Presigner.presignGetObject(presignRequest).url().toString();
        }).subscribeOn(Schedulers.boundedElastic());
    }
    
    private String generateKey(String originalFilename) {
        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = originalFilename.substring(lastDotIndex);
        }
        
        String datePrefix = java.time.LocalDate.now().toString().replace("-", "/");
        return String.format("%s/%s/%s%s", prefix, datePrefix, UUID.randomUUID(), extension);
    }
}
